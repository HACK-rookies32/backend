package com.example.kidhackbackend.users.filtering.service;

import com.example.kidhackbackend.users.filtering.dto.BlockRuleResponse;
import com.example.kidhackbackend.users.filtering.dto.HarmfulKeywordResponse;
import com.example.kidhackbackend.users.filtering.entity.BlockRule;
import com.example.kidhackbackend.users.filtering.entity.HarmfulKeyword;
import com.example.kidhackbackend.users.filtering.repository.BlockRuleRepository;
import com.example.kidhackbackend.users.filtering.repository.HarmfulKeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 부모 차단 블랙리스트 CRUD 및 차단 판정 — 역할표 No.6. */
@Service
@RequiredArgsConstructor
public class BlockRuleService {

    private final BlockRuleRepository repository;
    private final HarmfulKeywordRepository harmfulKeywordRepository;

    @Transactional
    public BlockRule addRule(Long childId, String pattern) {
        return repository.save(new BlockRule(childId, pattern.trim()));
    }

    @Transactional(readOnly = true)
    public List<BlockRule> getRules(Long childId) {
        return repository.findByChildId(childId);
    }

    /**
     * 자녀 앱의 기기 차단 목록용 조회(QA #17) — 부모 규칙 + 관리자 전역 키워드를 한 목록으로.
     *
     * <p>자녀 앱은 이 목록으로 기기 안에서 유해 콘텐츠를 차단한다. 예전엔 부모 규칙만 줘서 관리자
     * 키워드가 반영되지 않았다. 관리자 키워드는 음수 id 로 실어 앱에서 삭제되지 않게 한다
     * ({@link BlockRuleResponse} 참고). 관리자 테이블은 소규모(수기 등록)라 매 호출 전수 조회로 충분하다.
     *
     * <p>대시보드용 {@link #getRules}(부모 규칙만)는 그대로 둔다 — 이 병합은 앱 차단 경로에만 쓴다.
     */
    @Transactional(readOnly = true)
    public List<BlockRuleResponse> getRulesForBlocking(Long childId) {
        List<BlockRuleResponse> merged = new ArrayList<>();
        repository.findByChildId(childId).forEach(r -> merged.add(BlockRuleResponse.from(r)));
        harmfulKeywordRepository.findAll().forEach(k -> merged.add(BlockRuleResponse.fromAdmin(k)));
        return merged;
    }

    /**
     * 관리자 지정 전역 유해 키워드 목록 — 회원(부모) 읽기 전용 조회(모바일 요청 #2).
     *
     * <p>기존엔 관리자 키워드를 부모에게 목록으로 내려주는 경로가 없어(관리자 API는 admin 인증 전용,
     * {@link #getRulesForBlocking}은 자녀별 규칙과 병합된 앱 차단용) 부모 앱이 전역 키워드를 따로
     * 표시할 수 없었다. 이 조회는 관리자 키워드만 최근 등록순으로 내려준다. 소유권 개념이 없는
     * 전역 정책 데이터라 childId 파라미터가 없고 IDOR 여지도 없다.
     */
    @Transactional(readOnly = true)
    public List<HarmfulKeywordResponse> getHarmfulKeywords() {
        return harmfulKeywordRepository.findAllByOrderByIdDesc().stream()
                .map(HarmfulKeywordResponse::from)
                .toList();
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
        // 없는 id 면 조용히 통과(멱등). deleteById 를 그냥 호출하면
        // EmptyResultDataAccessException → 500 으로 나가버린다.
        if (repository.existsById(ruleId)) {
            repository.deleteById(ruleId);
        }
    }

    /**
     * 대상(앱/URL)이 차단 목록에 걸리는지 판정. 패턴이 포함되면 차단.
     *
     * <p>부모가 등록한 자녀별 규칙과, 관리자가 등록한 전역 차단 키워드를 함께 검사한다.
     */
    @Transactional(readOnly = true)
    public boolean isBlocked(Long childId, String target) {
        String lowered = target.toLowerCase(Locale.ROOT);

        boolean blockedByRule = repository.findByChildId(childId).stream()
                .anyMatch(rule -> lowered.contains(rule.getPattern().toLowerCase(Locale.ROOT)));
        if (blockedByRule) {
            return true;
        }

        return harmfulKeywordRepository.findAll().stream()
                .map(HarmfulKeyword::getKeyword)
                .anyMatch(keyword -> lowered.contains(keyword.toLowerCase(Locale.ROOT)));
    }
}
