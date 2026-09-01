package com.example.kidhackbackend.adminfinance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.kidhackbackend.admin.audit.entity.AdminActivityType;
import com.example.kidhackbackend.admin.audit.entity.AdminAuditLog;
import com.example.kidhackbackend.admin.audit.entity.AuditTargetType;
import com.example.kidhackbackend.admin.audit.repository.AdminAuditLogRepository;
import com.example.kidhackbackend.admin.audit.service.AdminAuditLogService;
import com.example.kidhackbackend.admin.finance.dto.AdminActionResponse;
import com.example.kidhackbackend.admin.finance.service.AdminFinanceCommandService;
import com.example.kidhackbackend.global.exception.DuplicateResourceException;
import com.example.kidhackbackend.global.exception.ResourceNotFoundException;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import com.example.kidhackbackend.users.qrpay.entity.QrPayment;
import com.example.kidhackbackend.users.qrpay.entity.QrStatus;
import com.example.kidhackbackend.users.qrpay.repository.QrPaymentRepository;
import com.example.kidhackbackend.users.wallet.dto.RefundResponse;
import com.example.kidhackbackend.users.wallet.entity.ChargeOrder;
import com.example.kidhackbackend.users.wallet.entity.ChargeStatus;
import com.example.kidhackbackend.users.wallet.entity.Wallet;
import com.example.kidhackbackend.users.wallet.repository.ChargeOrderRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 관리자 자금 변경(No.48·50) 슬라이스 테스트 — 강제환불/잔액조정 정상 + 취약점(사유·한도 미검증, 이중 강제환불).
 * (VULN-ADMIN-1 권한경계는 시큐리티/컨트롤러 계층 → 라이브에서 검증)
 */
@DataJpaTest
// @DataJpaTest 는 슬라이스라 컴포넌트 스캔을 하지 않는다. AdminFinanceCommandService 가
// 생성자로 받는 AdminAuditLogService 까지 직접 넣어 줘야 컨텍스트가 뜬다.
// 빠지면 NoSuchBeanDefinitionException('AdminAuditLogService') 로 죽는다.
// (AdminAuditLogService 의 의존성은 JPA 리포지토리 둘뿐이라 슬라이스가 알아서 채운다)
@Import({AdminFinanceCommandService.class, AdminAuditLogService.class})
class AdminFinanceCommandServiceTest {

    private static final long CHILD_ID = 300L;
    private static final long USER_ID = 400L;

    @Autowired AdminFinanceCommandService service;
    @Autowired QrPaymentRepository qrRepository;
    @Autowired WalletRepository walletRepository;
    @Autowired ChargeOrderRepository chargeOrderRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired AdminAuditLogRepository adminAuditLogRepository;

    private void giveBalance(long ownerId, long balance) {
        Wallet w = new Wallet(ownerId);
        w.credit(balance);
        walletRepository.save(w);
    }

    private QrPayment paidQr(long childId, String token, long amount) {
        QrPayment q = new QrPayment(token, childId, LocalDateTime.now().plusSeconds(60));
        q.markPaid(amount, "GS25", 1L, LocalDateTime.now());
        return qrRepository.save(q);
    }

    @Test
    void 강제환불_자녀잔액_원복되고_CANCELLED된다() {
        giveBalance(CHILD_ID, 7000L);
        QrPayment qr = paidQr(CHILD_ID, "qr_a", 3000L);

        AdminActionResponse res = service.forceRefund(qr.getQrToken());

        assertThat(res.balance()).isEqualTo(10_000L);
        assertThat(res.transaction().type()).isEqualTo("REFUND");
        assertThat(qrRepository.findByQrToken("qr_a").orElseThrow().getStatus()).isEqualTo(QrStatus.CANCELLED);
    }

    @Test
    void 잔액조정_증액과_감액() {
        AdminActionResponse plus = service.adjust(USER_ID, 5000L, "이벤트 보상");
        assertThat(plus.balance()).isEqualTo(5000L);
        assertThat(plus.transaction().type()).isEqualTo("ADJUST");

        AdminActionResponse minus = service.adjust(USER_ID, -2000L, "오적립 회수");
        assertThat(minus.balance()).isEqualTo(3000L);
    }

    @Test
    void VULN_ADMIN_2_사유없이_거대금액_조정이_통과된다() {
        AdminActionResponse res = service.adjust(USER_ID, 999_999_999L, "");

        // 취약: reason 공백·한도 검증이 없어 임의 금액 조정(자금 창출).
        assertThat(res.balance()).isEqualTo(999_999_999L);
    }

    @Test
    void VULN_ADMIN_3_이미환불된_결제를_재강제환불해_중복환급() {
        giveBalance(CHILD_ID, 7000L);
        QrPayment qr = paidQr(CHILD_ID, "qr_a", 3000L);

        service.forceRefund(qr.getQrToken());              // 1차 → 10000
        AdminActionResponse twice = service.forceRefund(qr.getQrToken()); // 2차(이중) → 13000

        // 취약: 상태 미검증으로 또 3000 환급.
        assertThat(twice.balance()).isEqualTo(13_000L);
    }

    // ------------------------------------------------------------------
    // #139 관리자 충전 취소 — 부모용과 달리 상태 검증이 들어간 '안전한' 경로다.
    // ------------------------------------------------------------------

    /** 감사로그는 SecurityContext 의 관리자 주체를 읽는다. 없으면 기록을 건너뛰므로 미리 심어 둔다. */
    private Member loginAsAdmin(String loginId) {
        Member admin = new Member();
        admin.setLoginId(loginId);
        admin.setUsername("관리자");
        admin.setNickname(loginId);
        admin.setPassword("pw");
        admin.setPhoneNumber("01000000000");
        admin.setBirthDate(LocalDate.of(1990, 1, 1));
        admin.setRole(Role.SUPER_ADMIN);
        Member saved = memberRepository.save(admin);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(saved, null, List.of()));
        return saved;
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private ChargeOrder paidCharge(long ownerId, String merchantUid, long amount) {
        ChargeOrder order = new ChargeOrder(merchantUid, ownerId, amount);
        order.markPaid("imp_test");
        return chargeOrderRepository.save(order);
    }

