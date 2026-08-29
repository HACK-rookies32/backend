package com.example.kidhackbackend.adminfinance;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import com.example.kidhackbackend.users.qrpay.entity.QrPayment;
import com.example.kidhackbackend.users.qrpay.repository.AdminPaymentProjection;
import com.example.kidhackbackend.users.qrpay.repository.QrPaymentRepository;
import com.example.kidhackbackend.users.wallet.entity.TxType;
import com.example.kidhackbackend.users.wallet.entity.Wallet;
import com.example.kidhackbackend.users.wallet.entity.WalletTransaction;
import com.example.kidhackbackend.users.wallet.repository.WalletRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletTransactionRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * 관리자 매장결제 목록 슬라이스 테스트 (3분할 2/3).
 *
 * <p>핵심 두 가지:
 * <ul>
 *   <li>거래 원장에 없는 <b>매장명</b>이 실제로 내려오는가 (이 API 의 존재 이유)</li>
 *   <li>같은 QR 로 중복 결제된 경우({@code [VULN-WALLET-16]}) <b>결제 행이 복제되지 않는가</b></li>
 * </ul>
 */
@DataJpaTest
class AdminPaymentListTest {

    @Autowired QrPaymentRepository qrPaymentRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired WalletRepository walletRepository;
    @Autowired WalletTransactionRepository txRepository;

    private Member member(String loginId, String username, Role role, Member parent) {
        Member m = new Member();
        m.setLoginId(loginId);
        m.setUsername(username);
        m.setNickname(loginId);
        m.setPassword("pw");
        m.setPhoneNumber("01000000000");
        m.setBirthDate(LocalDate.of(2013, 1, 1));
        m.setRole(role);
        m.setParent(parent);
        return memberRepository.save(m);
    }

    /** 결제 완료된 QR 1건을 만든다. */
    private QrPayment paidQr(Long childId, String token, long amount, String store) {
        QrPayment qr = new QrPayment(token, childId, LocalDateTime.now().plusMinutes(1));
        qr.markPaid(amount, store, 1L, LocalDateTime.now());
        return qrPaymentRepository.save(qr);
    }

    private Page<AdminPaymentProjection> search(Long childId, String status, String merchant, String keyword) {
        return qrPaymentRepository.adminSearchPayments(childId, status, merchant, keyword, PageRequest.of(0, 20));
    }

    @Test
    void 매장명과_자녀_보호자_이름이_함께_조회된다() {
        Member parent = member("pp1", "김보호", Role.PARENT, null);
        Member child = member("cc1", "김자녀", Role.CHILD, parent);
        paidQr(child.getId(), "qr_a1", 3000, "GS25 Gangnam");

        Page<AdminPaymentProjection> page = search(null, null, null, null);

        assertThat(page.getContent()).hasSize(1);
        AdminPaymentProjection row = page.getContent().get(0);
        assertThat(row.getStoreName()).isEqualTo("GS25 Gangnam");   // 원장엔 없는 값
        assertThat(row.getChildName()).isEqualTo("김자녀");
        assertThat(row.getParentName()).isEqualTo("김보호");
        assertThat(row.getParentId()).isEqualTo(parent.getId());
        assertThat(row.getAmount()).isEqualTo(3000);
        assertThat(row.getStatus()).isEqualTo("PAID");
    }

    @Test
    void 결제후_잔액이_원장에서_채워진다() {
        Member child = member("cc2", "자녀B", Role.CHILD, null);
        Wallet w = walletRepository.save(new Wallet(child.getId()));
        paidQr(child.getId(), "qr_b1", 3000, "CU");
        txRepository.save(new WalletTransaction(w.getId(), TxType.PAYMENT, 3000, 22000, "qr_b1"));

        Page<AdminPaymentProjection> page = search(null, null, null, null);

        assertThat(page.getContent().get(0).getBalanceAfter()).isEqualTo(22000);
    }

    @Test
    void 중복결제된_QR도_결제행이_복제되지_않는다() {
        // [VULN-WALLET-16] 같은 QR 재사용 → PAYMENT 원장이 2건. 조인하면 행이 2개로 늘어난다.
        Member child = member("cc3", "자녀C", Role.CHILD, null);
        Wallet w = walletRepository.save(new Wallet(child.getId()));
        paidQr(child.getId(), "qr_c1", 3000, "Emart24");
        txRepository.save(new WalletTransaction(w.getId(), TxType.PAYMENT, 3000, 7000, "qr_c1"));
        txRepository.save(new WalletTransaction(w.getId(), TxType.PAYMENT, 3000, 4000, "qr_c1"));

        Page<AdminPaymentProjection> page = search(null, null, null, null);

        assertThat(page.getContent()).hasSize(1);                       // 행 복제 없음
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getBalanceAfter()).isEqualTo(4000);  // 최근 1건
    }

    @Test
    void 결제되지_않은_QR은_제외된다() {
        Member child = member("cc4", "자녀D", Role.CHILD, null);
        // 발급만 하고 결제 안 함(paidAt null)
        qrPaymentRepository.save(new QrPayment("qr_d1", child.getId(), LocalDateTime.now().plusMinutes(1)));
        paidQr(child.getId(), "qr_d2", 1000, "CU");

        Page<AdminPaymentProjection> page = search(null, null, null, null);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getStoreName()).isEqualTo("CU");
    }

    @Test
    void merchant_필터가_동작한다() {
        Member child = member("cc5", "자녀E", Role.CHILD, null);
        paidQr(child.getId(), "qr_e1", 1000, "GS25 Gangnam");
        paidQr(child.getId(), "qr_e2", 2000, "CU Yeoksam");

        assertThat(search(null, null, "GS25", null).getContent()).hasSize(1);
        assertThat(search(null, null, "CU", null).getContent()).hasSize(1);
        assertThat(search(null, null, "없는매장", null).getContent()).isEmpty();
    }

    @Test
    void childId와_keyword_필터가_동작한다() {
        Member parent = member("pp6", "박보호", Role.PARENT, null);
        Member a = member("cc6", "박자녀", Role.CHILD, parent);
        Member b = member("cc7", "최자녀", Role.CHILD, null);
        paidQr(a.getId(), "qr_f1", 1000, "CU");
        paidQr(b.getId(), "qr_f2", 2000, "CU");

        assertThat(search(a.getId(), null, null, null).getContent()).hasSize(1);
        assertThat(search(null, null, null, "박자녀").getContent()).hasSize(1);
        // 보호자 실명으로도 검색된다
        assertThat(search(null, null, null, "박보호").getContent()).hasSize(1);
    }

    @Test
    void 페이징_메타가_유지된다() {
        Member child = member("cc8", "자녀F", Role.CHILD, null);
        for (int i = 0; i < 3; i++) {
            paidQr(child.getId(), "qr_g" + i, 1000, "CU");
        }

        Page<AdminPaymentProjection> first =
                qrPaymentRepository.adminSearchPayments(null, null, null, null, PageRequest.of(0, 2));

        assertThat(first.getContent()).hasSize(2);
        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.getTotalPages()).isEqualTo(2);
    }
}
