package com.example.kidhackbackend.qrpay;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.kidhackbackend.users.qrpay.dto.QrIssueResponse;
import com.example.kidhackbackend.users.qrpay.entity.QrPayment;
import com.example.kidhackbackend.users.qrpay.repository.QrPaymentRepository;
import com.example.kidhackbackend.users.qrpay.service.QrPaymentService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 결제 QR 발급(No.42) 슬라이스 테스트 — 정상 발급/만료시각 + 취약점(예측가능 토큰) 재현.
 */
@DataJpaTest
@Import({QrPaymentService.class})
class QrPaymentServiceTest {

    private static final long CHILD_ID = 200L;

    @Autowired QrPaymentService service;
    @Autowired QrPaymentRepository repository;

    @Test
    void QR발급_ISSUED_상태와_만료시각으로_생성된다() {
        ReflectionTestUtils.setField(service, "ttlSeconds", 60L);
        LocalDateTime before = LocalDateTime.now();

        QrIssueResponse res = service.issue(CHILD_ID);

        assertThat(res.status()).isEqualTo("ISSUED");
        assertThat(res.qrToken()).startsWith("qr_");
        // 만료시각이 발급 시점 + 약 60초 근방
        assertThat(res.expiresAt()).isAfter(before.plusSeconds(55));
        QrPayment saved = repository.findByQrToken(res.qrToken()).orElseThrow();
        assertThat(saved.getChildId()).isEqualTo(CHILD_ID);
        assertThat(saved.isExpired(before)).isFalse();
    }

    @Test
    void VULN_WALLET_13_QR토큰이_childId를_포함해_예측_가능하다() {
        QrIssueResponse res = service.issue(CHILD_ID);

        // 취약: 토큰이 "qr_{childId}_{millis}" 구조라 대상 childId와 발급 시각만 알면 재구성/열거 가능.
        assertThat(res.qrToken()).startsWith("qr_" + CHILD_ID + "_");
    }
}
