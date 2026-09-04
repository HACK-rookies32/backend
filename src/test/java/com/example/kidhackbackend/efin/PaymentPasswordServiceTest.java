package com.example.kidhackbackend.efin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.kidhackbackend.users.efin.entity.PaymentPasswordHistory;
import com.example.kidhackbackend.users.efin.repository.PaymentPasswordHistoryRepository;
import com.example.kidhackbackend.users.efin.service.PaymentPasswordService;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * 결제 비밀번호 설정(카드 발급 흐름)·변경·재사용 방지 + 변경 시 로그인 본인확인 + [VULN-EFIN-6] 이력 평문 재현.
 */
@DataJpaTest
@Import(PaymentPasswordService.class)
class PaymentPasswordServiceTest {

    private static final String LOGIN_PW = "1234";

    @Autowired PaymentPasswordService service;
    @Autowired PaymentPasswordHistoryRepository historyRepository;
    @Autowired MemberRepository memberRepository;

    private Long memberId;

    @BeforeEach
    void setUp() {
        Member m = new Member();
        m.setLoginId("paypw-user");
        m.setUsername("김부모");
        m.setPassword(LOGIN_PW); // [VULN-1] 평문 저장
        m.setPhoneNumber("010-1234-5678");
        m.setBirthDate(LocalDate.of(1985, 5, 10));
        m.setRole(Role.PARENT);
        memberId = memberRepository.save(m).getId();
    }

    @Test
    void 설정한_비밀번호로_본인확인이_된다() {
        service.setup(memberId, "1357");
        assertThat(service.matches(memberId, "1357")).isTrue();
        assertThat(service.matches(memberId, "0000")).isFalse();
        assertThat(service.exists(memberId)).isTrue();
    }

    @Test
    void 이미_설정되어_있으면_setup_거부() {
        service.setup(memberId, "1357");
        assertThatThrownBy(() -> service.setup(memberId, "2468"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 변경시_로그인_비밀번호가_틀리면_거부() {
        service.setup(memberId, "1357");
        assertThatThrownBy(() -> service.change(memberId, "1357", "2468", "9999"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 변경시_기존_결제비밀번호가_틀리면_거부() {
        service.setup(memberId, "1357");
        assertThatThrownBy(() -> service.change(memberId, "9999", "2468", LOGIN_PW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 과거에_쓴_비밀번호로는_바꿀_수_없다() {
        service.setup(memberId, "1357");
        service.change(memberId, "1357", "2468", LOGIN_PW);
        assertThatThrownBy(() -> service.change(memberId, "2468", "1357", LOGIN_PW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void VULN_EFIN_6_이력이_평문으로_저장된다() {
        service.setup(memberId, "1357");
        service.change(memberId, "1357", "2468", LOGIN_PW);

        List<PaymentPasswordHistory> history = historyRepository.findByMemberId(memberId);
        assertThat(history).extracting(PaymentPasswordHistory::getPassword)
                .containsExactlyInAnyOrder("1357", "2468");
    }
}
