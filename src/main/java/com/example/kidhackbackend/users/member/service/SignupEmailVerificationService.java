package com.example.kidhackbackend.users.member.service;

import com.example.kidhackbackend.global.mail.EmailPort;
import com.example.kidhackbackend.global.mail.EmailVerification;
import com.example.kidhackbackend.global.mail.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 회원가입 이메일 인증(mock).
 *
 * <p>기존 아이디찾기·비번재설정({@link AccountRecoveryService})과 동일한 인프라
 * ({@link EmailVerification} 테이블 + {@link EmailPort} mock 발송)를 재사용한다.
 * 다만 가입 시점엔 아직 회원이 없으므로 계정 조회 단계가 없고, 검증행의
 * {@code targetMemberId} 에는 회원 대신 sentinel(0) 을 담는다.
 *
 * <p><b>검증 자체는 정상이다</b> — 코드 일치·5분 만료·1회 사용({@code markUsed})을 모두
 * 검사한다. 서버측에는 잔존행 replay·만료 무시 같은 우회 결함을 두지 않았다.
 *
 * <p>⚠️ 의도된 취약점은 서비스가 아니라 "결과 전달"에 있다:
 * <ul>
 *   <li>[VULN-EMAILVERIFY-1] 검증 boolean 을 응답에 노출하고, 인증 통과 판단을 모바일
 *       클라이언트가 그 값으로 내린다 → 응답 변조(false→true)로 우회 가능.</li>
 *   <li>[VULN-EMAILVERIFY-2] 발송 코드를 응답 {@code data.code} 에도 노출(mock).</li>
 * </ul>
 * 서버는 인증 결과로 로그인·가입을 막지 않는다(사용자 결정: 클라이언트 UX 만 게이트).
 */
@Service
@RequiredArgsConstructor
public class SignupEmailVerificationService {

    /** 인증코드 유효시간(분) — AccountRecoveryService 와 동일. */
    private static final long CODE_TTL_MINUTES = 5;

    /** 가입 시점엔 회원이 없어 targetMemberId 에 넣는 sentinel. */
    private static final long NO_MEMBER_YET = 0L;

    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailPort emailPort;

    /**
     * 가입 이메일로 6자리 인증코드를 발송(mock)하고, 그 코드를 반환한다.
     *
     * @return 발송된 코드 — [VULN-EMAILVERIFY-2] 로 응답 body 에도 그대로 실린다.
     */
    @Transactional
    public String sendCode(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일은 필수입니다.");
        }
        String code = generateCode();
        emailVerificationRepository.save(new EmailVerification(
                NO_MEMBER_YET, email, code, EmailVerification.Purpose.SIGNUP,
                LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES)));
        emailPort.sendVerificationCode(email, code); // MockEmailSender: 로그로만 출력
        return code;
    }

    /**
     * 발송받은 코드를 검증한다. 정상 검증(코드일치·만료·used) 후 통과 시 1회 소진한다.
     *
     * <p>반환한 boolean 은 컨트롤러가 그대로 응답에 실어 보낸다([VULN-EMAILVERIFY-1]).
     * 코드가 없거나 틀리면 {@code false} 를 반환할 뿐, 예외로 막지 않는다 —
     * 판단은 클라이언트 몫이라는 (의도된) 설계를 그대로 드러낸다.
     */
    @Transactional
    public boolean verify(String email, String code) {
        EmailVerification verification = emailVerificationRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(email, EmailVerification.Purpose.SIGNUP)
                .orElse(null);
        if (verification == null || !verification.isValid(code)) {
            return false;
        }
        verification.markUsed(); // 정상 검증 통과분은 1회 소진(잔존행 재사용 방지)
        return true;
    }

    private String generateCode() {
        return String.valueOf((int) (Math.random() * 900000) + 100000);
    }
}
