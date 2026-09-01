package com.example.kidhackbackend.monitoring;

import com.example.kidhackbackend.global.notification.entity.NotificationType;
import com.example.kidhackbackend.global.notification.service.NotificationPort;
import com.example.kidhackbackend.users.monitoring.dto.GroupBy;
import com.example.kidhackbackend.users.monitoring.dto.UsageAggregateResponse;
import com.example.kidhackbackend.users.monitoring.dto.UsageReportRequest;
import com.example.kidhackbackend.users.monitoring.dto.UsageReportRequest.UsageItem;
import com.example.kidhackbackend.global.config.JpaAuditingConfig;
import com.example.kidhackbackend.users.monitoring.entity.AppUsage;
import com.example.kidhackbackend.users.monitoring.repository.AppUsageRepository;
import com.example.kidhackbackend.users.monitoring.service.AppUsageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({AppUsageService.class, JpaAuditingConfig.class, AppUsageServiceTest.TestConfig.class})
class AppUsageServiceTest {

    /** 자녀 기기로 나가는 요청을 가로채는 테스트용 NotificationPort. */
    static class RecordingPort implements NotificationPort {
        final List<NotificationType> published = new ArrayList<>();
        final List<Long> recipients = new ArrayList<>();

        @Override
        public void publish(Long recipientId, NotificationType type, String message) {
            published.add(type);
            recipients.add(recipientId);
        }
    }

    static class TestConfig {
        @Bean
        RecordingPort recordingPort() {
            return new RecordingPort();
        }
    }

    @Autowired
    RecordingPort recordingPort;

    @Autowired
    AppUsageService service;

    @Autowired
    AppUsageRepository repository;

    /**
     * 자녀 앱은 1시간 주기로만 올린다. 부모가 리포트를 새로고침할 때 이 요청이 나가야
     * 방금까지의 사용시간이 반영된다.
     */
    @Test
    void 보고_요청은_자녀_기기로_나간다() {
        service.requestReport(7L);

        assertThat(recordingPort.published).containsExactly(NotificationType.USAGE_REPORT_REQUESTED);
        // 받는 쪽은 부모가 아니라 자녀다 — 자녀 기기를 깨우는 것이 목적이다.
        assertThat(recordingPort.recipients).containsExactly(7L);
    }

    @Test
    void 배치_사용량을_저장한다() {
        int saved = service.report(new UsageReportRequest(1L, List.of(
                new UsageItem("YouTube", 600, LocalDateTime.now()),
                new UsageItem("Game", 300, LocalDateTime.now()))));

        assertThat(saved).isEqualTo(2);
        assertThat(repository.findByChildIdAndRecordedAtBetween(
                1L, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1))).hasSize(2);
    }

    @Test
    void 앱별로_집계한다() {
        LocalDateTime t = LocalDateTime.now();
        service.report(new UsageReportRequest(1L, List.of(
                new UsageItem("YouTube", 600, t),
                new UsageItem("YouTube", 400, t),
                new UsageItem("Game", 300, t))));

        UsageAggregateResponse res = service.aggregate(1L, GroupBy.APP, null, null);

        assertThat(res.items()).containsExactly(
                new UsageAggregateResponse.Bucket("Game", 300),
                new UsageAggregateResponse.Bucket("YouTube", 1000)); // key 정렬(Game < YouTube)
    }

    @Test
    void 일별로_집계한다() {
        LocalDateTime today = LocalDateTime.now();
        service.report(new UsageReportRequest(1L, List.of(
                new UsageItem("A", 100, today),
                new UsageItem("B", 200, today))));

        UsageAggregateResponse res = service.aggregate(1L, GroupBy.DAY, null, null);

        assertThat(res.items()).hasSize(1);
        assertThat(res.items().get(0).durationSec()).isEqualTo(300);
        assertThat(res.items().get(0).key()).isEqualTo(today.toLocalDate().toString());
    }

    @Test
    void 보관기간_경과_사용량은_롤링_삭제된다() {
        LocalDateTime old = LocalDateTime.now().minusDays(10);
        AppUsage e = new AppUsage(1L, "A", 100, old);
        repository.save(e);
        // createdAt(=적재시각)이 지금이라 8일 기준으론 안 지워지지만, 10일 전 기준으론 삭제 확인
        int deleted = repository.deleteByCreatedAtBefore(LocalDateTime.now().plusSeconds(1));
        assertThat(deleted).isEqualTo(1);
    }
}
