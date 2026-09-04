package com.example.kidhackbackend.filtering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.kidhackbackend.global.exception.DuplicateResourceException;
import com.example.kidhackbackend.users.filtering.entity.BlockRule;
import com.example.kidhackbackend.users.filtering.repository.BlockRuleRepository;
import com.example.kidhackbackend.users.filtering.service.BlockRuleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/** QA #40 — POST /api/filtering/rules 는 같은 자녀에 같은 패턴 중복 등록을 409(DuplicateResourceException)로 막는다. */
@DataJpaTest
@Import(BlockRuleService.class)
class BlockRuleDuplicateTest {

    private static final long CHILD = 7L;

    @Autowired BlockRuleService service;
    @Autowired BlockRuleRepository repository;

    @Test
    void 같은_자녀_같은_패턴_중복등록은_막는다() {
        service.addRule(CHILD, "badsite.com");

        assertThatThrownBy(() -> service.addRule(CHILD, "badsite.com"))
                .isInstanceOf(DuplicateResourceException.class);

        assertThat(repository.findByChildId(CHILD)).hasSize(1);   // 두 번째는 저장되지 않음
    }

    @Test
    void 앞뒤_공백만_다른_패턴도_중복으로_본다() {
        service.addRule(CHILD, "badsite.com");

        // addRule 이 trim 하므로 "  badsite.com  " 은 같은 값이다.
        assertThatThrownBy(() -> service.addRule(CHILD, "  badsite.com  "))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void 다른_자녀는_같은_패턴을_등록할_수_있다() {
        service.addRule(CHILD, "badsite.com");

        assertThatCode(() -> service.addRule(8L, "badsite.com")).doesNotThrowAnyException();
    }
}
