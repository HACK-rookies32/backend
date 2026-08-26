package com.example.kidhackbackend.users.qrpay.service;

import com.example.kidhackbackend.users.qrpay.dto.QrIssueResponse;
import com.example.kidhackbackend.users.qrpay.entity.QrPayment;
import com.example.kidhackbackend.users.qrpay.repository.QrPaymentRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 QR 발급 — QR결제 No.42. 자녀가 매장 결제용 1회성 QR 토큰을 발급한다.
 *
 * <p>발급: ISSUED 상태 + 60초 만료(expiresAt)로 저장. 금액·매장명은 승인(No.44) 시 확정.
 */
@Service
@RequiredArgsConstructor
public class QrPaymentService {

    private final QrPaymentRepository qrPaymentRepository;

    /** QR 만료(TTL) 초. application.yml 미수정 규칙 → 기본값 60초. */
    @Value("${kidhack.qr.ttl-seconds:60}")
    private long ttlSeconds;

    /** 결제 QR 발급: 자녀 소유로 1회성 토큰을 만들고 60초 만료로 저장한다. */
    @Transactional
    public QrIssueResponse issue(Long childId) {
        // [VULN-WALLET-13] 예측 가능한 QR 토큰(취약한 난수 — CWE-330).
        //   요구사항은 '예측 불가 난수'지만, 토큰을 childId + 발급시각(epoch millis)이라는 추측 가능한 값으로 만든다.
        //   → 공격자가 대상 자녀 id와 대략적 발급 시각만 알면 유효 QR 토큰을 재구성/열거해, No.44에서 그 자녀
        //     지갑으로 결제를 유도할 수 있다(1회성·무작위성이 사실상 무력화).
        //   방어: SecureRandom/UUID 등 예측 불가능한 난수로 생성(예: "qr_" + UUID.randomUUID()...).
        String qrToken = "qr_" + childId + "_" + System.currentTimeMillis();

        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(ttlSeconds);
        QrPayment qr = qrPaymentRepository.save(new QrPayment(qrToken, childId, expiresAt));
        return QrIssueResponse.from(qr);
    }
}
