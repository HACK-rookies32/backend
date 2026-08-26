package com.example.kidhackbackend.admin.finance.service;

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
 * <p>⚠️ 역할표상 두 기능 모두 "감사로그 자동기록"이 구현 포인트지만, 감사로그 모듈이 없어 제외한다
 * (그 부재가 {@code [VULN-ADMIN-4]}). 자금이 실제로 움직이므로 정상 운영이라면 감사로그 필수.
 */
@Service
@RequiredArgsConstructor
public class AdminFinanceCommandService {

    private final QrPaymentRepository qrPaymentRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

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

        // [VULN-ADMIN-4] 감사 추적 부재: 관리자 강제환불이 로그로 남지 않아 오남용 추적/부인방지 불가.
        //   TODO(감사로그): 관리자 id·대상·금액·사유·시각을 audit_log에 기록.

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

        // [VULN-ADMIN-4] 감사 추적 부재 — TODO(감사로그): 관리자 id·대상·금액·사유 기록.

        return new AdminActionResponse(userId, wallet.getBalance(), TxResponse.from(tx));
    }
}
