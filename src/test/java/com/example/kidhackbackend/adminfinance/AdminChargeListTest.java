package com.example.kidhackbackend.adminfinance;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.kidhackbackend.admin.finance.dto.AdminChargePageResponse;
import com.example.kidhackbackend.admin.finance.dto.AdminChargeRow;
import com.example.kidhackbackend.global.config.JpaAuditingConfig;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import com.example.kidhackbackend.users.wallet.entity.ChargeOrder;
import com.example.kidhackbackend.users.wallet.entity.ChargeStatus;
import com.example.kidhackbackend.users.wallet.repository.AdminChargeProjection;
import com.example.kidhackbackend.users.wallet.repository.ChargeOrderRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * 관리자 충전 목록 슬라이스 테스트 (3분할 3/3).
 *
 * <p>핵심 세 가지:
 * <ul>
 *   <li>거래 원장에 없는 <b>주문 상태·PG 거래 ID</b> 가 내려오는가 (이 API 의 존재 이유)</li>
 *   <li>원장에 아예 안 나타나는 <b>READY·EXPIRED 주문도 목록에 보이는가</b></li>
 *   <li><b>환불 사유</b>가 감사로그가 아니라 주문 컬럼(V11)에서 나오는가 —
 *       부모 환불은 사유가 없어 관리자 취소와 구분되는가</li>
 * </ul>
 */
@DataJpaTest
// createdAt(@CreatedDate)을 검증하므로 auditing 을 켜야 한다 — 슬라이스는 이 설정을 자동으로
// 끌어오지 않아 값이 null 로 남는다. 다른 자금 테스트들과 같은 방식으로 명시 import 한다.
@Import(JpaAuditingConfig.class)
class AdminChargeListTest {

    @Autowired ChargeOrderRepository chargeOrderRepository;
    @Autowired MemberRepository memberRepository;

    private Member member(String loginId, String username, String email) {
        Member m = new Member();
        m.setLoginId(loginId);
        m.setUsername(username);
        m.setNickname(loginId);
        m.setPassword("pw");
        m.setEmail(email);
        m.setPhoneNumber("01000000000");
        m.setBirthDate(LocalDate.of(1990, 1, 1));
        m.setRole(Role.PARENT);
        return memberRepository.save(m);
    }

    private ChargeOrder order(long ownerId, String uid, long amount) {
        return chargeOrderRepository.save(new ChargeOrder(uid, ownerId, amount));
    }

    private Page<AdminChargeProjection> search(Long userId, String status, String keyword) {
        return chargeOrderRepository.adminSearchCharges(userId, status, keyword, PageRequest.of(0, 20));
    }

    @Test
    void 충전목록_주문상태와_PG거래ID가_내려온다() {
        Member p = member("cl1", "김보호", "cl1@test.com");
        ChargeOrder o = order(p.getId(), "kid_paid", 50_000L);
        o.markPaid("imp_1a2b3c");
        chargeOrderRepository.save(o);

        AdminChargeRow row = AdminChargePageResponse.from(search(null, null, null)).content().get(0);

        // 충전취소 API 의 경로 파라미터. 목록에서 바로 취소를 걸려면 반드시 있어야 한다.
        assertThat(row.merchantUid()).isEqualTo("kid_paid");
        assertThat(row.id()).isNotNull();
        assertThat(row.userId()).isEqualTo(p.getId());
        assertThat(row.userName()).isEqualTo("김보호");
        assertThat(row.userEmail()).isEqualTo("cl1@test.com");
        assertThat(row.amount()).isEqualTo(50_000L);
        // 원장에는 없는 두 필드 — 이 API 의 존재 이유다.
        assertThat(row.status()).isEqualTo("PAID");
        assertThat(row.pgTransactionId()).isEqualTo("imp_1a2b3c");
        // MockPG 라 수단을 저장하지 않아 고정값(3분할 확정 3번).
        assertThat(row.method()).isEqualTo("카드");
        assertThat(row.createdAt()).isNotNull();
    }

    @Test
    void 원장에_안_나타나는_READY_EXPIRED_주문도_목록에_보인다() {
        Member p = member("cl2", "김보호", "cl2@test.com");
        order(p.getId(), "kid_ready", 10_000L);                     // 결제 전 — 원장 없음
        ChargeOrder expired = order(p.getId(), "kid_expired", 20_000L);
        expired.markExpired();
        chargeOrderRepository.save(expired);

        AdminChargePageResponse res = AdminChargePageResponse.from(search(null, null, null));

        assertThat(res.totalElements()).isEqualTo(2);
        assertThat(res.content()).extracting(AdminChargeRow::status)
                .containsExactlyInAnyOrder("READY", "EXPIRED");
        // 아직 승인 전이라 PG 거래 ID 가 없다.
        assertThat(res.content()).allSatisfy(r -> assertThat(r.pgTransactionId()).isNull());
    }

