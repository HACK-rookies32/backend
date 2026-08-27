package com.example.kidhackbackend.admin.finance.service;

import com.example.kidhackbackend.admin.audit.entity.AdminActivityType;
import com.example.kidhackbackend.admin.audit.service.AdminAuditLogService;
import com.example.kidhackbackend.admin.finance.dto.AdminActionResponse;
import com.example.kidhackbackend.global.exception.ResourceNotFoundException;
import com.example.kidhackbackend.users.qrpay.entity.QrPayment;
import com.example.kidhackbackend.users.qrpay.repository.QrPaymentRepository;
import com.example.kidhackbackend.users.wallet.dto.TxResponse;
import com.example.kidhackbackend.users.wallet.entity.TxType;
import com.example.kidhackbackend.users.wallet.entity.Wallet;
import com.example.kidhackbackend.users.wallet.entity.WalletTransaction;
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
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
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
        // [VULN-ADMIN-2] 조정 사유·한도 미검증.
        //   reason이 비어도, amount가 음수/거대 금액이어도 그대로 조정한다(무제한 자금 창출·삭감).
        //   방어: reason 필수(blank 거부) + 조정 한도·부호 정책 검증.

        Wallet wallet = walletRepository.findByOwnerId(userId)
                .orElseGet(() -> walletRepository.save(new Wallet(userId)));
        wallet.credit(amount); // amount 음수면 감액
        walletRepository.save(wallet);

        WalletTransaction tx = walletTransactionRepository.save(
                new WalletTransaction(wallet.getId(), TxType.ADJUST, amount, wallet.getBalance(), reason));

        // 감사로그: 누가 어느 지갑을 손으로 조정했는지 남긴다(대상은 지갑).
        adminAuditLogService.record(AdminActivityType.WALLET_BALANCE_ADJUST, wallet.getId());

        return new AdminActionResponse(userId, wallet.getBalance(), TxResponse.from(tx));
    }
}
