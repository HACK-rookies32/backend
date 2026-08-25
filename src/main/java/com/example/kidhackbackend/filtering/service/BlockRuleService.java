package com.example.kidhackbackend.filtering.service;

import com.example.kidhackbackend.filtering.entity.BlockRule;
import com.example.kidhackbackend.filtering.repository.BlockRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/** 부모 차단 블랙리스트 CRUD 및 차단 판정 — 역할표 No.6. */
@Service
@RequiredArgsConstructor
public class BlockRuleService {

    private final BlockRuleRepository repository;

    @Transactional
    public BlockRule addRule(Long childId, String pattern) {
        return repository.save(new BlockRule(childId, pattern.trim()));
    }

    @Transactional(readOnly = true)
    public List<BlockRule> getRules(Long childId) {
        return repository.findByChildId(childId);
    }

    /**
     * 차단 규칙 삭제.
     *
     * <p>[VULN-FILT-1] IDOR + 하드 삭제 + 감사로그 없음 (모의해킹용 의도적 취약점).
     * <ul>
     *   <li>ruleId가 <b>요청자(부모)의 자녀 규칙인지 검증하지 않는다</b> → 남의 자녀 차단규칙을
     *       id만으로 삭제해 <b>보호를 무력화</b>할 수 있다.</li>
     *   <li>물리 삭제라 <b>복구 불가</b>하고, <b>누가 언제 삭제했는지 기록이 없다</b>(감사 부재).</li>
     * </ul>
     * 대응: ruleId 소유권 검증 + 부모 권한 강제 + 소프트 삭제 + 감사 로그.
     */
    @Transactional
    public void deleteRule(Long ruleId) {
        repository.deleteById(ruleId);
    }

    /** 대상(앱/URL)이 자녀의 블랙리스트에 걸리는지 판정. 패턴이 포함되면 차단. */
    @Transactional(readOnly = true)
    public boolean isBlocked(Long childId, String target) {
        String lowered = target.toLowerCase(Locale.ROOT);
        return repository.findByChildId(childId).stream()
                .anyMatch(rule -> lowered.contains(rule.getPattern().toLowerCase(Locale.ROOT)));
    }
}
