package com.example.kidhackbackend.global.sms;

/**
 * SMS/ARS 발신 포트 — 전자금융 거래 OTP 전송용.
 *
 * <p>{@link com.example.kidhackbackend.global.mail.EmailPort} 와 동형 구조다. 실제 통신사 발신은
 * 이 프로젝트 범위 밖이라, mock 구현({@link MockSmsSender})이 "발신한 척"만 한다.
 */
public interface SmsPort {
    /** {@code to}(전화번호)로 인증번호 {@code code}를 발송한다(mock: 로그만 남김). */
    void sendOtp(String to, String code);
}