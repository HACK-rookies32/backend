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
        // [VULN-WALLET-7] 부모-자녀 관계(페어링)를 검증하지 않는다(IDOR).
        //   아래 requireExistingChild 가 확인하는 것은 "그 id 가 실제로 존재하는 CHILD 계정인가"뿐이다.
        //   "그 자녀가 이 부모의 자녀인가"(child.getParent().getId() == parentId)는 보지 않는다.
        //   → PARENT 권한만 있으면 아무 집 자녀 지갑에나 돈을 넣을 수 있다.
        //   막히는 것은 존재하지 않는 id, CHILD 가 아닌 계정, 자기 자신에게 보내기 이 셋뿐이라
        //   "검증이 있는 것처럼 보이지만 정작 소유권만 빠져 있는" 형태다.
        //   방어: memberRepository.findById(childId) 후 child.getParent() 가 parentId 인지 확인.
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
        // [VULN-WALLET-7] 회수도 같은 결함이다 — 부모-자녀 관계를 확인하지 않는다.
        //   이쪽은 돈이 자녀 → 부모 방향으로 움직이므로 피해가 더 직접적이다. 남의 집 자녀 지갑에서
        //   돈을 빼내 공격자 부모의 지갑으로 옮길 수 있다. 즉 이 건은 실제 금전 탈취가 성립한다.
        //   방어: 위와 동일하게 child.getParent() 가 parentId 인지 확인.
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
