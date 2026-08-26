package com.example.kidhackbackend.users.wallet.service;

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
 * 보내는 지갑 debit + 받는 지갑 credit + 원장 2건(TRANSFER_OUT/TRANSFER_IN)을 남긴다.
 * 충전 서비스와 분리해 결합도를 낮춘다.
 */
@Service
@RequiredArgsConstructor
public class WalletTransferService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    /** 용돈 이체: 부모 지갑에서 자녀 지갑으로. */
    @Transactional
    public TransferResponse transfer(Long parentId, Long childId, long amount) {
        // [VULN-WALLET-7] 부모-자녀 페어링 미검증(IDOR).
        //   childId가 실제로 이 부모(parentId)의 자녀(Member.parent)인지, CHILD 역할인지 확인하지 않는다.
        //   → 부모 권한만 있으면 임의 회원 id로 이체해 남의 자녀(또는 아무 회원) 지갑에 입금할 수 있다.
        //   방어: memberRepository.findById(childId)로 로드해 child.getParent().getId()==parentId + CHILD 확인.
        return move(parentId, childId, amount);
    }

    /** 잔액 회수: 자녀 지갑에서 부모 지갑으로. */
    @Transactional
    public TransferResponse recover(Long parentId, Long childId, long amount) {
        // [VULN-WALLET-7] 회수도 동일하게 페어링을 검증하지 않는다 → 남의 자녀 잔액을 회수(탈취) 가능.
        return move(childId, parentId, amount);
    }

    /** 공통 이체: fromOwnerId → toOwnerId 로 amount 이동. */
    private TransferResponse move(Long fromOwnerId, Long toOwnerId, long amount) {
        Wallet from = walletRepository.findByOwnerId(fromOwnerId)
                .orElseGet(() -> walletRepository.save(new Wallet(fromOwnerId)));
        Wallet to = walletRepository.findByOwnerId(toOwnerId)
                .orElseGet(() -> walletRepository.save(new Wallet(toOwnerId)));

        // [VULN-WALLET-8] 잔액 검증 부재.
        //   보내는 지갑 잔액이 amount보다 적어도 막지 않아 잔액이 음수가 된다(초과 이체/회수).
        //   방어: if (from.getBalance() < amount) throw new IllegalArgumentException("잔액 부족");
        from.debit(amount);
        to.credit(amount);
        walletRepository.save(from);
        walletRepository.save(to);

        walletTransactionRepository.save(new WalletTransaction(
                from.getId(), TxType.TRANSFER_OUT, amount, from.getBalance(), String.valueOf(toOwnerId)));
        walletTransactionRepository.save(new WalletTransaction(
                to.getId(), TxType.TRANSFER_IN, amount, to.getBalance(), String.valueOf(fromOwnerId)));

        return new TransferResponse(fromOwnerId, toOwnerId, amount, from.getBalance(), to.getBalance());
    }
}
