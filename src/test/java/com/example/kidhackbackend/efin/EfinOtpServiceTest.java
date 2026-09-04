package com.example.kidhackbackend.efin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.kidhackbackend.global.sms.SmsPort;
import com.example.kidhackbackend.users.efin.dto.OtpIssueResponse;
import com.example.kidhackbackend.users.efin.service.EfinOtpService;
import com.example.kidhackbackend.users.member.entity.Member;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/** 거래 OTP 발급·검증·소비 + [VULN-EFIN-1] 응답 노출 · [VULN-EFIN-1b] 시도 무제한 재현. */
@DataJpaTest
@Import({EfinOtpService.class, EfinOtpServiceTest.NoOpSmsConfig.class})
class EfinOtpServiceTest {

    @TestConfiguration
    static class NoOpSmsConfig {
        @Bean
        SmsPort smsPort() {
            return (to, code) -> { /* no-op mock */ };
        }
    }

    @Autowired EfinOtpService service;

    private Member member() {
        Member m = new Member();
        m.setId(1L);
        m.setPhoneNumber("010-1234-5678");
        return m;
    }

    @Test
    void VULN_EFIN_1_발급코드가_응답에_노출된다() {
        OtpIssueResponse res = service.issue(member());
        assertThat(res.code()).hasSize(6);          // 응답에 코드가 그대로 담긴다
        assertThat(res.phone()).isEqualTo("010-****-5678");
    }

    @Test
    void VULN_EFIN_1b_실패를_반복해도_잠기지_않는다() {
        Member m = member();
        String code = service.issue(m).code();

        // 잘못된 코드를 여러 번 시도해도 잠기지 않는다 → 브루트포스 가능.
        for (int i = 0; i < 20; i++) {
            assertThat(service.verify(m, "000000")).isFalse();
        }
        // 그 뒤에도 올바른 코드는 여전히 통과.
        assertThat(service.verify(m, code)).isTrue();
    }

    @Test
    void 소비된_OTP는_재사용되지_않는다() {
        Member m = member();
        String code = service.issue(m).code();

        service.consume(m.getId(), m.getPhoneNumber(), code); // 1회 소비
        assertThatThrownBy(() -> service.consume(m.getId(), m.getPhoneNumber(), code))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void OTP가_비어있으면_소비_거부() {
        Member m = member();
        service.issue(m);
        assertThatThrownBy(() -> service.consume(m.getId(), m.getPhoneNumber(), " "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}