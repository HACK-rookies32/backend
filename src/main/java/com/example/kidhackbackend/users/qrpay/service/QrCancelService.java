package com.example.kidhackbackend.users.qrpay.service;

import com.example.kidhackbackend.global.exception.ResourceNotFoundException;
import com.example.kidhackbackend.global.notification.entity.NotificationType;
import com.example.kidhackbackend.global.notification.service.NotificationPort;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import com.example.kidhackbackend.users.qrpay.dto.QrCancelResponse;
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
 * 매장결제 취소 (QR) — QR결제 No.46. No.44 승인의 역연산.
 *
 * <p>부모가 자녀의 PAID 결제를 취소한다: 자녀 지갑 환급(credit, PAYMENT_CANCEL) → QR CANCELLED → 부모 알림.
 * 본인 자녀 결제만 취소 가능해야 한다(정상). 원장은 users.wallet(WalletTransaction)을 재사용한다.
 */
@Service
@RequiredArgsConstructor
public class QrCancelService {

    private final QrPaymentRepository qrPaymentRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final MemberRepository memberRepository;
    private final NotificationPort notificationPort;

    /**
     * 매장결제 취소: qrToken의 결제를 취소하고 자녀 지갑에 전액 환급한다.
     *
     * @param parentId 취소를 요청한(로그인한) 부모 id
     * @param qrToken  취소할 결제 QR 토큰
     */
    @Transactional
    public QrCancelResponse cancel(Long parentId, String qrToken) {
        QrPayment qr = qrPaymentRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new ResourceNotFoundException("결제를 찾을 수 없습니다."));

        // [VULN-WALLET-9] 취소 소유권/페어링 미검증(IDOR / BOLA).
        //   결제한 자녀(qr.getChildId())가 이 부모(parentId)의 자녀인지 확인하지 않는다('본인 자녀 결제만' 위반).
        //   → 아무 PARENT나 qrToken만 알면 남의 자녀 결제를 취소해, 그 자녀 지갑에 임의로 환급(자금 조작)할 수 있다.
        //   방어: memberRepository.findById(qr.getChildId()) 후 child.getParent().getId()==parentId 확인.

        // [VULN-WALLET-10] 취소 상태·멱등 미검증.
        //   status가 PAID인지 확인하지 않아 이미 CANCELLED인 결제를 반복 취소할 수 있다.
        //   → 같은 qrToken으로 취소를 N번 호출하면 자녀 잔액이 결제액의 N배로 환급된다(자금 창출).
        //   방어: if (qr.getStatus() != QrStatus.PAID) throw ...; + 멱등/@Version 락.

        long cancelAmount = (qr.getAmount() != null) ? qr.getAmount() : 0L;
        qr.markCancelled();

        Wallet wallet = walletRepository.findByOwnerId(qr.getChildId())
                .orElseGet(() -> walletRepository.save(new Wallet(qr.getChildId())));
        wallet.credit(cancelAmount);
        walletRepository.save(wallet);

        WalletTransaction tx = walletTransactionRepository.save(
                new WalletTransaction(wallet.getId(), TxType.PAYMENT_CANCEL, cancelAmount,
                        wallet.getBalance(), qrToken));

        // 부모 알림: 자녀의 부모를 찾아 취소 알림 발행(부모 없으면 건너뜀).
        memberRepository.findById(qr.getChildId())
                .map(Member::getParent)
                .ifPresent(parent -> notificationPort.publish(parent.getId(), qr.getChildId(),
                        NotificationType.PAYMENT_CANCELED,
                        "자녀의 " + qr.getStoreName() + " 결제(" + cancelAmount + "원)가 취소되었습니다."));

        return new QrCancelResponse(qr.getQrToken(), qr.getStatus().name(), cancelAmount,
                wallet.getBalance(), TxResponse.from(tx));
    }
}
