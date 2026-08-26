package com.example.kidhackbackend.users.qrpay.service;

import com.example.kidhackbackend.global.exception.ResourceNotFoundException;
import com.example.kidhackbackend.users.qrpay.dto.QrIssueResponse;
import com.example.kidhackbackend.users.qrpay.dto.QrStatusResponse;
import com.example.kidhackbackend.users.qrpay.entity.QrPayment;
import com.example.kidhackbackend.users.qrpay.entity.QrStatus;
import com.example.kidhackbackend.users.qrpay.repository.QrPaymentRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 QR 발급/조회 — QR결제 No.42(발급)·No.43(상태 폴링). 자녀가 매장 결제용 1회성 QR을 다룬다.
 *
 * <p>발급: ISSUED 상태 + 60초 만료(expiresAt)로 저장. 금액·매장명은 승인(No.44) 시 확정.
 * 폴링: qrToken으로 상태 조회(만료는 조회 시 계산해 반영, 상태 강제 전이는 하지 않음).
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

    /**
     * 결제 QR 상태 조회(폴링) — No.43. 만료는 조회 시 계산해 반영한다(상태 강제 전이는 하지 않음).
     *
     * @param requesterId 조회를 요청한(로그인한) 회원 id
     * @param qrToken     조회할 QR 토큰
     */
    @Transactional(readOnly = true)
    public QrStatusResponse getStatus(Long requesterId, String qrToken) {
        QrPayment qr = qrPaymentRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new ResourceNotFoundException("QR을 찾을 수 없습니다."));

        // [VULN-WALLET-14] 폴링 소유권 미검증(IDOR / BOLA).
        //   QR 발급자(qr.getChildId())와 요청자(requesterId)가 같은지 확인하지 않는다(발급 본인만 조회 위반).
        //   → 아무 자녀나 qrToken만 알면 남의 QR 상태를 조회할 수 있고, VULN-13(예측가능 토큰)과 결합하면
        //     타 자녀의 QR 토큰을 열거·추적할 수 있다(결제 상태 정보 노출).
        //   방어: if (!qr.getChildId().equals(requesterId)) throw new AccessDenied...;

        // 만료 반영: 저장 상태가 ISSUED인데 만료 시각을 지났으면 응답 상태를 EXPIRED로 계산(DB는 그대로).
        QrStatus effective = qr.getStatus();
        if (effective == QrStatus.ISSUED && qr.isExpired(LocalDateTime.now())) {
            effective = QrStatus.EXPIRED;
        }
        return new QrStatusResponse(qr.getQrToken(), effective.name(), qr.getExpiresAt());
    }
}
