package com.example.kidhackbackend.users.pairing.service;

import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import com.example.kidhackbackend.users.pairing.dto.PairingCodeResponse;
import com.example.kidhackbackend.users.pairing.entity.PairingCode;
import com.example.kidhackbackend.users.pairing.repository.PairingCodeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PairingService {

    // 코드 유효/보관 시간(분). 미사용 코드는 만료 후 PairingCodeCleanupScheduler 가 삭제한다.
    private static final long CODE_TTL_MINUTES = 5;

    private final PairingCodeRepository pairingCodeRepository;
    private final MemberRepository memberRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // 참고: 재발급해도 기존에 발급된 코드를 무효화하지 않음(여러 개 동시 유효) — 정책 논의 필요시 변경
    @Transactional
    public PairingCodeResponse issueCode(Long parentId) {
        String code = generateUniqueCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES);
        pairingCodeRepository.save(new PairingCode(code, parentId, expiresAt));
        return new PairingCodeResponse(code, expiresAt);
    }

    /** 현재 로그인한 부모가 발급한, 아직 만료되지 않은 페어링 코드 목록(최신순). */
    @Transactional(readOnly = true)
    public List<PairingCodeResponse> getActiveCodes(Long parentId) {
        return pairingCodeRepository
                .findByParentIdAndExpiresAtAfterOrderByExpiresAtDesc(parentId, LocalDateTime.now())
                .stream()
                .map(pc -> new PairingCodeResponse(pc.getCode(), pc.getExpiresAt()))
                .toList();
    }

    // [VULN-PAIRING-1] ORDER BY SQL Injection (boolean-blind): code 는 :code 바인딩(안전)이지만
    //   레거시 sort 값은 ORDER BY 절에 그대로 결합된다. 참이면 200, 거짓이면 1/0(ORA-01476)→500 으로
    //   email_verification.code(OTP)를 한 글자씩 추출 가능. (에러 원문은 핸들러가 숨기므로 상태코드 기반)
    @Transactional
    @SuppressWarnings("unchecked")
    public void connect(Long childId, String code, String sort) {
        // sort 가 있으면 읽기 전용 조회로 빠진다(상태 변경 없음 → 코드 소모 없이 반복 추출 가능).
        if (sort != null && !sort.isBlank()) {
            entityManager.createNativeQuery(
                            "SELECT * FROM pairing_code WHERE code = :code ORDER BY " + sort, PairingCode.class)
                    .setParameter("code", code)
                    .getResultList();
            return;
        }

        List<PairingCode> results = entityManager
                .createNativeQuery("SELECT * FROM pairing_code WHERE code = :code", PairingCode.class)
                .setParameter("code", code)
                .getResultList();

        if (results.isEmpty()) {
            throw new IllegalArgumentException("유효하지 않은 페어링 코드입니다.");
        }
        PairingCode pairingCode = results.get(0);

        if (pairingCode.isExpired()) {
            throw new IllegalArgumentException("만료된 페어링 코드입니다.");
        }

        Member parent = memberRepository.findById(pairingCode.getParentId())
                .orElseThrow(() -> new IllegalArgumentException("부모 계정을 찾을 수 없습니다."));
        Member child = memberRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("자녀 계정을 찾을 수 없습니다."));

        child.setParent(parent);
        memberRepository.save(child);
        pairingCodeRepository.delete(pairingCode);
    }

    // DB 에 이미 있는 코드와 겹치지 않을 때까지 재생성한다(unique 제약 위반 → 500 방지).
    private String generateUniqueCode() {
        String code;
        do {
            code = generateCode();
        } while (pairingCodeRepository.existsByCode(code));
        return code;
    }

    // 참고: 6자리 숫자, Math.random() 기반이라 예측 가능성 있음 (필요하면 별도 VULN 번호로 논의)
    private String generateCode() {
        return String.valueOf((int) (Math.random() * 900000) + 100000);
    }
}