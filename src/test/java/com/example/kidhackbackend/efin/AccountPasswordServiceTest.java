package com.example.kidhackbackend.efin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.kidhackbackend.users.efin.entity.AccountPasswordHistory;
import com.example.kidhackbackend.users.efin.repository.AccountPasswordHistoryRepository;
import com.example.kidhackbackend.users.efin.service.AccountPasswordService;
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
 * 계좌비밀번호 설정·변경·재사용 방지 + 로그인 비밀번호 본인확인 + [VULN-EFIN-6] 이력 평문 저장 재현.
 */
@DataJpaTest
@Import(AccountPasswordService.class)
class AccountPasswordServiceTest {

    private static final String LOGIN_PW = "1234";

    @Autowired AccountPasswordService service;
    @Autowired AccountPasswordHistoryRepository historyRepository;
    @Autowired MemberRepository memberRepository;

    private Long memberId;

    @BeforeEach
    void setUp() {
        Member m = new Member();
        m.setLoginId("acctpw-user");
        m.setUsername("김부모");
        m.setPassword(LOGIN_PW); // [VULN-1] 평문 저장
        m.setPhoneNumber("010-1234-5678");
        m.setBirthDate(LocalDate.of(1985, 5, 10));
        m.setRole(Role.PARENT);
        memberId = memberRepository.save(m).getId();
    }

    @Test
    void 설정한_비밀번호로_본인확인이_된다() {
        service.set(memberId, "135790", LOGIN_PW);
        assertThat(service.matches(memberId, "135790")).isTrue();
        assertThat(service.matches(memberId, "000000")).isFalse();
    }

    @Test
    void 설정시_로그인_비밀번호가_틀리면_거부() {
        assertThatThrownBy(() -> service.set(memberId, "135790", "9999"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 변경시_기존_계좌비밀번호가_틀리면_거부() {
        service.set(memberId, "135790", LOGIN_PW);
        assertThatThrownBy(() -> service.change(memberId, "999999", "246802", LOGIN_PW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 과거에_쓴_비밀번호로는_바꿀_수_없다() {
        service.set(memberId, "135790", LOGIN_PW);
        service.change(memberId, "135790", "246802", LOGIN_PW);
        assertThatThrownBy(() -> service.change(memberId, "246802", "135790", LOGIN_PW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void VULN_EFIN_6_이력이_평문으로_저장된다() {
        service.set(memberId, "135790", LOGIN_PW);
        service.change(memberId, "135790", "246802", LOGIN_PW);

        List<AccountPasswordHistory> history = historyRepository.findByMemberId(memberId);
        assertThat(history).extracting(AccountPasswordHistory::getPassword)
                .containsExactlyInAnyOrder("135790", "246802");
    }
}
