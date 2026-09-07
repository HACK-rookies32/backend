package com.example.kidhackbackend.users.efin.service;

import com.example.kidhackbackend.global.mail.EmailVerification;
import com.example.kidhackbackend.global.mail.EmailVerification.Purpose;
import com.example.kidhackbackend.global.mail.EmailVerificationRepository;
import com.example.kidhackbackend.global.sms.SmsPort;
import com.example.kidhackbackend.users.efin.dto.OtpIssueResponse;
import com.example.kidhackbackend.users.member.entity.Member;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 거래 인증수단(OTP/ARS) — 2차 인증. 송금·회수 등 거래 승인 전에 전화번호로 OTP 를 발급·검증한다.
 *
 * <p>저장은 별도 테이블 없이 {@link EmailVerification}(Purpose=TX_OTP) 를 재사용한다 —
 * {@code email} 컬럼에 전화번호를 담는다. 발신은 {@link SmsPort} mock(로그만).
 *
 * <p>⚠️ [VULN-EFIN-1] 발급 코드를 응답에 노출한다({@link OtpIssueResponse}).
 * ⚠️ [VULN-EFIN-1b] 검증 실패 횟수 제한이 없다 — 6자리(10^6)를 무제한 시도하면 뚫린다(로그인 VULN-4 계열).
 */
@Service
@RequiredArgsConstructor
public class EfinOtpService {

    private static final long CODE_TTL_SECONDS = 180;

    private final EmailVerificationRepository verificationRepository;
    private final SmsPort smsPort;

    /** 거래 OTP 발급 → mock SMS 발신 + 응답에 코드 노출. */
    @Transactional
    public OtpIssueResponse issue(Member member) {
        String phone = member.getPhoneNumber();
        String code = generateCode();
        verificationRepository.save(new EmailVerification(
                member.getId(), phone, code, Purpose.TX_OTP,
                LocalDateTime.now().plusSeconds(CODE_TTL_SECONDS)));
        smsPort.sendOtp(phone, code);
        // [재점검 2026-09-07] 취약 확정 — 금취분평 001 [전자금융] 거래 인증수단 검증 / 주통기 IL(정보 누출).
        //   OTP 를 발신 채널과 같은 응답 body 로 돌려줘 2차 인증(소지 기반)이 무력화됨.
        return new OtpIssueResponse(mask(phone), code, CODE_TTL_SECONDS); // [VULN-EFIN-1] code 노출
    }

    /** 사전 확인(송금 전 UI 점검) — 소비하지 않는다. 시도 횟수 제한 없음([VULN-EFIN-1b]). */
    @Transactional(readOnly = true)
    public boolean verify(Member member, String code) {
        return verificationRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(member.getPhoneNumber(), Purpose.TX_OTP)
                .map(v -> v.isValid(code))
                .orElse(false);
    }

    /**
     * 거래 승인 시 OTP 소비(일회성). 유효하지 않으면 예외.
     *
     * <p>[VULN-EFIN-1b] 실패해도 잠기지 않으므로, 실패한 시도를 계속 반복해 브루트포스할 수 있다.
     * <p>[재점검 2026-09-07] 취약 확정 — 금취분평 032 인증 오류 횟수 제한기능 제공 / 주통기 AU(자동화 공격).
     *    실패 시 markUsed 를 하지 않아 같은 OTP 를 TTL(180초) 내 무제한 재시도 가능.
     */
    @Transactional
    public void consume(Long memberId, String phone, String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("거래 인증번호(OTP)가 필요합니다.");
        }
        EmailVerification v = verificationRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(phone, Purpose.TX_OTP)
                .orElseThrow(() -> new IllegalArgumentException("발급된 거래 인증번호가 없습니다."));
        if (!v.isValid(code)) {
            throw new IllegalArgumentException("거래 인증번호가 올바르지 않거나 만료되었습니다.");
        }
        v.markUsed();
    }

    private String generateCode() {
        return String.valueOf((int) (Math.random() * 900000) + 100000);
    }

    /** 010-1234-5678 → 010-****-5678. 형식이 다르면 원문을 보존. */
    private String mask(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() < 7) return phone;
        String head = digits.substring(0, 3);
        String tail = digits.substring(digits.length() - 4);
        return head + "-****-" + tail;
    }
}