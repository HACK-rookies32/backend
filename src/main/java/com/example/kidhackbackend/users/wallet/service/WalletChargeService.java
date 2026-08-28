package com.example.kidhackbackend.users.wallet.service;

import com.example.kidhackbackend.global.exception.AdminAccessDeniedException;
import com.example.kidhackbackend.global.exception.ResourceNotFoundException;
import com.example.kidhackbackend.users.wallet.dto.ChargeApproveRequest;
import com.example.kidhackbackend.users.wallet.dto.ChargeApproveResponse;
import com.example.kidhackbackend.users.wallet.dto.ChargeOrderResponse;
import com.example.kidhackbackend.users.wallet.dto.ChargePrepareRequest;
import com.example.kidhackbackend.users.wallet.dto.ChargePrepareResponse;
import com.example.kidhackbackend.users.wallet.dto.PgWebhookRequest;
import com.example.kidhackbackend.users.wallet.dto.TxResponse;
import com.example.kidhackbackend.users.wallet.entity.ChargeOrder;
import com.example.kidhackbackend.users.wallet.entity.ChargeStatus;
import com.example.kidhackbackend.users.wallet.entity.TxType;
import com.example.kidhackbackend.users.wallet.entity.Wallet;
import com.example.kidhackbackend.users.wallet.entity.PgWebhookEvent;
import com.example.kidhackbackend.users.wallet.entity.WalletTransaction;
import com.example.kidhackbackend.users.wallet.pg.PgApproval;
import com.example.kidhackbackend.users.wallet.pg.PgCheckout;
import com.example.kidhackbackend.users.wallet.pg.PgClient;
import com.example.kidhackbackend.users.wallet.repository.ChargeOrderRepository;
import com.example.kidhackbackend.users.wallet.repository.PgWebhookEventRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletTransactionRepository;
import java.util.Set;
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
    private final PgWebhookEventRepository pgWebhookEventRepository;
    private final PgClient pgClient;

    /** 결제 실패로 볼 웹훅 status 값들. */
    private static final Set<String> FAILURE_STATUSES = Set.of("failed", "cancelled", "canceled", "aborted");

    /** 충전 준비: 로그인한 회원 소유로 READY 주문을 만들고 merchantUid를 돌려준다. */
    @Transactional
    public ChargePrepareResponse prepare(Long ownerId, ChargePrepareRequest request) {
        // 지갑이 없으면 이 시점에 0원으로 생성(1인 1지갑).
        walletRepository.findByOwnerId(ownerId)
                .orElseGet(() -> walletRepository.save(new Wallet(ownerId)));

        String merchantUid = "kid_" + UUID.randomUUID().toString().replace("-", "");
        ChargeOrder order = chargeOrderRepository.save(
                new ChargeOrder(merchantUid, ownerId, request.amount()));

        PgCheckout checkout = pgClient.checkout(merchantUid, request.amount());
        return ChargePrepareResponse.from(order, checkout);
    }

    /**
     * 결제 진행 중 취소 — 아직 승인되지 않은(READY) 주문만 CANCELLED 로 돌린다. 잔액 변화 없음.
     *
     * <p>승인/환불과 달리 이 신규 API 는 소유권을 검증한다(요청자 == 주문 소유자).
     *
     * @param requesterId 취소를 요청한(로그인한) 회원 id
     */
    @Transactional
    public ChargeOrderResponse cancel(Long requesterId, String merchantUid) {
        ChargeOrder order = chargeOrderRepository.findByMerchantUid(merchantUid)
                .orElseThrow(() -> new ResourceNotFoundException("충전 주문을 찾을 수 없습니다."));

        if (!order.getOwnerId().equals(requesterId)) {
            throw new AdminAccessDeniedException("본인 충전 주문만 취소할 수 있습니다.");
        }
        if (order.getStatus() == ChargeStatus.CANCELLED) {
            return ChargeOrderResponse.from(order); // 멱등
        }
        if (order.getStatus() != ChargeStatus.READY) {
            throw new IllegalArgumentException(
                    "결제가 진행되지 않은 주문만 취소할 수 있습니다. 이미 결제된 주문은 환불을 이용하세요.");
        }
        order.markCancelled();
        return ChargeOrderResponse.from(order);
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

        // [VULN-WALLET-3] 멱등성/재생(replay) 방어 부재 (approve 경로에만 남음 — 웹훅 경로는 confirmByWebhook 에서 멱등 처리).
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
        boolean sigProvided = signature != null && !signature.isBlank();

        // [VULN-WALLET-4] 웹훅 서명검증 우회 — 서명 헤더가 '있을 때만' 검증하고, 없으면 통째로 건너뛴다.
        //   → 공격자가 X-PG-Signature 헤더 없이 위조 바디만 보내면 그대로 통과되어 결제 위조 가능.
        //   방어: 서명 헤더를 필수로 강제 + 항상 verify().
        if (sigProvided && !pgClient.verify(rawBody, signature)) {
            // 잘못된 서명은 거부(400). 이 케이스는 트랜잭션이 롤백되므로 이벤트는 남기지 않는다.
            throw new IllegalArgumentException("유효하지 않은 웹훅 서명입니다.");
        }

        String status = payload.status() == null ? "" : payload.status().toLowerCase();
        ChargeOrder order = chargeOrderRepository.findByMerchantUid(payload.merchantUid()).orElse(null);

        if (order == null) {
            recordWebhook(payload, sigProvided, "ORDER_NOT_FOUND");
            return;
        }

        // [W-1] 결제 실패/취소 웹훅 반영 — READY 주문만 FAILED 로 전이(이미 처리된 건 건드리지 않음).
        if (FAILURE_STATUSES.contains(status)) {
            if (order.getStatus() == ChargeStatus.READY) {
                order.markFailed();
            }
            recordWebhook(payload, sigProvided, "MARKED_FAILED");
            return;
        }

        // 결제 성공("paid") 외의 상태는 무시한다.
        if (!"paid".equals(status)) {
            recordWebhook(payload, sigProvided, "IGNORED_STATUS");
            return;
        }

        // [W-2] 멱등 처리 — 이미 처리된 주문(READY 아님)이면 재적립하지 않는다.
        //   ([VULN-WALLET-3] 의 재생 취약점은 웹훅 경로에서는 이 가드로 막았고, approve 경로에만 남아 있다.)
        if (order.getStatus() != ChargeStatus.READY) {
            recordWebhook(payload, sigProvided, "IGNORED_NOT_READY");
            return;
        }

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

        recordWebhook(payload, sigProvided, "CONFIRMED");
    }

    /** [W-3] 웹훅 수신 기록. */
    private void recordWebhook(PgWebhookRequest payload, boolean signatureProvided, String outcome) {
        pgWebhookEventRepository.save(new PgWebhookEvent(
                payload.merchantUid(), payload.status(), payload.paidAmount(), signatureProvided, outcome));
    }
}
