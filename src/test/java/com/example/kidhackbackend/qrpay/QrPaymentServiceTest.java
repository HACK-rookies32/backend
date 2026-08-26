package com.example.kidhackbackend.qrpay;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.kidhackbackend.users.qrpay.dto.QrIssueResponse;
import com.example.kidhackbackend.users.qrpay.dto.QrStatusResponse;
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
 * 결제 QR 발급/조회(No.42·43) 슬라이스 테스트 — 정상 발급·조회/만료계산 + 취약점(예측토큰·폴링 IDOR) 재현.
 */
@DataJpaTest
@Import({QrPaymentService.class})
class QrPaymentServiceTest {

    private static final long CHILD_ID = 200L;
    private static final long OTHER_CHILD_ID = 201L;

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

    @Test
    void 상태조회_ISSUED_QR을_정상_반환한다() {
        String token = service.issue(CHILD_ID).qrToken();

        QrStatusResponse res = service.getStatus(CHILD_ID, token);

        assertThat(res.qrToken()).isEqualTo(token);
        assertThat(res.status()).isEqualTo("ISSUED");
    }

    @Test
    void 상태조회_만료된_QR은_EXPIRED로_계산된다() {
        // 만료 시각이 과거인 QR을 직접 저장(ISSUED)
        QrPayment expired = repository.save(
                new QrPayment("qr_expired_1", CHILD_ID, LocalDateTime.now().minusSeconds(1)));

        QrStatusResponse res = service.getStatus(CHILD_ID, expired.getQrToken());

        assertThat(res.status()).isEqualTo("EXPIRED"); // DB는 ISSUED이나 조회 시 만료 반영
        assertThat(repository.findByQrToken("qr_expired_1").orElseThrow().getStatus().name())
                .isEqualTo("ISSUED"); // 강제 전이는 하지 않음
    }

    @Test
    void VULN_WALLET_14_소유권검증없이_남의_QR상태를_조회할수있다() {
        String victimToken = service.issue(CHILD_ID).qrToken(); // 피해자(CHILD_ID) QR

        // 공격자(OTHER_CHILD_ID)가 qrToken만으로 피해자 QR 상태 조회
        QrStatusResponse res = service.getStatus(OTHER_CHILD_ID, victimToken);

        // 취약: 발급자 대조가 없어 남의 QR 상태가 그대로 노출된다.
        assertThat(res.qrToken()).isEqualTo(victimToken);
        assertThat(res.status()).isEqualTo("ISSUED");
    }
}
