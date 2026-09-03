package com.example.kidhackbackend.global.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SMS/ARS 발신 mock — 실제 통신사 발신 대신 로그만 남긴다.
 *
 * <p>{@link com.example.kidhackbackend.global.mail.MockEmailSender} 와 동형. 거래 OTP는 이 발신을
 * "한 척"만 하고, 실제 코드는 발급 API 응답에 그대로 노출된다([VULN-EFIN-1], VULN-AUTH-MAIL 동형).
 */
@Slf4j
@Component
public class MockSmsSender implements SmsPort {
    @Override
    public void sendOtp(String to, String code) {
        log.info("[MOCK SMS] to={}, code={}", to, code);
    }
}