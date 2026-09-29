package com.example.kidhackbackend.users.member.service;

import com.example.kidhackbackend.global.mail.EmailVerification;
import com.example.kidhackbackend.global.mail.EmailVerification.Purpose;
import com.example.kidhackbackend.global.mail.EmailVerificationRepository;
import com.example.kidhackbackend.global.sms.SmsPort;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입 휴대폰 인증 - 인증코드 발급·검증.
 *
 * <p>거래 OTP({@code EfinOtpService})와 동일하게 별도 테이블 없이
 * {@link EmailVerification}(Purpose=SIGNUP_PHONE)을 재사용한다 — {@code email} 컬럼에 전화번호를 담고,
 * 가입 전이라 {@code targetMemberId}=0 을 쓴다. 발신은 {@link SmsPort} mock(로그만).
 *
 * <p>⚠️ [VULN-PHONEVERIFY-1] 발급 코드를 발신 채널(SMS)과 같은 응답 body 에 그대로 노출한다
 * ({@code PhoneVerifySendCodeResponse}). 정상이라면 코드는 그 번호 소유자에게만 SMS 로 가야 한다.
 * <p>⚠️ [VULN-PHONEVERIFY-1b] 검증 실패 횟수 제한이 없다 — 무제한 시도 가능(EfinOtpService VULN-EFIN-1b 계열).
 * <p>⚠️ [VULN-PHONEVERIFY-2] 인증 결과가 가입(signup)과 서버에서 묶이지 않는다. {@code verify} 는
 * {valid} 를 돌려줄 뿐이고 이 결과를 클라이언트가 신뢰한다 — 응답을 valid:true 로 변조하면
 * 틀린 코드로도 통과하고, 애초에 signup 은 인증 여부를 확인하지 않는다(인증이 장식이 된다).
 */
@Service
@RequiredArgsConstructor
public class PhoneVerificationService {

    private static final long CODE_TTL_SECONDS = 180;

    private final SmsPort smsPort;
    private final EmailVerificationRepository verificationRepository;

    /** 휴대폰 인증코드 발급 → 저장 + mock SMS 발신 + (응답용) 코드 반환. */
    @Transactional
    public String sendCode(String phoneNumber) {
        String code = generateCode();
        verificationRepository.save(new EmailVerification(
                0L, phoneNumber, code, Purpose.SIGNUP_PHONE,
                LocalDateTime.now().plusSeconds(CODE_TTL_SECONDS)));
        smsPort.sendOtp(phoneNumber, code); // mock: 로그만 남김([MOCK SMS] ...)
        return code;
    }

    /**
     * 입력 코드가 유효한지 검증한다(소비하지 않음 — 시도 제한 없음 [VULN-PHONEVERIFY-1b]).
     * 서버는 이 결과를 강제하지 않고 {valid} 로만 돌려주므로, 클라이언트가 신뢰하는 이 응답이
     * 곧 우회 지점이 된다([VULN-PHONEVERIFY-2]).
     */
    @Transactional(readOnly = true)
    public boolean verify(String phoneNumber, String code) {
        return verificationRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(phoneNumber, Purpose.SIGNUP_PHONE)
                .map(v -> v.isValid(code))
                .orElse(false);
    }

    /** 6자리 숫자 코드. EfinOtpService 와 동일한 방식(앞자리 0 없음, 100000~999999). */
    private String generateCode() {
        return String.valueOf((int) (Math.random() * 900000) + 100000);
    }
}
