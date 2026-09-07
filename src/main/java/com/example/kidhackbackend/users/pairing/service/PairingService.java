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

    // [VULN-PAIRING-1] SQL Injection — 코드 조회를 파라미터 바인딩 없이 문자열 결합으로 만든다.
    //   아래 sql 은 사용자가 보낸 code 를 따옴표 사이에 그대로 끼워 넣는다. 그래서 code 안에
    //   따옴표를 넣으면 "값"이 아니라 "질의문 자체"를 바꿀 수 있다.
    //
    //   2026-09-07 모의해킹에서 실제로 통한 입력은 다음과 같다(자녀 attacker_child(123) 계정).
    //     code = zzz' OR parent_id=120 --
    //   이러면 완성되는 질의는 이렇게 된다.
    //     SELECT * FROM pairing_code WHERE code = 'zzz' OR parent_id=120 -- '
    //   · 'zzz' 는 존재하지 않는 코드라 첫 조건은 거짓이지만, OR 뒤가 참이면 행이 돌아온다.
    //   · parent_id=120 은 "120번 부모가 발급한 코드 행"을 뜻한다. 즉 붙고 싶은 부모를 직접 지정한다.
    //   · 끝의 -- 는 SQL 주석이라, 원래 문장에 남아 있던 닫는 따옴표를 무력화해 문법 오류를 막는다.
    //   결과: 유효한 페어링 코드를 하나도 모르는 자녀가 임의의 부모 계정에 자녀로 연결된다.
    //
    //   ' OR '1'='1 처럼 조건을 항상 참으로 만드는 고전적 입력도 통하지만, 그때는 어느 부모의
    //   행이 뽑힐지 통제할 수 없다. 아래 results.get(0) 에 ORDER BY 가 없어 "첫 행"을 DB 가
    //   임의로 정하기 때문이다. 부모를 지정하려면 위처럼 parent_id 를 직접 거는 편이 확실하다.
    //
    //   방어: 문자열 결합을 없애고 파라미터 바인딩을 쓴다.
    //         pairingCodeRepository.findByCode(code) 또는 createNativeQuery(..).setParameter(..).
    @Transactional
    @SuppressWarnings("unchecked")
    public void connect(Long childId, String code) {
        String sql = "SELECT * FROM pairing_code WHERE code = '" + code + "'";
        List<PairingCode> results = entityManager.createNativeQuery(sql, PairingCode.class).getResultList();

        if (results.isEmpty()) {
            throw new IllegalArgumentException("유효하지 않은 페어링 코드입니다.");
        }
        // 주입으로 여러 행이 걸려도 ORDER BY 없이 첫 행 하나만 쓴다. 어느 행이 첫 행인지는
        // DB 가 정하므로, 공격자는 위처럼 parent_id 를 직접 지정해 원하는 행만 걸리게 만든다.
        PairingCode pairingCode = results.get(0);

        if (pairingCode.isExpired()) {
            throw new IllegalArgumentException("만료된 페어링 코드입니다.");
        }

        Member parent = memberRepository.findById(pairingCode.getParentId())
                .orElseThrow(() -> new IllegalArgumentException("부모 계정을 찾을 수 없습니다."));
        Member child = memberRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("자녀 계정을 찾을 수 없습니다."));

        // 여기가 실제 피해가 확정되는 지점이다. 위에서 뽑힌 행의 parentId 를 그대로 부모로 삼아
        // 자녀 계정에 저장하므로, 주입으로 고른 부모가 이 자녀의 부모가 된다. 그 부모는 이후
        // 위치 조회·스크린샷 등 자녀 감시 기능을 정상 권한으로 쓸 수 있게 된다.
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