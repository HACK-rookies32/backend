package com.example.kidhackbackend.admin.finance.service;

import com.example.kidhackbackend.admin.audit.entity.AdminActivityType;
import com.example.kidhackbackend.admin.audit.service.AdminAuditLogService;
import com.example.kidhackbackend.admin.finance.dto.AdminActionResponse;
import com.example.kidhackbackend.global.exception.DuplicateResourceException;
import com.example.kidhackbackend.global.exception.ResourceNotFoundException;
import com.example.kidhackbackend.users.qrpay.entity.QrPayment;
import com.example.kidhackbackend.users.qrpay.repository.QrPaymentRepository;
import com.example.kidhackbackend.users.wallet.dto.RefundResponse;
import com.example.kidhackbackend.users.wallet.dto.TxResponse;
import com.example.kidhackbackend.users.wallet.entity.ChargeOrder;
import com.example.kidhackbackend.users.wallet.entity.ChargeStatus;
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
 * 관리자 자금 변경 — 역할표 No.48(강제환불)·No.50(잔액 수동조정). SUPER_ADMIN 전용(의도).
 *
 * <p>역할표상 두 기능 모두 "감사로그 자동기록"이 구현 포인트다. 다른 admin 기능은 컨트롤러에서
 * 감사로그를 남기지만, 이 둘만 서비스 안에서 남긴다 — 기록할 대상(결제 id·지갑 id)이 여기서야 확정되고,
 * 돈이 움직인 트랜잭션과 같은 범위에서 커밋돼야 "잔액은 바뀌었는데 이력은 없다" 가 생기지 않기 때문이다.
 */
@Service
@RequiredArgsConstructor
public class AdminFinanceCommandService {

    private final QrPaymentRepository qrPaymentRepository;
    private final ChargeOrderRepository chargeOrderRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final com.example.kidhackbackend.users.member.repository.MemberRepository memberRepository;
    private final AdminAuditLogService adminAuditLogService;

    /** No.48 강제환불: 자녀 QR 결제를 강제 환불(자녀 지갑 원복 + REFUND). */
    @Transactional
    public AdminActionResponse forceRefund(String qrToken) {
        QrPayment qr = qrPaymentRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new ResourceNotFoundException("결제를 찾을 수 없습니다."));

        // [VULN-ADMIN-3] 강제환불 상태·멱등 미검증.
        //   status가 PAID인지 확인하지 않아 이미 취소/환불된 결제를 재환불할 수 있다.
        //   → 같은 qrToken으로 반복 호출하면 자녀 잔액이 결제액의 배수로 환급된다(자금 창출).
        //   방어: if (qr.getStatus() != QrStatus.PAID) throw ...; + 멱등/@Version 락.

        long amount = (qr.getAmount() != null) ? qr.getAmount() : 0L;
        qr.markCancelled();

        Wallet wallet = walletRepository.findByOwnerId(qr.getChildId())
                .orElseGet(() -> walletRepository.save(new Wallet(qr.getChildId())));
        wallet.credit(amount);
        walletRepository.save(wallet);

        WalletTransaction tx = walletTransactionRepository.save(
                new WalletTransaction(wallet.getId(), TxType.REFUND, amount, wallet.getBalance(), qrToken));

        // 감사로그: 누가 어느 결제를 강제환불했는지 남긴다(대상은 결제 건).
        adminAuditLogService.record(AdminActivityType.PAYMENT_REFUND, qr.getId());

