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
                new UsageItem("YouTube", null, 600, LocalDateTime.now().toString()),
                new UsageItem("Game", null, 300, LocalDateTime.now().toString()))));

        assertThat(saved).isEqualTo(2);
        assertThat(repository.findByChildIdAndRecordedAtBetween(
                1L, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1))).hasSize(2);
    }

    /**
     * 같은 앱이 기기 언어에 따라 다른 이름으로 들어온다. 패키지명이 같으면 한 줄이어야 한다.
     */
    @Test
    void 이름이_달라도_패키지가_같으면_한_앱으로_묶는다() {
        LocalDateTime early = LocalDateTime.now().minusHours(2);
        LocalDateTime late = LocalDateTime.now();
        service.report(new UsageReportRequest(1L, List.of(
                new UsageItem("Camera", "com.android.camera", 18, early.toString()),
                new UsageItem("카메라", "com.android.camera", 78, late.toString()))));

        UsageAggregateResponse res = service.aggregate(
                1L, GroupBy.APP, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));

        assertThat(res.items()).hasSize(1);
        // 화면에는 패키지명이 아니라 가장 최근에 기록된 이름을 내보낸다.
        assertThat(res.items().get(0).key()).isEqualTo("카메라");
        assertThat(res.items().get(0).durationSec()).isEqualTo(96);
    }

    /** 패키지명이 없는 옛 행은 예전처럼 앱 이름으로 묶여야 한다. */
    @Test
    void 패키지명이_없으면_앱_이름으로_묶는다() {
        LocalDateTime t = LocalDateTime.now();
        service.report(new UsageReportRequest(1L, List.of(
                new UsageItem("Gmail", null, 10, t.toString()),
                new UsageItem("Gmail", null, 5, t.toString()))));

        UsageAggregateResponse res = service.aggregate(
                1L, GroupBy.APP, t.minusDays(1), t.plusDays(1));

        assertThat(res.items()).hasSize(1);
        assertThat(res.items().get(0).key()).isEqualTo("Gmail");
        assertThat(res.items().get(0).durationSec()).isEqualTo(15);
    }

    @Test
    void 앱별로_집계한다() {
        LocalDateTime t = LocalDateTime.now();
        service.report(new UsageReportRequest(1L, List.of(
                new UsageItem("YouTube", null, 600, t.toString()),
                new UsageItem("YouTube", null, 400, t.toString()),
                new UsageItem("Game", null, 300, t.toString()))));

        UsageAggregateResponse res = service.aggregate(1L, GroupBy.APP, null, null);

        assertThat(res.items()).containsExactly(
                new UsageAggregateResponse.Bucket("Game", 300),
                new UsageAggregateResponse.Bucket("YouTube", 1000)); // key 정렬(Game < YouTube)
    }

    @Test
    void 일별로_집계한다() {
        LocalDateTime today = LocalDateTime.now();
        service.report(new UsageReportRequest(1L, List.of(
                new UsageItem("A", null, 100, today.toString()),
                new UsageItem("B", null, 200, today.toString()))));

        UsageAggregateResponse res = service.aggregate(1L, GroupBy.DAY, null, null);

        assertThat(res.items()).hasSize(1);
        assertThat(res.items().get(0).durationSec()).isEqualTo(300);
        assertThat(res.items().get(0).key()).isEqualTo(today.toLocalDate().toString());
    }

    @Test
    void 보관기간_경과_사용량은_롤링_삭제된다() {
        LocalDateTime old = LocalDateTime.now().minusDays(10);
        AppUsage e = new AppUsage(1L, "A", null, 100, old);
        repository.save(e);
        // createdAt(=적재시각)이 지금이라 8일 기준으론 안 지워지지만, 10일 전 기준으론 삭제 확인
        int deleted = repository.deleteByCreatedAtBefore(LocalDateTime.now().plusSeconds(1));
        assertThat(deleted).isEqualTo(1);
    }
}
