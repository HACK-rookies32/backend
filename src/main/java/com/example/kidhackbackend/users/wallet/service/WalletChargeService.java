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

        // [VULN-WALLET-2] 주문 소유권 미검증(IDOR / BOLA).
        //   위에서 merchantUid 로 주문을 찾아왔을 뿐, 그 주문의 주인(order.getOwnerId())이
        //   지금 요청한 사람(requesterId)과 같은지 확인하지 않는다. 넘겨받은 requesterId 가
        //   이 메서드 어디에서도 쓰이지 않는다는 점을 보면 결함이 분명히 드러난다.
        //   → 주문번호만 알면 아무 PARENT 나 남의 준비 주문을 대신 승인·확정할 수 있다.
        //
        //   ※ 헷갈리기 쉬운 부분 — "그래서 공격자가 돈을 버나?" 아니다.
        //     아래 :115 가 요청자가 아니라 order.getOwnerId() 의 지갑을 꺼내므로, 적립된 돈은
        //     주문 주인(피해자)에게 간다. 공격자 지갑은 늘지 않는다. 그런데도 취약점인 이유:
        //       ① 사업자 손실 — 승인은 "이 사람이 PG 에서 실제로 결제를 마쳤다"를 확정하는 행위다.
        //          피해자가 결제를 포기한 주문까지 남이 확정시킬 수 있으니, 실제로 받지 않은 돈이
        //          쓸 수 있는 잔액으로 발행된다. 그 잔액을 매장에서 쓰면 가맹점 정산은 진짜
        //          현금으로 나간다. 손해 보는 쪽은 공격자가 아니라 서비스다.
        //       ② 피해자의 취소권 박탈 — 위 cancel() 은 READY 주문만 취소를 허용한다. 공격자가
        //          먼저 승인해 PAID 로 만들면 정작 주인은 자기 주문을 취소할 수 없게 된다.
        //       ③ 추적 불가·책임 전가 — 아래에서 남기는 WalletTransaction 에는 "누가 승인했는지"
        //          필드가 없다. 피해자 원장에는 피해자가 충전한 것처럼만 찍히고 공격자 흔적은
        //          남지 않는다. 나중에 부정 충전을 조사하면 피해자가 범인으로 보인다.
        //       ④ [VULN-WALLET-1](금액 변조)과 합치면 남의 잔액을 원하는 만큼 올릴 수 있고,
        //          [VULN-WALLET-5](남의 충전 환불)와 합치면 내릴 수도 있다. 두 개를 합치면
        //          사실상 임의 계정 잔액에 대한 쓰기 권한이 된다.
        //
        //   전제: merchantUid 는 prepare 에서 UUID 로 만들어져 추측할 수 없다. 그래서 실제
        //     공격은 주문번호가 로그·화면·다른 정보노출 취약점으로 새는 것이 전제다. 하지만
        //     추측 불가능한 값에 기대는 것은 인가가 아니라 '모호함에 기댄 보안'일 뿐이다.
        //     실제로 같은 클래스의 cancel(:77) 은 같은 merchantUid 를 받으면서도 소유권을 검증한다.
        //   방어: if (!order.getOwnerId().equals(requesterId)) throw new AccessDenied...;

        PgApproval approval = pgClient.approve(order.getMerchantUid(), request.amount());

        // [VULN-WALLET-3] 같은 요청을 다시 보내면 다시 처리된다(재전송·replay 방어 부재).
        //   "멱등하다"는 같은 요청을 몇 번 보내도 결과가 한 번 보낸 것과 같다는 뜻이다. 결제 승인은
        //   반드시 멱등해야 하는데(네트워크가 끊겨 앱이 재시도하는 일이 흔하다) 여기엔 그 장치가 없다.
        //   바로 아래 markPaid 앞에 "이 주문이 아직 READY 인가?"를 보는 검사가 없어서, 이미 결제
        //   완료된 주문이어도 그냥 한 번 더 적립한다.
        //   → 같은 merchantUid 로 approve 를 N번 부르면 잔액이 N배가 된다. 2026-09-07 모의해킹에서
        //     두 번 호출해 1,000,000 → 2,000,000 이 되는 것을 확인했다. 남의 주문일 필요도 없이
        //     자기 주문만으로 성립한다. 동시에 들어온 요청 하나만 통과시키는 락(@Version)도 없다.
        //   참고: 같은 클래스의 웹훅 경로(confirmByWebhook)에는 [W-2] 가드가 있어 막혀 있다.
        //         즉 이 결함은 두 경로 중 approve 쪽에만 남아 있다.
        //   방어: if (order.getStatus() != ChargeStatus.READY) return 기존결과; + @Version 락.
        order.markPaid(approval.pgTransactionId());

        Wallet wallet = walletRepository.findByOwnerId(order.getOwnerId())
                .orElseGet(() -> walletRepository.save(new Wallet(order.getOwnerId())));

        // [VULN-WALLET-1] 결제 금액 변조 — 이 도메인의 핵심 결함.
        //   적립할 금액을 서버가 가진 값이 아니라 클라이언트가 보낸 request.amount() 로 정한다.
        //   서버는 이미 믿을 수 있는 금액을 갖고 있는데(prepare 에서 저장한 order.getAmount())
        //   그것과 대조조차 하지 않는다.
        //   → 100원짜리 주문을 amount=1000000 으로 승인 요청하면 그대로 100만원이 충전된다.
        //     2026-09-07 모의해킹에서 실제로 성립했다.
        //
        //   ※ "PG 응답과 대조하면 되지 않나?" — 이 환경에서는 그것만으로 못 막는다.
        //     위 :107 이 부르는 pgClient 는 실습용 MockPgClient 이고, 그 approve() 는 넘겨받은
        //     amount 를 그대로 paidAmount 에 담아 돌려준다(메아리처럼 되돌아온다).
        //     게다가 :107 이 넘기는 값이 request.amount(), 즉 공격자가 보낸 그 숫자다. 그러니
        //     approval.paidAmount() 와 request.amount() 를 비교해 봐야 항상 같아서 아무것도 못 잡는다.
        //     신뢰할 수 있는 값은 서버 DB 에 있는 order.getAmount() 하나뿐인데, 이 경로는 그걸 안 본다.
        //
        //   방어(순서대로):
        //     ① 적립액을 request.amount() 대신 order.getAmount() 로 바꾼다. Mock 이든 실제 PG 든 효과가 있다.
        //     ② 실제 PG 연동 시 approval.paidAmount() == order.getAmount() 교차 검증을 추가한다.
        //        (지금 넣어도 Mock 이라 의미가 없으므로 ①이 먼저다.)
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
        //   아래 if 조건이 (sigProvided && !verify(...)) 이라 앞부분이 거짓이면 verify() 를 아예
        //   호출하지 않는다. 서명을 '틀리게' 보내면 400 으로 막히는데, '아예 빼면' 무사통과다.
        //   방어 장치를 조건부로 만들어 둔 탓에 헤더를 생략하는 쪽이 더 쉬운 공격이 되어 버렸다.
        //
        //   이 결함이 특히 위험한 이유는 이 엔드포인트가 PG 서버가 부르는 콜백이라
        //   PgWebhookSecurityConfig 에서 /api/pg/** 를 permitAll 로 열어 두었기 때문이다.
        //   즉 로그인도 토큰도 필요 없다 — 인터넷에서 누구나 부를 수 있고, 진짜 PG 가 보낸
        //   것인지 가려낼 수단이 오직 이 서명 하나뿐인데 그 하나를 건너뛸 수 있다.
        //   → 2026-09-07 모의해킹에서 X-PG-Signature 헤더 없이 status:paid, paidAmount:777777 을
        //     보내 잔액을 777,777원 늘리는 데 성공했다.
        //   방어: 서명 헤더를 필수로 강제(없으면 즉시 400) + 조건 없이 항상 verify().
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
