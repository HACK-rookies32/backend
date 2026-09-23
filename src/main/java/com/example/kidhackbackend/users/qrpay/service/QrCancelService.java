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

        // [VULN-WALLET-9] 취소 소유권(부모-자녀 관계) 미검증(IDOR / BOLA).
        //   위에서 qrToken 으로 결제를 찾아왔을 뿐, 그 결제를 한 자녀(qr.getChildId())가 취소를
        //   요청한 부모(parentId)의 자녀인지 확인하지 않는다. '본인 자녀 결제만 취소' 원칙 위반이다.
        //   → 아무 PARENT 나 QR 토큰만 알면 남의 집 자녀 결제를 취소하고, 아래에서 그 자녀 지갑에
        //     환급까지 넣을 수 있다. 남의 지갑 잔액을 임의로 올리는 것이라 자금 조작에 해당한다.
        //   ※ QR 토큰은 [VULN-WALLET-13] 때문에 "qr_{자녀id}_{발급시각ms}" 로 예측 가능하다.
        //     즉 이 결함은 토큰을 몰라도 계산해 낼 수 있어 공격 난이도가 크게 낮아진다.
        //   방어: memberRepository.findById(qr.getChildId()) 후 child.getParent().getId()==parentId 확인.

        // [VULN-WALLET-10] 이미 취소된 결제인지 확인하지 않는다(재전송·replay 방어 부재).
        //   아래 markCancelled 앞에 "이 결제가 아직 PAID 인가?"를 보는 검사가 없다. 그래서 이미
        //   CANCELLED 인 결제에 취소를 또 걸어도 그대로 처리되고, 환급이 한 번 더 들어간다.
        //   → 같은 qrToken 으로 취소를 N번 부르면 결제액의 N배가 자녀 지갑에 쌓인다. 원래 결제한
        //     금액보다 많은 돈이 생기므로 사실상 돈을 만들어 내는 것이다(자금 창출).
        //   방어: if (qr.getStatus() != QrStatus.PAID) throw ...; + 동시 요청 차단용 @Version 락.

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
