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

    private static final long CODE_TTL_MINUTES = 10;

    private final PairingCodeRepository pairingCodeRepository;
    private final MemberRepository memberRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // 참고: 재발급해도 기존에 발급된 코드를 무효화하지 않음(여러 개 동시 유효) — 정책 논의 필요시 변경
    @Transactional
    public PairingCodeResponse issueCode(Long parentId) {
        String code = generateCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES);
        pairingCodeRepository.save(new PairingCode(code, parentId, expiresAt));
        return new PairingCodeResponse(code, expiresAt);
    }

    // [VULN-PAIRING-1] SQL Injection: 코드 조회를 파라미터 바인딩 없이 문자열 결합으로 수행.
    //   code 파라미터에 ' OR '1'='1 등을 넣으면 임의 조건으로 다른 코드 행을 조회/우회 가능.
    @Transactional
    @SuppressWarnings("unchecked")
    public void connect(Long childId, String code) {
        String sql = "SELECT * FROM pairing_code WHERE code = '" + code + "'";
        List<PairingCode> results = entityManager.createNativeQuery(sql, PairingCode.class).getResultList();

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

    // 참고: 6자리 숫자, Math.random() 기반이라 예측 가능성 있음 (필요하면 별도 VULN 번호로 논의)
    private String generateCode() {
        return String.valueOf((int) (Math.random() * 900000) + 100000);
    }
}