    @Test
    void 관리자취소만_사유가_있다_환불시각은_둘다_남는다() {
        Member p = member("cl3", "김보호", "cl3@test.com");

        ChargeOrder byAdmin = order(p.getId(), "kid_admin", 50_000L);
        byAdmin.markPaid("imp_admin");
        byAdmin.markRefunded("잘못 충전 민원 처리");          // 관리자 충전취소 경로
        chargeOrderRepository.save(byAdmin);

        ChargeOrder byParent = order(p.getId(), "kid_parent", 30_000L);
        byParent.markPaid("imp_parent");
        byParent.markRefunded();                              // 부모 환불 경로 — 사유를 받지 않는다
        chargeOrderRepository.save(byParent);

        var rows = AdminChargePageResponse.from(search(null, null, null)).content();
        AdminChargeRow admin = rows.stream().filter(r -> r.amount() == 50_000L).findFirst().orElseThrow();
        AdminChargeRow parent = rows.stream().filter(r -> r.amount() == 30_000L).findFirst().orElseThrow();

        // 둘 다 REFUNDED 이고 환불 시각도 둘 다 있다 — 구분 근거는 '사유' 뿐이다.
        // (refundedAt 을 모바일 ChargeHistoryResponse.refundedAt 과 같은 의미로 맞췄기 때문이다:
        //  거기서는 REFUNDED 주문이면 경로와 무관하게 값이 있다)
        assertThat(admin.status()).isEqualTo("REFUNDED");
        assertThat(parent.status()).isEqualTo("REFUNDED");
        assertThat(admin.refundedAt()).isNotNull();
        assertThat(parent.refundedAt()).isNotNull();
        assertThat(admin.refundReason()).isEqualTo("잘못 충전 민원 처리");
        assertThat(parent.refundReason()).isNull();
    }

    @Test
    void status_필터() {
        Member p = member("cl4", "김보호", "cl4@test.com");
        order(p.getId(), "kid_r", 10_000L);
        ChargeOrder paid = order(p.getId(), "kid_p", 20_000L);
        paid.markPaid("imp_x");
        chargeOrderRepository.save(paid);

        assertThat(search(null, "PAID", null).getTotalElements()).isEqualTo(1);
        assertThat(search(null, "READY", null).getTotalElements()).isEqualTo(1);
        // 철자 주의 — CANCELLED(L 2개). 해당 건이 없으면 0건.
        assertThat(search(null, ChargeStatus.CANCELLED.name(), null).getTotalElements()).isZero();
    }

    @Test
    void userId_필터로_남의_주문은_안_보인다() {
        Member a = member("cl5", "김보호", "cl5@test.com");
        Member b = member("cl6", "박보호", "cl6@test.com");
        order(a.getId(), "kid_a", 10_000L);
        order(b.getId(), "kid_b", 20_000L);

        Page<AdminChargeProjection> mine = search(a.getId(), null, null);

        assertThat(mine.getTotalElements()).isEqualTo(1);
        assertThat(mine.getContent().get(0).getUserId()).isEqualTo(a.getId());
    }

    @Test
    void keyword_는_실명과_이메일_모두_매칭된다() {
        Member p = member("cl7", "김보호", "boho7@test.com");
        member("cl8", "박다른", "other8@test.com");
        order(p.getId(), "kid_k", 10_000L);

        assertThat(search(null, null, "김보").getTotalElements()).isEqualTo(1);
        assertThat(search(null, null, "boho7").getTotalElements()).isEqualTo(1);
        assertThat(search(null, null, "없는키워드").getTotalElements()).isZero();
    }

    @Test
    void 회원이_삭제돼도_충전이력은_남고_이름은_null이_된다() {
        Member p = member("cl9", "김보호", "cl9@test.com");
        order(p.getId(), "kid_orphan", 10_000L);
        memberRepository.delete(p);
        memberRepository.flush();

        AdminChargePageResponse res = AdminChargePageResponse.from(search(null, null, null));

        // LEFT JOIN 이라 행이 사라지지 않는다 — 사라지면 장부에 구멍이 생긴다.
        assertThat(res.totalElements()).isEqualTo(1);
        AdminChargeRow row = res.content().get(0);
        assertThat(row.userName()).isNull();
        assertThat(row.userEmail()).isNull();
        assertThat(row.amount()).isEqualTo(10_000L);
    }

    @Test
    void 목록의_merchantUid로_충전취소_API를_바로_호출할_수_있다() {
        Member p = member("cl11", "김보호", "cl11@test.com");
        ChargeOrder o = order(p.getId(), "kid_link", 50_000L);
        o.markPaid("imp_link");
        chargeOrderRepository.save(o);

        AdminChargeRow row = AdminChargePageResponse.from(search(null, null, null)).content().get(0);

        // 프론트가 목록 행에서 꺼낸 값으로 POST /finance/charges/{merchantUid}/cancel 을 부른다.
        // 이 값이 없으면 거래내역의 refUid 를 따로 뒤져야 해서 탭 전용 API 의 취지가 무너진다.
        assertThat(row.merchantUid()).isNotNull().startsWith("kid_");
        assertThat(chargeOrderRepository.findByMerchantUid(row.merchantUid())).isPresent();
    }

    @Test
    void 페이징_봉투_형태() {
        Member p = member("cl10", "김보호", "cl10@test.com");
        for (int i = 0; i < 3; i++) {
            order(p.getId(), "kid_pg" + i, 1000L * (i + 1));
        }

        AdminChargePageResponse res = AdminChargePageResponse.from(
                chargeOrderRepository.adminSearchCharges(null, null, null, PageRequest.of(0, 2)));

        assertThat(res.page()).isZero();
        assertThat(res.size()).isEqualTo(2);
        assertThat(res.totalElements()).isEqualTo(3);
        assertThat(res.totalPages()).isEqualTo(2);
        assertThat(res.content()).hasSize(2);
    }
}
