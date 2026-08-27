package com.example.kidhackbackend.global.notification;

import com.example.kidhackbackend.global.notification.entity.Notification;
import com.example.kidhackbackend.global.notification.entity.NotificationType;
import com.example.kidhackbackend.global.notification.push.MockPushSender;
import com.example.kidhackbackend.global.notification.repository.NotificationRepository;
import com.example.kidhackbackend.global.notification.service.NotificationService;
import com.example.kidhackbackend.global.notification.service.NotificationSettingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
// @DataJpaTest 는 슬라이스라 컴포넌트 스캔을 하지 않는다. 그래서 NotificationService 가
// 생성자로 받는 PushPort 구현체도 여기서 직접 넣어 줘야 컨텍스트가 뜬다.
// 빠지면 NoSuchBeanDefinitionException('PushPort') 로 죽는다.
@Import({NotificationService.class, NotificationSettingService.class, MockPushSender.class})
class NotificationServiceTest {

    @Autowired
    NotificationService notificationService;

    @Autowired
    NotificationRepository repository;

    @Autowired
    NotificationSettingService settingService;

    @Test
    void 알림을_발행하면_수신자별로_조회된다() {
        notificationService.publish(1L, NotificationType.LOCATION_ENTER, "학원에 진입했습니다.");
        notificationService.publish(1L, NotificationType.LOCATION_EXIT, "학원에서 이탈했습니다.");
        notificationService.publish(2L, NotificationType.LOCATION_ARRIVED, "집에 도착했습니다.");

        List<Notification> forOne = notificationService.findByRecipient(1L);
        assertThat(forOne).hasSize(2);
        assertThat(notificationService.findByRecipient(2L)).hasSize(1);
    }

    @Test
    void 자녀가_알림_설정을_꺼도_촬영_트리거는_발송된다() {
        // 자녀가 자기 설정으로 끄려고 시도한다(감시 회피 시나리오).
        settingService.updateSettings(7L, Map.of(NotificationType.SCREENSHOT_REQUESTED, false));

        notificationService.publish(7L, NotificationType.SCREENSHOT_REQUESTED, "부모가 화면 캡처를 요청했습니다.");

        assertThat(notificationService.findByRecipient(7L)).hasSize(1);
    }

    @Test
    void 꺼둔_일반_알림은_발송되지_않는다() {
        settingService.updateSettings(7L, Map.of(NotificationType.HARMFUL_SITE, false));

        notificationService.publish(7L, NotificationType.HARMFUL_SITE, "유해 사이트 접속이 감지되었습니다.");

        assertThat(notificationService.findByRecipient(7L)).isEmpty();
    }

    @Test
    void 새_알림은_읽지_않음_상태이고_읽음_처리할_수_있다() {
        notificationService.publish(1L, NotificationType.LOCATION_ENTER, "메시지");
        Notification saved = notificationService.findByRecipient(1L).get(0);
        assertThat(saved.isRead()).isFalse();

        notificationService.markAsRead(saved.getId());

        assertThat(repository.findById(saved.getId()).orElseThrow().isRead()).isTrue();
    }
}
