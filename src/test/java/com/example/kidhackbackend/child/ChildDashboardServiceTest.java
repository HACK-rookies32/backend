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
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    @Autowired MemberRepository memberRepository;

    private Long persistMember(Role role) {
        Member m = new Member();
        m.setLoginId(role + "_" + System.nanoTime());
        m.setUsername("테스트");
        m.setPassword("pw");
        m.setPhoneNumber("010-1234-5678");
        m.setBirthDate(LocalDate.of(2015, 3, 21));
        m.setRole(role);
        return memberRepository.save(m).getId();
    }

    @Test
    void 대시보드가_여러_도메인을_한번에_조합한다() {
        Long childId = persistMember(Role.CHILD);
        Long parentId = persistMember(Role.PARENT);

        // 위치 업로드 → 현재 위치 생성
        trackingService.record(new TrackRequest(childId, "37.5665", "126.9780"));
        // 실시간 토글 ON
        trackingSettingService.setRealtime(childId, true);
        // 자녀 알림 4건(최근 3건만 나와야 함)
        for (int i = 0; i < 4; i++) {
            notificationService.publish(childId, NotificationType.ETC, "msg" + i);
        }
        // 부모 알림 1건
        notificationService.publish(parentId, NotificationType.PAYMENT_APPROVED, "결제 승인");
        // 차단 규칙
        blockRuleService.addRule(childId, "badsite.com");

        ChildDashboardResponse d = dashboardService.getDashboard(childId, parentId);

        assertThat(d.childId()).isEqualTo(childId);
        assertThat(d.currentLocation()).isNotNull();
        assertThat(d.currentLocation().latitude()).isEqualTo("37.5665");
        assertThat(d.realtimeEnabled()).isTrue();
        assertThat(d.childNotifications()).hasSize(3);        // 최근 3건 제한
        assertThat(d.parentNotifications()).hasSize(1);
        assertThat(d.blockRules()).hasSize(1);
    }

    @Test
    void 데이터가_없는_자녀는_빈_대시보드를_반환한다() {
        Long childId = persistMember(Role.CHILD);
        Long parentId = persistMember(Role.PARENT);

        ChildDashboardResponse d = dashboardService.getDashboard(childId, parentId);

        assertThat(d.currentLocation()).isNull();
        assertThat(d.realtimeEnabled()).isFalse();
        assertThat(d.childNotifications()).isEmpty();
        assertThat(d.parentNotifications()).isEmpty();
        assertThat(d.blockRules()).isEmpty();
        assertThat(d.places()).isEmpty();
    }

    @Test
    void 존재하지_않는_childId는_예외를_던진다() {
        assertThatThrownBy(() -> dashboardService.getDashboard(999999L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("자녀를 찾을 수 없습니다.");
    }

    @Test
    void CHILD_가_아닌_대상은_예외를_던진다() {
        Long parentId = persistMember(Role.PARENT);

        assertThatThrownBy(() -> dashboardService.getDashboard(parentId, parentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("자녀를 찾을 수 없습니다.");
    }
}
