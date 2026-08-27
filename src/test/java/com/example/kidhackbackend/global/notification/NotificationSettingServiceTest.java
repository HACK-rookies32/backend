package com.example.kidhackbackend.global.notification;

import com.example.kidhackbackend.global.notification.entity.NotificationSetting;
import com.example.kidhackbackend.global.notification.entity.NotificationType;
import com.example.kidhackbackend.global.notification.repository.NotificationSettingRepository;
import com.example.kidhackbackend.global.notification.service.NotificationSettingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 알림 설정 (명세서 4.2).
 *
 * <p>여기서 지키려는 것은 하나다. <b>감시받는 쪽이 스스로 감시를 끌 수 없어야 한다.</b>
 * 촬영 트리거는 수신자가 자녀이고 내용이 명령이라, 알림 토글에 섞이면 자녀가
 * {@code PATCH /api/users/me/notification-settings} 로 부모의 추가 촬영 요청을
 * 무력화할 수 있다.
 */
@DataJpaTest
@Import(NotificationSettingService.class)
class NotificationSettingServiceTest {

    @Autowired
    NotificationSettingService service;

    @Autowired
    NotificationSettingRepository repository;

    @Test
    void 일반_알림은_사용자가_끌_수_있다() {
        service.updateSettings(7L, Map.of(NotificationType.HARMFUL_SITE, false));

        assertThat(service.getSettings(7L)).containsEntry(NotificationType.HARMFUL_SITE, false);
        assertThat(service.isEnabled(7L, NotificationType.HARMFUL_SITE)).isFalse();
    }

    @Test
    void 촬영_트리거는_꺼_달라고_해도_꺼지지_않는다() {
        service.updateSettings(7L, Map.of(NotificationType.SCREENSHOT_REQUESTED, false));

        // 요청은 조용히 무시되고(명세서 4.2 — 정의되지 않은 key 는 무시),
        assertThat(service.isEnabled(7L, NotificationType.SCREENSHOT_REQUESTED)).isTrue();
        // 조회 응답에도 이 key 자체가 나오지 않는다(명세서 4.2 의 key 목록과 일치).
        assertThat(service.getSettings(7L)).doesNotContainKey(NotificationType.SCREENSHOT_REQUESTED);
    }

    @Test
    void 이미_꺼진_채로_저장된_행이_있어도_트리거는_통과한다() {
        Map<NotificationType, Boolean> stored = new EnumMap<>(NotificationType.class);
        stored.put(NotificationType.SCREENSHOT_REQUESTED, false);
        repository.save(new NotificationSetting(7L, stored));

        assertThat(service.isEnabled(7L, NotificationType.SCREENSHOT_REQUESTED)).isTrue();
        assertThat(service.getSettings(7L)).doesNotContainKey(NotificationType.SCREENSHOT_REQUESTED);
    }

    @Test
    void 설정을_한_번도_바꾸지_않으면_전부_켜짐이다() {
        Map<NotificationType, Boolean> defaults = service.getSettings(7L);

        assertThat(defaults).containsEntry(NotificationType.LOCATION_ENTER, true);
        assertThat(defaults.values()).containsOnly(true);
        assertThat(defaults).doesNotContainKey(NotificationType.SCREENSHOT_REQUESTED);
    }
}