        return new AdminActionResponse(qr.getChildId(), wallet.getBalance(), TxResponse.from(tx));
    }

    /** No.50 잔액 수동조정: 특정 회원 잔액을 가감(+/-)하고 ADJUST 원장을 남긴다. */
    @Transactional
    public AdminActionResponse adjust(Long userId, long amount, String reason) {
        // 없는 회원에 조정하면 지갑을 새로 만들어 200 이 나가던 문제(#29) → 존재검증으로 404.
        // (VULN-ADMIN-2 의 '거대 금액 자금 창출' 표적은 실재 회원 대상으로 그대로 유지된다.)
        if (!memberRepository.existsById(userId)) {
            throw new ResourceNotFoundException("존재하지 않는 회원입니다: " + userId);
        }

        // [VULN-ADMIN-2] 조정 사유·한도 미검증.
        //   reason이 비어도, amount가 음수/거대 금액이어도 그대로 조정한다(무제한 자금 창출·삭감).
        //   방어: reason 필수(blank 거부) + 조정 한도·부호 정책 검증.

        Wallet wallet = walletRepository.findByOwnerId(userId)
                .orElseGet(() -> walletRepository.save(new Wallet(userId)));
        wallet.credit(amount); // amount 음수면 감액
        walletRepository.save(wallet);

        // reason 이 refUid 자리에 들어가 있던 것은 규약 위반이다 — refUid 는 기계가 읽는 참조 키다.
        // V15 의 memo 로 옮기되, 웹이 refUid 를 읽고 있을 수 있어 당분간 양쪽에 모두 넣는다.
        // (웹 회신에서 refUid 는 추후 제거 예정으로 안내했다)
        WalletTransaction tx = walletTransactionRepository.save(
                new WalletTransaction(wallet.getId(), TxType.ADJUST, amount, wallet.getBalance(),
                        reason, reason));

        // 감사로그: 누가 어느 지갑을 손으로 조정했는지 + 왜(#173, QA #10).
        // 사유를 받아 놓고 넘기지 않아 description 이 null 이었다 — 충전 취소(아래)는 남기는데
        // 잔액 조정만 빠져 있었다. 돈을 수동 조작하는 기능이라 사유 없는 감사기록은 쓸모가 없다.
        adminAuditLogService.record(AdminActivityType.WALLET_BALANCE_ADJUST, wallet.getId(), reason);

        return new AdminActionResponse(userId, wallet.getBalance(), TxResponse.from(tx));
    }

    /**
     * #139 관리자 충전 취소: 완료된 충전을 관리자 권한으로 되돌린다(프론트 3차 1번).
     *
     * <p>부모용 {@code WalletRefundService.refund} 와 결과는 같지만 <b>일부러 재사용하지 않고</b>
     * 여기에 따로 둔다. 부모용에는 의도된 취약점 두 개가 심겨 있어서다.
     * <ul>
     *   <li>{@code [VULN-WALLET-5]} 소유권 미검증 — 관리자 경로는 남의 주문을 다루는 게 정상이라 무의미</li>
     *   <li>{@code [VULN-WALLET-6]} 상태 미검증 — <b>이건 물려받으면 안 된다</b>. 잔액만 남아 있으면
     *       이미 REFUNDED 인 주문을 다시 취소해 이중 환불(자금 창출)이 된다.</li>
     * </ul>
     * 관리자 자금 조작은 감사 대상이라 장부가 깨지면 안 되므로 {@code PAID} 검증을 넣는다.
     * 모의해킹 소재는 {@code VULN-ADMIN-1/3} 으로 이미 충분하다.
     *
     * <p>부모용 메서드는 취약점 유지를 위해 <b>손대지 않는다</b>.
     *
     * @param merchantUid 취소할 충전 주문번호(거래내역 {@code CHARGE} 행의 {@code refUid})
     * @param reason      취소 사유 — 감사로그에 그대로 남는다. 공백은 컨트롤러의 {@code @NotBlank} 가 막는다
     */
    @Transactional
    public RefundResponse cancelCharge(String merchantUid, String reason) {
        ChargeOrder order = chargeOrderRepository.findByMerchantUid(merchantUid)
                .orElseThrow(() -> new ResourceNotFoundException("충전 주문을 찾을 수 없습니다."));

        // 상태 검증 — 부모용에 없는(=[VULN-WALLET-6] 를 물려받지 않는) 방어.
        // READY/FAILED/EXPIRED 는 애초에 돈이 들어온 적이 없고, REFUNDED/CANCELLED 는 이미 되돌린 건이다.
        if (order.getStatus() != ChargeStatus.PAID) {
            throw new DuplicateResourceException(
                    "이미 처리되었거나 취소할 수 없는 충전입니다. 현재 상태: " + order.getStatus());
        }

        long refundAmount = order.getAmount();

        Wallet wallet = walletRepository.findByOwnerId(order.getOwnerId())
                .orElseGet(() -> walletRepository.save(new Wallet(order.getOwnerId())));

        // 잔액 음수 방지 — 이미 써버린 돈은 되돌릴 수 없다. 프론트 요청대로 409 로 내보낸다.
        if (wallet.getBalance() < refundAmount) {
            throw new DuplicateResourceException(
                    "환불 가능 잔액이 부족합니다. 현재 잔액: " + wallet.getBalance() + ", 환불액: " + refundAmount);
        }

        // 사유·시각을 주문에도 남긴다(#145). 감사로그에도 같은 사유가 가지만 목적이 다르다 —
        // 감사로그는 '누가 했는가'(부인방지), 주문 필드는 '주문이 어떻게 됐는가'(업무 상태)다.
        // 충전 목록(GET /finance/charges)이 감사로그를 문자열로 뒤지지 않아도 되게 하는 것이 요점이다.
        order.markRefunded(reason);
        wallet.debit(refundAmount);
        walletRepository.save(wallet);

        // 원장의 refUid 는 merchantUid 여야 한다 — 프론트가 kid_ 접두사로 '충전 환불' 을 식별한다(#131 확정).
        // 그래서 취소 사유는 이 자리에 넣을 수 없고 memo 로 간다(V15). 감사로그 description 에도 같은
        // 사유가 남지만 목적이 다르다 — 감사로그는 '누가 했는가'(부인방지), memo 는 '이 거래가 무엇인가'다.
        WalletTransaction tx = walletTransactionRepository.save(
                new WalletTransaction(wallet.getId(), TxType.REFUND, refundAmount,
                        wallet.getBalance(), order.getMerchantUid(), reason));

        // 감사로그: 누가 · 어느 지갑에서 · 왜 충전을 되돌렸는지. 돈이 움직인 트랜잭션과 같은 범위에서
        // 커밋돼야 "잔액은 바뀌었는데 이력은 없다" 가 생기지 않는다.
        adminAuditLogService.record(AdminActivityType.CHARGE_CANCEL, wallet.getId(),
                "[" + merchantUid + "] " + reason);

        return new RefundResponse(order.getMerchantUid(), refundAmount, wallet.getBalance(), TxResponse.from(tx));
    }
}
