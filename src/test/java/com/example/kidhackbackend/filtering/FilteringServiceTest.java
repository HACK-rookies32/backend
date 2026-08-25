package com.example.kidhackbackend.filtering;

import com.example.kidhackbackend.filtering.entity.BlockRule;
import com.example.kidhackbackend.filtering.repository.BlockRuleRepository;
import com.example.kidhackbackend.filtering.service.BlockRuleService;
import com.example.kidhackbackend.filtering.service.FilteringService;
import com.example.kidhackbackend.global.notification.entity.NotificationType;
import com.example.kidhackbackend.global.notification.service.NotificationPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({BlockRuleService.class, FilteringService.class, FilteringServiceTest.TestConfig.class})
class FilteringServiceTest {

    /** 알림 발행을 가로채는 테스트용 NotificationPort. */
    static class RecordingPort implements NotificationPort {
        final List<NotificationType> published = new ArrayList<>();
        @Override
        public void publish(Long recipientId, NotificationType type, String message) {
            published.add(type);
        }
    }

    static class TestConfig {
        @Bean
        RecordingPort recordingPort() {
            return new RecordingPort();
        }
    }

    @Autowired BlockRuleService blockRuleService;
    @Autowired FilteringService filteringService;
    @Autowired BlockRuleRepository repository;
    @Autowired RecordingPort recordingPort;

    @Test
    void 블랙리스트에_걸리면_차단되고_알림이_발행된다() {
        blockRuleService.addRule(1L, "badsite.com");

        boolean blocked = filteringService.detect(1L, "http://www.BADSITE.com/page");

        assertThat(blocked).isTrue();
        assertThat(recordingPort.published).containsExactly(NotificationType.HARMFUL_SITE);
    }

    @Test
    void 블랙리스트에_없으면_차단되지_않고_알림도_없다() {
        blockRuleService.addRule(1L, "badsite.com");

        boolean blocked = filteringService.detect(1L, "http://goodsite.com");

        assertThat(blocked).isFalse();
        assertThat(recordingPort.published).isEmpty();
    }

    @Test
    void 규칙_삭제는_소유권_검증없이_id로_삭제된다_VULN() {
        BlockRule mine = blockRuleService.addRule(1L, "a.com");
        BlockRule others = blockRuleService.addRule(2L, "b.com"); // 다른 자녀의 규칙

        // [VULN-FILT-1] 소유권 검증 없이 남의 규칙도 id만으로 삭제됨
        blockRuleService.deleteRule(others.getId());

        assertThat(repository.findById(others.getId())).isEmpty();
        assertThat(repository.findById(mine.getId())).isPresent();
    }
}
