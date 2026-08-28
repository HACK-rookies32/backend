package com.example.kidhackbackend.users.wallet.service;

import com.example.kidhackbackend.global.notification.entity.NotificationType;
import com.example.kidhackbackend.global.notification.service.NotificationPort;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import com.example.kidhackbackend.users.wallet.dto.TransferResponse;
import com.example.kidhackbackend.users.wallet.entity.TxType;
import com.example.kidhackbackend.users.wallet.entity.Wallet;
import com.example.kidhackbackend.users.wallet.entity.WalletTransaction;
import com.example.kidhackbackend.users.wallet.repository.WalletRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 부모↔자녀 지갑 이체 — 역할표 No.39(용돈 이체)·No.40(잔액 회수).
 *
 * <p>이체(transfer): 부모→자녀. 회수(recover): 자녀→부모. 방향만 반대인 동일 로직.
 * 보내는 지갑 debit + 받는 지갑 credit + 원장 2건(TRANSFER_OUT/TRANSFER_IN)을 남기고,
 * 자녀에게 알림을 발행한다.
 */
@Service
@RequiredArgsConstructor
public class WalletTransferService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final MemberRepository memberRepository;
    private final NotificationPort notificationPort;

    /** 용돈 이체: 부모 지갑에서 자녀 지갑으로. */
    @Transactional
    public TransferResponse transfer(Long parentId, Long childId, long amount) {
        // [VULN-WALLET-7] 부모-자녀 페어링(소유권)은 여전히 검증하지 않는다(IDOR).
        //   childId 가 실재하는 CHILD 인지만 확인하고, child.getParent()==parentId 는 보지 않으므로
        //   PARENT 권한만 있으면 남의 자녀 지갑에도 입금할 수 있다. (유령 id·비-CHILD·자기 자신만 차단됨)
        requireDifferent(parentId, childId);
        requireExistingChild(childId);

        TransferResponse res = move(parentId, childId, amount);
        notificationPort.publish(childId, NotificationType.ALLOWANCE_RECEIVED,
                "부모가 용돈 " + won(amount) + "을 보냈어요.");
        return res;
    }

    /** 잔액 회수: 자녀 지갑에서 부모 지갑으로. */
    @Transactional
    public TransferResponse recover(Long parentId, Long childId, long amount) {
        // [VULN-WALLET-7] 회수도 소유권은 검증하지 않는다 → 남의 자녀 잔액을 회수(탈취) 가능.
        requireDifferent(parentId, childId);
        requireExistingChild(childId);

        TransferResponse res = move(childId, parentId, amount);
        notificationPort.publish(childId, NotificationType.ALLOWANCE_RECLAIMED,
                "부모가 잔액 " + won(amount) + "을 회수했어요.");
        return res;
    }

    /** 공통 이체: fromOwnerId → toOwnerId 로 amount 이동. */
    private TransferResponse move(Long fromOwnerId, Long toOwnerId, long amount) {
        Wallet from = walletRepository.findByOwnerId(fromOwnerId)
                .orElseGet(() -> walletRepository.save(new Wallet(fromOwnerId)));
        Wallet to = walletRepository.findByOwnerId(toOwnerId)
                .orElseGet(() -> walletRepository.save(new Wallet(toOwnerId)));

        // [T-1] 잔액 음수 방지 — 돈이라 이것만은 막는다. 보내는 쪽 잔액보다 큰 금액이면 거부.
        if (from.getBalance() < amount) {
            throw new IllegalArgumentException("보내는 지갑의 잔액이 부족합니다.");
        }

        from.debit(amount);
        to.credit(amount);
        walletRepository.save(from);
        walletRepository.save(to);

        walletTransactionRepository.save(new WalletTransaction(
                from.getId(), TxType.TRANSFER_OUT, amount, from.getBalance(), String.valueOf(toOwnerId)));
        walletTransactionRepository.save(new WalletTransaction(
                to.getId(), TxType.TRANSFER_IN, amount, to.getBalance(), String.valueOf(fromOwnerId)));

        return new TransferResponse(
                fromOwnerId, nameOf(fromOwnerId),
                toOwnerId, nameOf(toOwnerId),
                amount, from.getBalance(), to.getBalance());
    }

    /** childId 가 실재하는(삭제되지 않은) CHILD 계정인지 확인. 소유권(페어링)은 검증하지 않는다. */
    private void requireExistingChild(Long childId) {
        Member child = memberRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("자녀를 찾을 수 없습니다."));
        if (child.isDeleted() || child.getRole() != Role.CHILD) {
            throw new IllegalArgumentException("자녀를 찾을 수 없습니다.");
        }
    }

    private void requireDifferent(Long a, Long b) {
        if (a.equals(b)) {
            throw new IllegalArgumentException("자기 자신에게는 이체할 수 없습니다.");
        }
    }

    private String nameOf(Long ownerId) {
        return memberRepository.findById(ownerId).map(Member::getUsername).orElse("(알 수 없음)");
    }

    private static String won(long amount) {
        return String.format("%,d원", amount);
    }
}
