package com.example.kidhackbackend.filtering;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.kidhackbackend.users.filtering.dto.HarmfulKeywordResponse;
import com.example.kidhackbackend.users.filtering.entity.HarmfulKeyword;
import com.example.kidhackbackend.users.filtering.repository.HarmfulKeywordRepository;
import com.example.kidhackbackend.users.filtering.service.BlockRuleService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/** #2 — 부모 읽기 전용 관리자 유해 키워드 목록 조회(getHarmfulKeywords). */
@DataJpaTest
@Import(BlockRuleService.class)
class HarmfulKeywordQueryTest {

    @Autowired BlockRuleService service;
    @Autowired HarmfulKeywordRepository keywordRepository;

    @Test
    void 관리자_키워드를_양수id와_keyword로_내려준다() {
        HarmfulKeyword k = keywordRepository.save(new HarmfulKeyword("성인물"));

        List<HarmfulKeywordResponse> result = service.getHarmfulKeywords();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(k.getId());   // 음수 아님 — 삭제와 무관한 읽기 전용
        assertThat(result.get(0).id()).isPositive();
        assertThat(result.get(0).keyword()).isEqualTo("성인물");
    }

    @Test
    void 최근_등록순으로_내려준다() {
        keywordRepository.save(new HarmfulKeyword("도박"));
        keywordRepository.save(new HarmfulKeyword("성인물"));
        keywordRepository.save(new HarmfulKeyword("폭력"));

        // 최근 등록(가장 큰 id)이 먼저.
        assertThat(service.getHarmfulKeywords()).extracting(HarmfulKeywordResponse::keyword)
                .containsExactly("폭력", "성인물", "도박");
    }

    @Test
    void 키워드가_없으면_빈_목록() {
        assertThat(service.getHarmfulKeywords()).isEmpty();
    }
}
