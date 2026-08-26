package com.example.kidhackbackend.users.wallet.service;

import com.example.kidhackbackend.global.exception.ResourceNotFoundException;
import com.example.kidhackbackend.users.wallet.dto.ChargeApproveRequest;
import com.example.kidhackbackend.users.wallet.dto.ChargeApproveResponse;
import com.example.kidhackbackend.users.wallet.dto.ChargePrepareRequest;
import com.example.kidhackbackend.users.wallet.dto.ChargePrepareResponse;
import com.example.kidhackbackend.users.wallet.dto.PgWebhookRequest;
import com.example.kidhackbackend.users.wallet.dto.TxResponse;
import com.example.kidhackbackend.users.wallet.entity.ChargeOrder;
import com.example.kidhackbackend.users.wallet.entity.TxType;
import com.example.kidhackbackend.users.wallet.entity.Wallet;
import com.example.kidhackbackend.users.wallet.entity.WalletTransaction;
import com.example.kidhackbackend.users.wallet.pg.PgApproval;
import com.example.kidhackbackend.users.wallet.pg.PgClient;
import com.example.kidhackbackend.users.wallet.repository.ChargeOrderRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletTransactionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PG Mock 지갑 충전 — 역할표 No.35 (준비/승인 2단계).
 *
 * <p>준비(prepare): 결제 인텐트(ChargeOrder, READY) 생성 + merchantUid 발급.
 * 승인(approve): PG Mock 승인 확인 → 주문 PAID → 잔액 적립 → 원장(WalletTransaction) 기록.
 */
@Service
@RequiredArgsConstructor
public class WalletChargeService {

    private final WalletRepository walletRepository;
    private final ChargeOrderRepository chargeOrderRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final PgClient pgClient;

    /** 충전 준비: 로그인한 회원 소유로 READY 주문을 만들고 merchantUid를 돌려준다. */
    @Transactional
    public ChargePrepareResponse prepare(Long ownerId, ChargePrepareRequest request) {
        // 지갑이 없으면 이 시점에 0원으로 생성(1인 1지갑).
        walletRepository.findByOwnerId(ownerId)
                .orElseGet(() -> walletRepository.save(new Wallet(ownerId)));

        String merchantUid = "kid_" + UUID.randomUUID().toString().replace("-", "");
        ChargeOrder order = chargeOrderRepository.save(
                new ChargeOrder(merchantUid, ownerId, request.amount()));

        return ChargePrepareResponse.from(order);
    }

    /**
     * 충전 승인: 준비된 주문을 PG 승인 확인 후 확정하고 잔액을 적립한다.
     *
     * @param requesterId 승인을 요청한(로그인한) 회원 id
     */
    @Transactional
    public ChargeApproveResponse approve(Long requesterId, ChargeApproveRequest request) {
        ChargeOrder order = chargeOrderRepository.findByMerchantUid(request.merchantUid())
                .orElseThrow(() -> new ResourceNotFoundException("충전 주문을 찾을 수 없습니다."));

        // [VULN-WALLET-2] 소유권 미검증(IDOR / Broken Object Level Authorization).
        //   주문의 소유자(order.getOwnerId())와 요청자(requesterId)가 같은지 확인하지 않는다.
        //   → merchantUid만 알면 아무 PARENT나 남의 준비 주문을 승인·확정할 수 있고,
        //     아래에서 잔액이 order.getOwnerId()의 지갑으로 들어가므로 타인 지갑을 조작하게 된다.
        //   방어: if (!order.getOwnerId().equals(requesterId)) throw new AccessDenied...;

        PgApproval approval = pgClient.approve(order.getMerchantUid(), request.amount());

        // [VULN-WALLET-3] 멱등성/재생(replay) 방어 부재.
        //   status가 이미 PAID인지 확인하지 않고 무조건 다시 적립한다. 동일 merchantUid로
        //   approve를 N번 호출하면 잔액이 N배로 불어난다(중복 충전). 낙관적 락/멱등키도 없다.
        //   방어: if (order.getStatus() != ChargeStatus.READY) return 기존결과; + @Version 락.
        order.markPaid(approval.pgTransactionId());

        Wallet wallet = walletRepository.findByOwnerId(order.getOwnerId())
                .orElseGet(() -> walletRepository.save(new Wallet(order.getOwnerId())));

        // [VULN-WALLET-1] 결제 금액 변조(핵심).
        //   준비 시 확정된 order.getAmount()나 PG가 보고한 실제 결제액을 서버측에서 대조하지 않고,
        //   클라이언트가 보낸 request.amount()를 그대로 적립한다(Mock PG도 이 값을 그대로 에코).
        //   → 100원짜리 주문을 amount=1000000으로 승인 요청하면 100만원이 충전된다.
        //   방어: 적립액은 order.getAmount() 사용 + approval.paidAmount()==order.getAmount() 검증.
        long creditAmount = request.amount();
        wallet.credit(creditAmount);
        walletRepository.save(wallet);

        WalletTransaction tx = walletTransactionRepository.save(
                new WalletTransaction(wallet.getId(), TxType.CHARGE, creditAmount,
                        wallet.getBalance(), order.getMerchantUid()));

        return new ChargeApproveResponse(order.getMerchantUid(), wallet.getBalance(), TxResponse.from(tx));
    }

