package com.example.kidhackbackend.users.wallet.service;

import com.example.kidhackbackend.global.exception.ResourceNotFoundException;
import com.example.kidhackbackend.users.wallet.dto.RefundResponse;
import com.example.kidhackbackend.users.wallet.dto.TxResponse;
import com.example.kidhackbackend.users.wallet.entity.Card;
import com.example.kidhackbackend.users.wallet.entity.ChargeOrder;
import com.example.kidhackbackend.users.wallet.entity.TxType;
import com.example.kidhackbackend.users.wallet.entity.Wallet;
import com.example.kidhackbackend.users.wallet.entity.WalletTransaction;
import com.example.kidhackbackend.users.wallet.pg.PgClient;
import com.example.kidhackbackend.users.wallet.pg.PgRefund;
import com.example.kidhackbackend.users.wallet.repository.CardRepository;
import com.example.kidhackbackend.users.wallet.repository.ChargeOrderRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 지갑 충전 취소·환불 — 역할표 No.37. 완료된 충전(PAID)을 환불한다.
 *
 * <p>환불: (카드 지정 시 mock PG payout) → 잔액 검증(음수 방지) → 잔액 차감 → 원장(REFUND) 기록 → 주문 REFUNDED.
 * 전액 환불만 지원(부분 환불은 후속). 충전 서비스와 분리해 결합도를 낮춘다.
 */
@Service
@RequiredArgsConstructor
public class WalletRefundService {

    private final WalletRepository walletRepository;
    private final ChargeOrderRepository chargeOrderRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final CardRepository cardRepository;
    private final PgClient pgClient;

    /** 카드 미지정 환불(레거시) — 지갑 잔액만 차감한다. */
    @Transactional
    public RefundResponse refund(Long requesterId, String merchantUid) {
        return refund(requesterId, merchantUid, null);
    }

