package com.example.kidhackbackend.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.kidhackbackend.admin.member.dto.AdminCustomerUpdateRequest;
import com.example.kidhackbackend.admin.member.service.AdminCustomerService;
import com.example.kidhackbackend.global.config.JpaAuditingConfig;
import com.example.kidhackbackend.global.exception.DuplicateResourceException;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * 관리자 회원 수정에서 이메일이 겹칠 때.
 *
 * <p>{@code member.email} 은 unique 라, 남이 쓰는 주소를 그대로 넣으면 flush 시점에 제약 위반이
 * 나 <b>500</b> 으로 떨어졌다. 회원용 {@code PATCH /api/users/me} 는 같은 상황에서 409 를 주고
 * 있어 두 경로의 응답이 갈렸다.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class AdminCustomerEmailDuplicateTest {

    @Autowired MemberRepository memberRepository;

    private AdminCustomerService service;

    @BeforeEach
    void setUp() {
        // 기기 이력·지갑 조회는 이 테스트와 무관한 협력자다.
        service = new AdminCustomerService(
                memberRepository,
                Mockito.mock(com.example.kidhackbackend.users.device.repository.DeviceEventRepository.class),
                Mockito.mock(com.example.kidhackbackend.admin.finance.service.AdminFinanceQueryService.class));
    }

    private Member member(String loginId) {
        Member m = new Member();
        m.setLoginId(loginId);
        m.setPassword("pw");
        m.setUsername("사용자");
        m.setNickname(loginId);
        m.setEmail(loginId + "@test.com");
        m.setRole(Role.PARENT);
        m.setBirthDate(LocalDate.of(1990, 1, 1));
        m.setPhoneNumber("010-0000-0000");
        return memberRepository.save(m);
    }

    @Test
    void 남이_쓰는_이메일로_바꾸면_409() {
        Member a = member("aaa");
        Member b = member("bbb");

        AdminCustomerUpdateRequest req =
                new AdminCustomerUpdateRequest(null, b.getEmail(), null, null);

        assertThatThrownBy(() -> service.updateCustomer(a.getId(), req))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void 자기_이메일_그대로면_통과한다() {
        Member a = member("ccc");

        AdminCustomerUpdateRequest req =
                new AdminCustomerUpdateRequest("새닉네임", a.getEmail(), null, null);

        assertThatCode(() -> service.updateCustomer(a.getId(), req)).doesNotThrowAnyException();
        assertThat(memberRepository.findById(a.getId()).orElseThrow().getNickname())
                .isEqualTo("새닉네임");
    }

    @Test
    void 아무도_안_쓰는_이메일로는_바꿀_수_있다() {
        Member a = member("ddd");

        AdminCustomerUpdateRequest req =
                new AdminCustomerUpdateRequest(null, "fresh@test.com", null, null);

        assertThatCode(() -> service.updateCustomer(a.getId(), req)).doesNotThrowAnyException();
        assertThat(memberRepository.findById(a.getId()).orElseThrow().getEmail())
                .isEqualTo("fresh@test.com");
    }
}
