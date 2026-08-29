package com.example.kidhackbackend.device;

import com.example.kidhackbackend.users.device.entity.DeviceEvent;
import com.example.kidhackbackend.users.device.entity.DeviceEventType;
import com.example.kidhackbackend.users.device.repository.DeviceEventRepository;
import com.example.kidhackbackend.users.device.service.DeviceEventService;
import com.example.kidhackbackend.global.notification.entity.NotificationType;
import com.example.kidhackbackend.global.notification.service.NotificationPort;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({DeviceEventService.class, DeviceEventServiceTest.TestConfig.class})
class DeviceEventServiceTest {

    static class RecordingPort implements NotificationPort {
        final List<NotificationType> published = new ArrayList<>();
        final List<Long> recipients = new ArrayList<>();
        final List<Long> children = new ArrayList<>();

        @Override
        public void publish(Long recipientId, NotificationType type, String message) {
            publish(recipientId, null, type, message);
        }

        @Override
        public void publish(Long recipientId, Long childId, NotificationType type, String message) {
            published.add(type);
            recipients.add(recipientId);
            children.add(childId);
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

    /**
     * RecordingPort 는 컨텍스트 싱글턴이라 테스트끼리 기록이 섞인다.
     * (@DataJpaTest 는 DB 만 롤백하고 빈 상태는 되돌리지 않는다.)
     */
    @BeforeEach
    void clearRecorder() {
        recordingPort.published.clear();
        recordingPort.recipients.clear();
        recordingPort.children.clear();
    }


    @Autowired MemberRepository memberRepository;

    /** 부모에 연결된 자녀를 만든다. 알림 수신자가 부모라 관계가 있어야 발행된다. */
    private Member pairedChild() {
        Member parent = memberRepository.save(member("parent01", Role.PARENT, null));
        return memberRepository.save(member("child01", Role.CHILD, parent));
    }

    private Member member(String loginId, Role role, Member parent) {
        Member m = new Member();
        m.setLoginId(loginId);
        m.setUsername(loginId);
        m.setNickname(loginId);
        m.setPassword("pw");
        m.setEmail(loginId + "@test.com");
        m.setPhoneNumber("01000000000");
        m.setBirthDate(LocalDate.of(2014, 1, 1));
        m.setRole(role);
        m.setParent(parent);
        return m;
    }


    @Test
    void 전원꺼짐_보고시_기록되고_부모에게_트래킹불가_알림이_발행된다() {
        Member child = pairedChild();

        deviceEventService.report(child.getId(), "device-1", DeviceEventType.POWER_OFF);

        assertThat(repository.findByChildIdOrderByCreatedAtDesc(child.getId()))
                .hasSize(1)
                .first()
                .extracting(DeviceEvent::getDeviceId)
                .isEqualTo("device-1");
        assertThat(recordingPort.published).containsExactly(NotificationType.DEVICE_POWER_OFF);
        // 수신자는 부모, 대상 자녀는 childId 로 남는다.
        assertThat(recordingPort.recipients).containsExactly(child.getParent().getId());
        assertThat(recordingPort.children).containsExactly(child.getId());
    }

    @Test
    void 부모와_연결되지_않은_자녀는_알림을_보내지_않는다() {
        Member orphan = memberRepository.save(member("child02", Role.CHILD, null));

        deviceEventService.report(orphan.getId(), "device-2", DeviceEventType.GPS_OFF);

        assertThat(repository.findByChildIdOrderByCreatedAtDesc(orphan.getId())).hasSize(1);
        assertThat(recordingPort.published).isEmpty();
    }
}
