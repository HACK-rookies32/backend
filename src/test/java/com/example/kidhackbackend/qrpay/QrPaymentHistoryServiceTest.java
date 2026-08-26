package com.example.kidhackbackend.qrpay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.qrpay.dto.QrPaymentHistoryPageResponse;
import com.example.kidhackbackend.users.qrpay.entity.QrPayment;
import com.example.kidhackbackend.users.qrpay.repository.QrPaymentRepository;
import com.example.kidhackbackend.users.qrpay.service.QrPaymentHistoryService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 자녀 결제 내역(No.45) 슬라이스 테스트 — CHILD 본인/PARENT 조회·페이징·결제이력 필터 + 취약점(페어링 IDOR) 재현.
 */
@DataJpaTest
@Import({QrPaymentHistoryService.class})
class QrPaymentHistoryServiceTest {

    private static final long CHILD_ID = 300L;
    private static final long OTHER_CHILD_ID = 301L;
    private static final long PARENT_ID = 500L;

    @Autowired QrPaymentHistoryService service;
    @Autowired QrPaymentRepository qrRepository;

    private Member member(Long id, Role role) {
        try {
            var c = Member.class.getDeclaredConstructor();
            c.setAccessible(true);
            Member m = c.newInstance();
            ReflectionTestUtils.setField(m, "id", id);
            ReflectionTestUtils.setField(m, "role", role);
            return m;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void paidQr(long childId, String token, long amount) {
        QrPayment q = new QrPayment(token, childId, LocalDateTime.now().plusSeconds(60));
        q.markPaid(amount, "GS25", 1L, LocalDateTime.now());
        qrRepository.save(q);
    }

    private void issuedQr(long childId, String token) {
        qrRepository.save(new QrPayment(token, childId, LocalDateTime.now().plusSeconds(60)));
    }

    @Test
    void 자녀본인_결제된것만_조회된다_미결제ISSUED는_제외() {
        paidQr(CHILD_ID, "qr_a", 3000L);
        paidQr(CHILD_ID, "qr_b", 5000L);
        issuedQr(CHILD_ID, "qr_c"); // 미결제 → 제외

        QrPaymentHistoryPageResponse res = service.getPayments(member(CHILD_ID, Role.CHILD), null, 0, 10);

        assertThat(res.totalElements()).isEqualTo(2);
        assertThat(res.content()).extracting("qrToken").containsExactlyInAnyOrder("qr_a", "qr_b");
    }

    @Test
    void 부모가_자기_childId로_자녀_결제내역을_조회한다() {
        paidQr(CHILD_ID, "qr_a", 3000L);

        QrPaymentHistoryPageResponse res = service.getPayments(member(PARENT_ID, Role.PARENT), CHILD_ID, 0, 10);

        assertThat(res.totalElements()).isEqualTo(1);
        assertThat(res.content().get(0).storeName()).isEqualTo("GS25");
    }

    @Test
    void 페이징_size1이면_totalPages가_2가된다() {
        paidQr(CHILD_ID, "qr_a", 3000L);
        paidQr(CHILD_ID, "qr_b", 5000L);

        QrPaymentHistoryPageResponse res = service.getPayments(member(CHILD_ID, Role.CHILD), null, 0, 1);

        assertThat(res.size()).isEqualTo(1);
        assertThat(res.content()).hasSize(1);
        assertThat(res.totalPages()).isEqualTo(2);
    }

    @Test
    void PARENT가_childId없이_요청하면_거부된다() {
        assertThatThrownBy(() -> service.getPayments(member(PARENT_ID, Role.PARENT), null, 0, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void VULN_WALLET_19_부모가_남의_자녀_결제내역을_조회할수있다() {
        paidQr(OTHER_CHILD_ID, "qr_x", 7000L); // 다른 집 자녀의 결제

        // PARENT_ID(500)는 OTHER_CHILD_ID(301)의 부모가 아님 — 그런데도 조회됨
        QrPaymentHistoryPageResponse res = service.getPayments(member(PARENT_ID, Role.PARENT), OTHER_CHILD_ID, 0, 10);

        assertThat(res.totalElements()).isEqualTo(1); // 취약: 페어링 미검증으로 남의 자녀 내역 노출
        assertThat(res.content().get(0).amount()).isEqualTo(7000L);
    }
}