    @Test
    void 충전취소_잔액이_차감되고_주문이_REFUNDED된다() {
        giveBalance(USER_ID, 50_000L);
        paidCharge(USER_ID, "kid_ok", 50_000L);

        RefundResponse res = service.cancelCharge("kid_ok", "잘못 충전 민원 처리");

        assertThat(res.merchantUid()).isEqualTo("kid_ok");
        assertThat(res.refundedAmount()).isEqualTo(50_000L);
        assertThat(res.balance()).isZero();
        assertThat(res.transaction().type()).isEqualTo("REFUND");
        // 프론트가 kid_ 접두사로 '충전 환불' 을 식별하므로 refUid 는 merchantUid 여야 한다(#131 확정).
        assertThat(res.transaction().refUid()).isEqualTo("kid_ok");
        // 사유는 refUid 가 아니라 memo 로 간다(V15, 웹 요청 6번).
        assertThat(res.transaction().memo()).isEqualTo("잘못 충전 민원 처리");
        assertThat(chargeOrderRepository.findByMerchantUid("kid_ok").orElseThrow().getStatus())
                .isEqualTo(ChargeStatus.REFUNDED);
    }

    @Test
    void 잔액조정_사유가_memo에_남는다() {
        // 감사로그는 SecurityContext 의 관리자 주체를 읽는다.
        loginAsAdmin("admin02");
        // refUid 는 웹 호환을 위해 당분간 유지한다 — 회신에서 추후 제거 예정으로 안내했다.
        AdminActionResponse res = service.adjust(USER_ID, -2000L, "오적립 회수");

        assertThat(res.transaction().memo()).isEqualTo("오적립 회수");
        assertThat(res.transaction().refUid()).isEqualTo("오적립 회수");
        // #173 — 감사로그에도 사유가 남아야 한다(QA #10). 충전 취소는 남기는데 여기만 빠져 있었다.
        assertThat(adminAuditLogRepository.findAll())
                .filteredOn(l -> l.getActivityType() == AdminActivityType.WALLET_BALANCE_ADJUST)
                .singleElement()
                .satisfies(l -> assertThat(l.getDescription()).isEqualTo("오적립 회수"));
    }

    @Test
    void 충전취소_감사로그에_사유가_기록된다() {
        loginAsAdmin("admin01");
        giveBalance(USER_ID, 50_000L);
        paidCharge(USER_ID, "kid_audit", 50_000L);

        service.cancelCharge("kid_audit", "잘못 충전 민원 처리");

        List<AdminAuditLog> logs = adminAuditLogRepository.findAll();
        assertThat(logs).hasSize(1);
        AdminAuditLog logged = logs.get(0);
        assertThat(logged.getActivityType()).isEqualTo(AdminActivityType.CHARGE_CANCEL);
        assertThat(logged.getTargetType()).isEqualTo(AuditTargetType.WALLET);
        assertThat(logged.getAdminLoginId()).isEqualTo("admin01");
        // V8 에서 추가한 컬럼. 주문번호와 사유가 함께 남아야 이력만 보고 추적이 된다.
        assertThat(logged.getDescription()).isEqualTo("[kid_audit] 잘못 충전 민원 처리");
    }

    @Test
    void 충전취소_없는_주문이면_404() {
        assertThatThrownBy(() -> service.cancelCharge("kid_none", "사유"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void 충전취소_이미_환불된_주문은_409로_거부된다() {
        giveBalance(USER_ID, 100_000L);
        paidCharge(USER_ID, "kid_twice", 50_000L);

        service.cancelCharge("kid_twice", "1차 취소");

        // 부모용(VULN-WALLET-6)과 달리 상태를 검증하므로, 잔액이 남아 있어도 이중 환불이 막힌다.
        assertThatThrownBy(() -> service.cancelCharge("kid_twice", "2차 취소"))
                .isInstanceOf(DuplicateResourceException.class);
        assertThat(walletRepository.findByOwnerId(USER_ID).orElseThrow().getBalance())
                .isEqualTo(50_000L);   // 1회분만 빠졌다
    }

    @Test
    void 충전취소_결제되지_않은_READY_주문은_409로_거부된다() {
        giveBalance(USER_ID, 50_000L);
        chargeOrderRepository.save(new ChargeOrder("kid_ready", USER_ID, 50_000L));   // markPaid 안 함

        assertThatThrownBy(() -> service.cancelCharge("kid_ready", "사유"))
                .isInstanceOf(DuplicateResourceException.class);
        // 돈이 들어온 적 없는 주문으로 잔액을 빼가지 못한다.
        assertThat(walletRepository.findByOwnerId(USER_ID).orElseThrow().getBalance())
                .isEqualTo(50_000L);
    }

    @Test
    void 충전취소_이미_써서_잔액이_부족하면_409로_거부된다() {
        giveBalance(USER_ID, 10_000L);              // 5만원 충전 후 4만원을 이미 써버린 상태
        paidCharge(USER_ID, "kid_spent", 50_000L);

        assertThatThrownBy(() -> service.cancelCharge("kid_spent", "사유"))
                .isInstanceOf(DuplicateResourceException.class);
        assertThat(walletRepository.findByOwnerId(USER_ID).orElseThrow().getBalance())
                .isEqualTo(10_000L);   // 음수로 내려가지 않는다
    }
}
