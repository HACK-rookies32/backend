package com.example.kidhackbackend.users.member.service;

import com.example.kidhackbackend.global.mail.EmailPort;
import com.example.kidhackbackend.global.mail.EmailVerification;
import com.example.kidhackbackend.global.mail.EmailVerificationRepository;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AccountRecoveryService {

    private static final long CODE_TTL_MINUTES = 5;

    private final MemberRepository memberRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailPort emailPort;

    @Transactional
    public String sendFindIdCode(String username, String email) {
        // 본인 이메일로 먼저 찾고(PARENT/ADMIN), 없으면 부모 이메일로 자녀 계정을 찾음
        Member target = memberRepository.findByUsernameAndEmail(username, email)
                .or(() -> memberRepository.findByUsernameAndParentEmail(username, email))
                .orElseThrow(() -> new IllegalArgumentException("일치하는 계정을 찾을 수 없습니다.")); // [VULN-3] 계열

        String code = generateCode();
        emailVerificationRepository.save(new EmailVerification(
                target.getId(), email, code, EmailVerification.Purpose.FIND_ID,
                LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES)));
        emailPort.sendVerificationCode(email, code);
        return code; // [VULN-AUTH-MAIL] 인증코드를 응답에도 그대로 노출
    }

    @Transactional
    public String verifyFindId(String email, String code) {
        EmailVerification verification = latestValid(email, EmailVerification.Purpose.FIND_ID, code);
        verification.markUsed();
        Member member = memberRepository.findById(verification.getTargetMemberId())
                .orElseThrow(() -> new IllegalArgumentException("일치하는 계정을 찾을 수 없습니다."));
        return member.getLoginId();
    }

    @Transactional
    public String sendResetPasswordCode(String loginId, String email) {
        Member target = memberRepository.findByLoginIdAndEmail(loginId, email)
                .or(() -> memberRepository.findByLoginIdAndParentEmail(loginId, email))
                .orElseThrow(() -> new IllegalArgumentException("일치하는 계정을 찾을 수 없습니다."));

        String code = generateCode();
        emailVerificationRepository.save(new EmailVerification(
                target.getId(), email, code, EmailVerification.Purpose.RESET_PASSWORD,
                LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES)));
        emailPort.sendVerificationCode(email, code);
        return code;
    }

    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        EmailVerification verification = latestValid(email, EmailVerification.Purpose.RESET_PASSWORD, code);
        verification.markUsed();
        Member member = memberRepository.findById(verification.getTargetMemberId())
                .orElseThrow(() -> new IllegalArgumentException("일치하는 계정을 찾을 수 없습니다."));
        member.setPassword(newPassword); // [VULN-1] 평문 저장 (기존과 동일)
        memberRepository.save(member);
    }

    private EmailVerification latestValid(String email, EmailVerification.Purpose purpose, String code) {
        EmailVerification verification = emailVerificationRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(email, purpose)
                .orElseThrow(() -> new IllegalArgumentException("인증 요청 내역이 없습니다."));
        if (!verification.isValid(code)) {
            throw new IllegalArgumentException("인증코드가 올바르지 않거나 만료되었습니다.");
        }
        return verification;
    }

    private String generateCode() {
        return String.valueOf((int) (Math.random() * 900000) + 100000);
    }
}