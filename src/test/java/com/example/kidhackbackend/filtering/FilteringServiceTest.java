package com.example.kidhackbackend.filtering;

import com.example.kidhackbackend.users.filtering.entity.BlockRule;
import com.example.kidhackbackend.users.filtering.repository.BlockRuleRepository;
import com.example.kidhackbackend.users.filtering.service.BlockRuleService;
import com.example.kidhackbackend.users.filtering.service.FilteringService;
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
@Import({BlockRuleService.class, FilteringService.class, FilteringServiceTest.TestConfig.class})
class FilteringServiceTest {

    /** 알림 발행을 가로채는 테스트용 NotificationPort. */
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

    @Autowired BlockRuleService blockRuleService;
    @Autowired FilteringService filteringService;
    @Autowired BlockRuleRepository repository;
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
    void 블랙리스트에_걸리면_차단되고_부모에게_알림이_발행된다() {
        Member child = pairedChild();
        blockRuleService.addRule(child.getId(), "badsite.com");

        boolean blocked = filteringService.detect(child.getId(), "http://www.BADSITE.com/page");

        assertThat(blocked).isTrue();
        assertThat(recordingPort.published).containsExactly(NotificationType.HARMFUL_SITE);
        // 수신자는 부모, 대상 자녀는 childId 로 남는다.
        assertThat(recordingPort.recipients).containsExactly(child.getParent().getId());
        assertThat(recordingPort.children).containsExactly(child.getId());
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
