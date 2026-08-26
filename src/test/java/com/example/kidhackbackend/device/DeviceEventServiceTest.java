package com.example.kidhackbackend.device;

import com.example.kidhackbackend.users.device.entity.DeviceEventType;
import com.example.kidhackbackend.users.device.repository.DeviceEventRepository;
import com.example.kidhackbackend.users.device.service.DeviceEventService;
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
@Import({DeviceEventService.class, DeviceEventServiceTest.TestConfig.class})
class DeviceEventServiceTest {

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

    @Autowired DeviceEventService deviceEventService;
    @Autowired DeviceEventRepository repository;
    @Autowired RecordingPort recordingPort;

    @Test
    void 전원꺼짐_보고시_기록되고_트래킹불가_알림이_발행된다() {
        deviceEventService.report(1L, DeviceEventType.POWER_OFF);

        assertThat(repository.findByChildIdOrderByCreatedAtDesc(1L)).hasSize(1);
        assertThat(recordingPort.published).containsExactly(NotificationType.DEVICE_POWER_OFF);
    }
}
