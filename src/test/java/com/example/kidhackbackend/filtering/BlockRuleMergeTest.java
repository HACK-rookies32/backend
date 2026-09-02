package com.example.kidhackbackend.filtering;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.kidhackbackend.users.filtering.dto.BlockRuleResponse;
import com.example.kidhackbackend.users.filtering.entity.BlockRule;
import com.example.kidhackbackend.users.filtering.entity.HarmfulKeyword;
import com.example.kidhackbackend.users.filtering.repository.BlockRuleRepository;
import com.example.kidhackbackend.users.filtering.repository.HarmfulKeywordRepository;
import com.example.kidhackbackend.users.filtering.service.BlockRuleService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/** #181(QA #17) — 자녀 앱 차단 목록에 부모 규칙 + 관리자 키워드가 함께 오는지. */
@DataJpaTest
@Import(BlockRuleService.class)
class BlockRuleMergeTest {

    private static final long CHILD = 7L;

    @Autowired BlockRuleService service;
    @Autowired BlockRuleRepository ruleRepository;
    @Autowired HarmfulKeywordRepository keywordRepository;

    @Test
    void 부모규칙과_관리자키워드가_함께_내려온다() {
        ruleRepository.save(new BlockRule(CHILD, "badsite.com"));
        keywordRepository.save(new HarmfulKeyword("성인물"));

        List<BlockRuleResponse> merged = service.getRulesForBlocking(CHILD);

        assertThat(merged).extracting(BlockRuleResponse::pattern)
                .containsExactlyInAnyOrder("badsite.com", "성인물");
        assertThat(merged).extracting(BlockRuleResponse::source)
                .containsExactlyInAnyOrder("PARENT", "ADMIN");
    }

    @Test
    void 관리자키워드는_음수id로_와_삭제충돌을_막는다() {
        HarmfulKeyword k = keywordRepository.save(new HarmfulKeyword("도박"));

        BlockRuleResponse admin = service.getRulesForBlocking(CHILD).stream()
                .filter(r -> "ADMIN".equals(r.source())).findFirst().orElseThrow();

        assertThat(admin.id()).isEqualTo(-k.getId());   // 음수 = 양수 block_rule 과 안 겹침
        assertThat(admin.id()).isNegative();
        assertThat(admin.childId()).isNull();           // 전역이라 childId 없음
    }

    @Test
    void 대시보드용_getRules는_부모규칙만_준다_변경없음() {
        ruleRepository.save(new BlockRule(CHILD, "badsite.com"));
        keywordRepository.save(new HarmfulKeyword("성인물"));

        // getRules(대시보드)는 관리자 키워드를 섞지 않는다 — 병합은 앱 차단 경로에만.
        assertThat(service.getRules(CHILD)).extracting(BlockRule::getPattern)
                .containsExactly("badsite.com");
    }
}
