package com.example.kidhackbackend.users.efin.service;

import com.example.kidhackbackend.users.efin.dto.IdentityVerifyRequest;
import com.example.kidhackbackend.users.efin.dto.IdentityVerifyResponse;
import com.example.kidhackbackend.users.efin.entity.IdentityVerification;
import com.example.kidhackbackend.users.efin.repository.IdentityVerificationRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 접근매체 발급 시 실명확인 — 신분증 진위확인 결과를 저장하고 돌려준다.
 *
 * <p>⚠️ [VULN-EFIN-7] 진위확인이 {@link MockIdentityVerifier} 규칙 기반이고, 판정 결과를 응답에 노출한다.
 */
@Service
@RequiredArgsConstructor
public class IdentityVerificationService {

    private final MockIdentityVerifier verifier;
    private final IdentityVerificationRepository repository;

    @Transactional
    public IdentityVerifyResponse verify(Long memberId, IdentityVerifyRequest request) {
        boolean verified = verifier.verify(request);

        IdentityVerification saved = repository.save(new IdentityVerification(
                memberId, request.name(), parseBirth(request.birthDate()),
                request.idType(), verified));

        // [VULN-EFIN-7] 판정 결과를 그대로 응답에 노출.
        // [재점검 2026-09-07] 취약 확정 — 금취분평 027 [전자금융] 접근매체 발급 시 실명확인 / 주통기 IA(불충분한 인증 절차).
        //   외부 진위확인 대조 없이 형식·자기일치(자릿수, 앞6자리=생년월일)만으로 통과(MockIdentityVerifier) + 판정 결과 응답 노출.
        return new IdentityVerifyResponse(saved.isVerified(), saved.getName(), saved.getVerifiedAt());
    }

    /** 실명확인을 통과했는지(접근매체 발급 전 확인용). */
    @Transactional(readOnly = true)
    public boolean isVerified(Long memberId) {
        return repository.findTopByMemberIdOrderByCreatedAtDesc(memberId)
                .map(IdentityVerification::isVerified)
                .orElse(false);
    }

    /** YYMMDD → LocalDate(2000년대/1900년대 추정). 실패해도 저장은 계속(느슨한 mock). */
    private LocalDate parseBirth(String yymmdd) {
        try {
            String digits = yymmdd.replaceAll("[^0-9]", "");
            int yy = Integer.parseInt(digits.substring(0, 2));
            String century = yy <= 25 ? "20" : "19";
            return LocalDate.parse(century + digits, DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (Exception e) {
            return LocalDate.of(1900, 1, 1);
        }
    }
}