    /**
     * PG 웹훅 확정 — 역할표 No.36. 서명 검증 후 주문을 확정(PAID)하고 잔액을 적립한다.
     *
     * <p>PG 서버가 서버-투-서버로 호출하는 콜백. 앱 토큰이 아니라 서명으로 진위를 확인한다.
     *
     * @param rawBody   웹훅 원본 바디(서명 검증 대상)
     * @param signature 요청 헤더 {@code X-PG-Signature}
     * @param payload   파싱된 웹훅 페이로드
     */
    @Transactional
    public void confirmByWebhook(String rawBody, String signature, PgWebhookRequest payload) {
        // [VULN-WALLET-4] 웹훅 서명검증 우회.
        //   서명 헤더가 '있을 때만' 검증하고, 없으면(null/blank) 검증을 통째로 건너뛴다.
        //   → 공격자가 X-PG-Signature 헤더 없이 위조 웹훅 바디만 보내면 그대로 통과되어,
        //     임의 merchantUid를 원하는 paidAmount로 확정·적립할 수 있다(결제 위조).
        //   방어: 서명 헤더를 '필수'로 강제하고, 항상 verify() 실패 시 거부(상수시간 비교 권장).
        //         즉 `if (!pgClient.verify(rawBody, signature)) throw ...;` 로 무조건 검증.
        if (signature != null && !pgClient.verify(rawBody, signature)) {
            throw new IllegalArgumentException("유효하지 않은 웹훅 서명입니다.");
        }

        // 결제 성공 콜백만 확정한다. (취소/실패 상태 처리는 No.37에서 다룸)
        if (!"paid".equalsIgnoreCase(payload.status())) {
            return;
        }

        ChargeOrder order = chargeOrderRepository.findByMerchantUid(payload.merchantUid())
                .orElseThrow(() -> new ResourceNotFoundException("충전 주문을 찾을 수 없습니다."));

        // [VULN-WALLET-3] 멱등성/재생 방어 부재 — PG는 응답 실패 시 웹훅을 재전송한다.
        //   status 가드/멱등키 없이 매번 재적립하므로, 동일 웹훅 재전송·재생으로 중복 충전된다.
        //   방어: 이미 PAID면 재적립 금지(멱등 처리) + @Version 락.
        order.markPaid(payload.pgTransactionId());

        Wallet wallet = walletRepository.findByOwnerId(order.getOwnerId())
                .orElseGet(() -> walletRepository.save(new Wallet(order.getOwnerId())));

        // 웹훅이 통지한 결제액을 적립한다. 서명검증이 뚫리면([VULN-WALLET-4]) 이 금액도 공격자가 조작한다.
        long creditAmount = payload.paidAmount();
        wallet.credit(creditAmount);
        walletRepository.save(wallet);

        walletTransactionRepository.save(
                new WalletTransaction(wallet.getId(), TxType.CHARGE, creditAmount,
                        wallet.getBalance(), order.getMerchantUid()));
    }
}
