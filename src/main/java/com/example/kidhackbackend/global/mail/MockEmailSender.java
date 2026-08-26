package com.example.kidhackbackend.global.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MockEmailSender implements EmailPort {
    @Override
    public void sendVerificationCode(String to, String code) {
        log.info("[MOCK EMAIL] to={}, code={}", to, code);
    }
}