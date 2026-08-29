package com.example.kidhackbackend.users.qrpay.service;

import com.example.kidhackbackend.global.exception.ResourceNotFoundException;
import com.example.kidhackbackend.global.notification.entity.NotificationType;
import com.example.kidhackbackend.global.notification.service.NotificationPort;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import com.example.kidhackbackend.users.qrpay.dto.QrApproveResponse;
import com.example.kidhackbackend.users.qrpay.entity.QrPayment;
import com.example.kidhackbackend.users.qrpay.entity.StoreTerminal;
import com.example.kidhackbackend.users.qrpay.repository.QrPaymentRepository;
import com.example.kidhackbackend.users.wallet.dto.TxResponse;
import com.example.kidhackbackend.users.wallet.entity.TxType;
import com.example.kidhackbackend.users.wallet.entity.Wallet;
import com.example.kidhackbackend.users.wallet.entity.WalletTransaction;
import com.example.kidhackbackend.users.wallet.repository.WalletRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletTransactionRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * QR 스캔 결제 승인 — QR결제 No.44 (핵심). 매장 단말이 자녀 QR을 스캔해 결제를 확정한다.
 *
 * <p>정상 흐름: 단말 인증(X-Store-Terminal-Token) → QR 검증(존재·미만료·미사용) → 자녀 잔액 ≥ 금액 →
 * 자녀 지갑 차감(PAYMENT 원장) → QR PAID 전이(금액·매장·단말·시각 확정) → 부모 알림.
 * 원장은 users.wallet(WalletTransaction)을 재사용한다.
 */
@Service
@RequiredArgsConstructor
public class QrApprovalService {

    private final StoreTerminalService storeTerminalService;
    private final QrPaymentRepository qrPaymentRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final MemberRepository memberRepository;
    private final NotificationPort notificationPort;

    /**
     * QR 스캔 결제 승인.
     *
     * @param terminalToken 매장 단말 토큰(X-Store-Terminal-Token 헤더)
     * @param qrToken       스캔한 QR 토큰
     * @param amount        단말이 입력한 결제 금액(원)
     */
    @Transactional
    public QrApproveResponse approve(String terminalToken, String qrToken, long amount) {
        // [VULN-WALLET-15] 단말 토큰 검증 우회(옵셔널 호출 — 웹훅 VULN-4와 동형).
        //   X-Store-Terminal-Token이 없으면(null/blank) verify()를 통째로 건너뛴다.
        //   → 미인증 외부인이 헤더 없이 이 엔드포인트를 호출해 임의 자녀 지갑을 결제(자금 이동)할 수 있다.
        //   (헤더가 '있는데 틀리면' verify가 거부하므로, '없으면 통과'가 취약점 — VULN-4와 같은 패턴)
        //   방어: 단말 토큰 필수화 + 항상 verify() 호출·실패 시 거부.
        StoreTerminal terminal = null;
        if (terminalToken != null && !terminalToken.isBlank()) {
            terminal = storeTerminalService.verify(terminalToken);
        }

        // QR 존재 검증(정상): 없으면 404.
        QrPayment qr = qrPaymentRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new ResourceNotFoundException("QR을 찾을 수 없습니다."));

        // [VULN-WALLET-16] QR 미사용·멱등 미검증.
        //   status가 ISSUED(미사용)인지 확인하지 않아 이미 PAID된 QR을 다시 승인할 수 있다.
        //   → 같은 qrToken을 재전송/재사용하면 자녀 잔액이 반복 차감된다(중복 결제).
        //   방어: if (qr.getStatus() != QrStatus.ISSUED) throw ...; + 멱등/@Version 락.

        // [VULN-WALLET-17] QR 만료 미검증.
        //   qr.isExpired(now)를 확인하지 않아 60초가 지난 만료 QR도 승인된다(1회성·만료 무력화).
        //   방어: if (qr.isExpired(LocalDateTime.now())) throw ...;

        Wallet wallet = walletRepository.findByOwnerId(qr.getChildId())
                .orElseGet(() -> walletRepository.save(new Wallet(qr.getChildId())));

        // [VULN-WALLET-18] 자녀 잔액 미검증.
        //   wallet.getBalance() >= amount 확인 없이 차감해 잔액이 음수가 될 수 있다(초과 결제).
        //   방어: if (wallet.getBalance() < amount) throw ...(잔액 부족 거부);
        wallet.debit(amount);
        walletRepository.save(wallet);

        WalletTransaction tx = walletTransactionRepository.save(
                new WalletTransaction(wallet.getId(), TxType.PAYMENT, amount, wallet.getBalance(), qrToken));

        String storeName = (terminal != null) ? terminal.getStoreName() : "UNKNOWN";
        Long terminalId = (terminal != null) ? terminal.getId() : null;
        qr.markPaid(amount, storeName, terminalId, LocalDateTime.now());

        // 부모 알림: 자녀의 부모를 찾아 결제 승인 알림을 발행(부모가 없으면 건너뜀).
        memberRepository.findById(qr.getChildId())
                .map(Member::getParent)
                .ifPresent(parent -> notificationPort.publish(parent.getId(), qr.getChildId(),
                        NotificationType.PAYMENT_APPROVED,
                        "자녀가 " + storeName + "에서 " + amount + "원을 결제했습니다."));

        return new QrApproveResponse(qr.getQrToken(), qr.getStatus().name(), amount,
                storeName, wallet.getBalance(), TxResponse.from(tx));
    }
}
