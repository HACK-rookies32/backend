package com.example.kidhackbackend.child;

import com.example.kidhackbackend.users.child.dto.ChildDashboardResponse;
import com.example.kidhackbackend.users.child.service.ChildDashboardService;
import com.example.kidhackbackend.users.filtering.repository.BlockRuleRepository;
import com.example.kidhackbackend.users.filtering.service.BlockRuleService;
import com.example.kidhackbackend.global.config.JpaAuditingConfig;
import com.example.kidhackbackend.global.notification.entity.NotificationType;
import com.example.kidhackbackend.global.notification.push.MockPushSender;
import com.example.kidhackbackend.global.notification.service.NotificationService;
import com.example.kidhackbackend.global.notification.service.NotificationSettingService;
import com.example.kidhackbackend.users.location.dto.TrackRequest;
import com.example.kidhackbackend.users.location.service.GeofenceService;
import com.example.kidhackbackend.users.location.service.LogAlertAdapter;
import com.example.kidhackbackend.users.location.service.PlaceService;
import com.example.kidhackbackend.users.location.service.TrackingService;
import com.example.kidhackbackend.users.location.service.TrackingSettingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
// @DataJpaTest 는 슬라이스라 컴포넌트 스캔을 하지 않는다. NotificationService 가 생성자로
// 받는 PushPort 구현체(MockPushSender)까지 직접 넣어 줘야 컨텍스트가 뜬다.
// 빠지면 ChildDashboardService -> NotificationService -> PushPort 순으로 타고 내려가
// NoSuchBeanDefinitionException('PushPort') 로 죽는다.
@Import({ChildDashboardService.class, TrackingService.class, TrackingSettingService.class,
        PlaceService.class, GeofenceService.class, LogAlertAdapter.class,
        NotificationService.class, NotificationSettingService.class, BlockRuleService.class,
        MockPushSender.class, JpaAuditingConfig.class})
class ChildDashboardServiceTest {

    @Autowired ChildDashboardService dashboardService;
    @Autowired TrackingService trackingService;
    @Autowired TrackingSettingService trackingSettingService;
    @Autowired NotificationService notificationService;
    @Autowired BlockRuleService blockRuleService;
    @Autowired BlockRuleRepository blockRuleRepository;

    @Test
    void 대시보드가_여러_도메인을_한번에_조합한다() {
        // 위치 업로드 → 현재 위치 생성
        trackingService.record(new TrackRequest(1L, "37.5665", "126.9780"));
        // 실시간 토글 ON
        trackingSettingService.setRealtime(1L, true);
        // 알림 4건(최근 3건만 나와야 함)
        for (int i = 0; i < 4; i++) {
            notificationService.publish(1L, NotificationType.ETC, "msg" + i);
        }
        // 차단 규칙
        blockRuleService.addRule(1L, "badsite.com");

        ChildDashboardResponse d = dashboardService.getDashboard(1L);

        assertThat(d.childId()).isEqualTo(1L);
        assertThat(d.currentLocation()).isNotNull();
        assertThat(d.currentLocation().latitude()).isEqualTo("37.5665");
        assertThat(d.realtimeEnabled()).isTrue();
        assertThat(d.recentNotifications()).hasSize(3);       // 최근 3건 제한
        assertThat(d.blockRules()).hasSize(1);
    }

    @Test
    void 데이터가_없는_자녀는_빈_대시보드를_반환한다() {
        ChildDashboardResponse d = dashboardService.getDashboard(999L);

        assertThat(d.currentLocation()).isNull();
        assertThat(d.realtimeEnabled()).isFalse();
        assertThat(d.recentNotifications()).isEmpty();
        assertThat(d.blockRules()).isEmpty();
        assertThat(d.places()).isEmpty();
    }
}
