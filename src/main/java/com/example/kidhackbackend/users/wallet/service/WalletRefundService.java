package com.example.kidhackbackend.users.wallet.service;

import com.example.kidhackbackend.global.exception.ResourceNotFoundException;
import com.example.kidhackbackend.users.wallet.dto.RefundResponse;
import com.example.kidhackbackend.users.wallet.dto.TxResponse;
import com.example.kidhackbackend.users.wallet.entity.ChargeOrder;
import com.example.kidhackbackend.users.wallet.entity.TxType;
import com.example.kidhackbackend.users.wallet.entity.Wallet;
import com.example.kidhackbackend.users.wallet.entity.WalletTransaction;
import com.example.kidhackbackend.users.wallet.repository.ChargeOrderRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 지갑 충전 취소·환불 — 역할표 No.37. 완료된 충전(PAID)을 환불한다.
 *
 * <p>환불: 잔액 검증(음수 방지) → 잔액 차감 → 원장(REFUND) 기록 → 주문 REFUNDED. 전액 환불만 지원(부분 환불은 후속).
 * 충전 서비스와 분리해 결합도를 낮춘다.
 */
@Service
@RequiredArgsConstructor
public class WalletRefundService {

    private final WalletRepository walletRepository;
    private final ChargeOrderRepository chargeOrderRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    /**
     * 충전 환불: merchantUid로 주문을 찾아 전액 환불한다.
     *
     * @param requesterId 환불을 요청한(로그인한) 회원 id
     * @param merchantUid 환불할 충전 주문번호
     */
    @Transactional
    public RefundResponse refund(Long requesterId, String merchantUid) {
        ChargeOrder order = chargeOrderRepository.findByMerchantUid(merchantUid)
                .orElseThrow(() -> new ResourceNotFoundException("충전 주문을 찾을 수 없습니다."));

        // [VULN-WALLET-5] 환불 소유권 미검증(IDOR / BOLA).
        //   주문 소유자(order.getOwnerId())와 요청자(requesterId)가 같은지 확인하지 않는다.
        //   → merchantUid만 알면 아무 PARENT나 남의 충전을 환불시켜, 아래에서 order.getOwnerId()의
        //     지갑 잔액을 마음대로 차감(타인 지갑 조작)할 수 있다.
        //   방어: if (!order.getOwnerId().equals(requesterId)) throw new AccessDenied...;

        // [VULN-WALLET-6] 환불 상태 검증 부재(부분).
        //   PAID 상태인지, 이미 REFUNDED 인지 확인하지 않아 결제 안 된 주문도 환불을 시도할 수 있다.
        //   단, 아래 잔액 가드 때문에 실제로 돈이 빠지려면 잔액이 충분해야 하므로, 이중 환불·미결제 환불은
        //   대부분 잔액 부족으로 막힌다. 소유권 미검증([VULN-WALLET-5])은 그대로 열려 있다.

        long refundAmount = order.getAmount();

        Wallet wallet = walletRepository.findByOwnerId(order.getOwnerId())
                .orElseGet(() -> walletRepository.save(new Wallet(order.getOwnerId())));

        // [R-2] 잔액 음수 방지 — 돈이라 이것만은 막는다. 환불액이 현재 잔액보다 크면 거부.
        //   (부수 효과: 이중 환불·미결제 주문 환불도 여기서 대부분 차단된다.)
        if (wallet.getBalance() < refundAmount) {
            throw new IllegalArgumentException("환불 가능 잔액이 부족합니다.");
        }

        order.markRefunded();
        wallet.debit(refundAmount);
        walletRepository.save(wallet);

        WalletTransaction tx = walletTransactionRepository.save(
                new WalletTransaction(wallet.getId(), TxType.REFUND, refundAmount,
                        wallet.getBalance(), order.getMerchantUid()));

        return new RefundResponse(order.getMerchantUid(), refundAmount, wallet.getBalance(), TxResponse.from(tx));
    }
}