    /**
     * 충전 환불: merchantUid로 주문을 찾아 전액 환불한다. cardId가 있으면 그 카드로 mock PG 환불(payout)을 태운다.
     *
     * @param requesterId 환불을 요청한(로그인한) 회원 id
     * @param merchantUid 환불할 충전 주문번호
     * @param cardId      환불받을 카드 id (선택). null 이면 지갑 잔액만 차감
     */
    @Transactional
    public RefundResponse refund(Long requesterId, String merchantUid, Long cardId) {
        ChargeOrder order = chargeOrderRepository.findByMerchantUid(merchantUid)
                .orElseThrow(() -> new ResourceNotFoundException("충전 주문을 찾을 수 없습니다."));

        // [VULN-WALLET-5] 환불 소유권 미검증(IDOR / BOLA).
        //   주문 소유자(order.getOwnerId())와 요청자(requesterId)가 같은지 확인하지 않는다.
        //   → merchantUid만 알면 아무 PARENT나 남의 충전을 환불시켜 order.getOwnerId()의 지갑을 차감할 수 있다.

        // [VULN-WALLET-6] 환불 상태 검증 부재(부분). PAID/REFUNDED 여부를 확인하지 않는다(아래 R-2 가 대부분 차단).
        // [VULN-WALLET-5] 환불 소유권 미검증(IDOR / BOLA). — 판정: 취약
        //   위에서 merchantUid 로 주문을 찾았을 뿐, 그 주문의 주인(order.getOwnerId())이 환불을
        //   요청한 사람(requesterId)과 같은지 확인하지 않는다. 넘겨받은 requesterId 가 이 메서드
        //   어디에서도 쓰이지 않는다.
        //   → 주문번호만 알면 아무 PARENT 나 남의 충전을 환불시킬 수 있다. 환불은 잔액을 '깎는'
        //     동작이므로(아래 wallet.debit), 결과적으로 남의 지갑에서 돈을 빼는 것이 된다.
        //     공격자가 그 돈을 가져가지는 못하지만 피해자는 잃는다 — 잔액을 0으로 만들어 결제를
        //     못 하게 하는 것 자체가 공격이다. 2026-09-07 모의해킹에서 실제로 성립했다.
        //   ※ [VULN-WALLET-2](남의 충전 승인)와 짝이다. 그쪽이 남의 잔액을 올리고 이쪽이 내리므로,
        //     둘을 합치면 임의 계정 잔액에 대한 사실상의 쓰기 권한이 된다.
        //   방어: if (!order.getOwnerId().equals(requesterId)) throw new AccessDenied...;

        // [VULN-WALLET-6] 환불 상태 검증 부재. — 판정: 조건부 (아래 R-2 가드에 가려 대부분 막힌다)
        //   주문이 결제완료(PAID)인지, 이미 환불(REFUNDED)된 건은 아닌지 확인하는 코드가 없다.
        //   그래서 결제도 안 된 주문이나 이미 환불한 주문에 환불을 또 시도할 수 있다.
        //   다만 아래 [R-2] 잔액 가드가 "환불액보다 잔액이 적으면 거부"하므로, 이중 환불과 미결제
        //   환불은 대개 여기서 잔액 부족으로 먼저 걸린다. 상태 검사가 없다는 결함 자체는 남아 있고,
        //   잔액이 우연히 넉넉하면(다른 충전으로 채워져 있으면) 그대로 통과한다. 상태 검사가 아니라
        //   금액 가드에 의존해 막고 있는 셈이라, 방어가 있다고 보기는 어렵다.
        //   (참고: 관리자 경로 AdminFinanceCommandService 는 이 결함을 물려받지 않고 상태를 검증한다.)
        //   방어: if (order.getStatus() != ChargeStatus.PAID) throw ...; 로 상태를 직접 검사한다.

        long refundAmount = order.getAmount();

        Wallet wallet = walletRepository.findByOwnerId(order.getOwnerId())
                .orElseGet(() -> walletRepository.save(new Wallet(order.getOwnerId())));

        // [R-2] 잔액 음수 방지 — 돈이라 이것만은 막는다. 환불액이 현재 잔액보다 크면 거부.
        if (wallet.getBalance() < refundAmount) {
            throw new IllegalArgumentException("환불 가능 잔액이 부족합니다.");
        }

        // 카드 지정 환불(payout) — mock PG.
        String maskedCard = null;
        String pgRefundId = null;
        if (cardId != null) {
            // [VULN-WALLET-7] 환불 목적지 카드 소유권 미검증(IDOR).
            //   findByIdAndOwnerId(cardId, order.getOwnerId()) 로 '주문 소유자의 카드'만 허용해야 하지만,
            //   findById 로 아무 카드나 받아 준다. → 공격자가 자기 계정에 등록한 카드 id 를 지정해,
            //     탈취한 부모 계정 잔액을 자기 카드로 환불 수취할 수 있다.
            //   방어: cardRepository.findByIdAndOwnerId(cardId, order.getOwnerId()).orElseThrow(...);
            Card card = cardRepository.findById(cardId)
                    .orElseThrow(() -> new ResourceNotFoundException("환불받을 카드를 찾을 수 없습니다."));
            PgRefund pg = pgClient.refund(merchantUid, refundAmount, card.maskedCardNumber());
            maskedCard = pg.maskedCard();
            pgRefundId = pg.pgRefundId();
        }

        order.markRefunded();
        wallet.debit(refundAmount);
        walletRepository.save(wallet);

        String memo = pgRefundId == null ? null : "카드환불 " + pgRefundId + " → " + maskedCard;
        WalletTransaction tx = walletTransactionRepository.save(
                new WalletTransaction(wallet.getId(), TxType.REFUND, refundAmount,
                        wallet.getBalance(), order.getMerchantUid(), memo));

        return new RefundResponse(order.getMerchantUid(), refundAmount, wallet.getBalance(),
                maskedCard, pgRefundId, TxResponse.from(tx));
    }
}
