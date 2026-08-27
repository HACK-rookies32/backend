package com.example.kidhackbackend.global.notification;

import com.example.kidhackbackend.global.notification.entity.DeviceToken;
import com.example.kidhackbackend.global.notification.entity.Notification;
import com.example.kidhackbackend.global.notification.entity.NotificationType;
import com.example.kidhackbackend.global.notification.push.PushResult;
import com.example.kidhackbackend.global.notification.repository.DeviceTokenRepository;
import com.example.kidhackbackend.global.notification.repository.NotificationRepository;
import com.example.kidhackbackend.global.notification.service.NotificationService;
import com.example.kidhackbackend.global.notification.service.NotificationSettingService;
import org.junit.jupiter.api.BeforeEach;
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
@Import({NotificationService.class, NotificationSettingService.class, FakePushSender.class})
class NotificationServiceTest {

    @Autowired
    NotificationService notificationService;

    @Autowired
    NotificationRepository repository;

    @Autowired
    DeviceTokenRepository deviceTokenRepository;

    @Autowired
    NotificationSettingService settingService;

    @Autowired
    FakePushSender pushSender;

    @BeforeEach
    void resetPushSender() {
        pushSender.reset();
    }

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
        assertThat(pushSender.sent()).isEmpty();
    }

    @Test
    void 새_알림은_읽지_않음_상태이고_읽음_처리할_수_있다() {
        notificationService.publish(1L, NotificationType.LOCATION_ENTER, "메시지");
        Notification saved = notificationService.findByRecipient(1L).get(0);
        assertThat(saved.isRead()).isFalse();

        notificationService.markAsRead(saved.getId());

        assertThat(repository.findById(saved.getId()).orElseThrow().isRead()).isTrue();
    }

    @Test
    void 기기_토큰이_있으면_사람이_읽는_제목과_data_로_푸시가_나간다() {
        deviceTokenRepository.save(new DeviceToken(1L, "token-1"));

        notificationService.publish(1L, NotificationType.PAYMENT_APPROVED, "자녀가 1,000원을 결제했습니다.");

        Long notificationId = notificationService.findByRecipient(1L).get(0).getId();
        assertThat(pushSender.sent()).singleElement().satisfies(sent -> {
            assertThat(sent.token()).isEqualTo("token-1");
            // 예전에는 제목 자리에 "PAYMENT_APPROVED" 가 그대로 들어갔다.
            assertThat(sent.message().title()).isEqualTo("결제 승인");
            assertThat(sent.message().body()).isEqualTo("자녀가 1,000원을 결제했습니다.");
            assertThat(sent.message().data())
                    .containsEntry("type", "PAYMENT_APPROVED")
                    .containsEntry("notificationId", String.valueOf(notificationId));
        });
    }

    @Test
    void 기기_토큰이_없으면_알림만_저장되고_푸시는_생략된다() {
        notificationService.publish(1L, NotificationType.LOCATION_ENTER, "학원에 진입했습니다.");

        assertThat(notificationService.findByRecipient(1L)).hasSize(1);
        assertThat(pushSender.sent()).isEmpty();
    }

    @Test
    void FCM_이_무효라고_답한_토큰은_지운다() {
        deviceTokenRepository.save(new DeviceToken(1L, "dead-token"));
        pushSender.willReturn("dead-token", PushResult.TOKEN_INVALID);

        notificationService.publish(1L, NotificationType.NOTICE, "공지");

        assertThat(deviceTokenRepository.findByMemberId(1L)).isEmpty();
    }

    @Test
    void 일시적인_발송_실패로는_토큰을_지우지_않는다() {
        deviceTokenRepository.save(new DeviceToken(1L, "live-token"));
        pushSender.willReturn("live-token", PushResult.FAILED);

        notificationService.publish(1L, NotificationType.NOTICE, "공지");

        // 네트워크 오류로 멀쩡한 토큰을 날리면 그 사용자는 영영 알림을 못 받는다.
        assertThat(deviceTokenRepository.findByMemberId(1L)).isPresent();
    }

    @Test
    void 공지_전체발송은_알림을_모두_저장하고_토큰_있는_사람에게만_푸시한다() {
        deviceTokenRepository.save(new DeviceToken(1L, "token-1"));
        deviceTokenRepository.save(new DeviceToken(2L, "token-2"));
        // 3번 회원은 앱을 설치하지 않아 토큰이 없다.

        notificationService.publishAll(List.of(1L, 2L, 3L), NotificationType.NOTICE, "새 공지가 등록되었습니다");

        assertThat(notificationService.findByRecipient(1L)).hasSize(1);
        assertThat(notificationService.findByRecipient(3L)).hasSize(1);
        assertThat(pushSender.sent()).extracting(FakePushSender.Sent::token)
                .containsExactlyInAnyOrder("token-1", "token-2");
    }

    @Test
    void 공지를_꺼둔_사람은_전체발송에서도_빠진다() {
        settingService.updateSettings(2L, Map.of(NotificationType.NOTICE, false));
        deviceTokenRepository.save(new DeviceToken(1L, "token-1"));
        deviceTokenRepository.save(new DeviceToken(2L, "token-2"));

        notificationService.publishAll(List.of(1L, 2L), NotificationType.NOTICE, "새 공지가 등록되었습니다");

        assertThat(notificationService.findByRecipient(2L)).isEmpty();
        assertThat(pushSender.sent()).extracting(FakePushSender.Sent::token)
                .containsExactly("token-1");
    }
}