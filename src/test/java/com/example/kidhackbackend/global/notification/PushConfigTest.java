package com.example.kidhackbackend.global.notification;

import com.example.kidhackbackend.global.notification.push.FirebasePushSender;
import com.example.kidhackbackend.global.notification.push.MockPushSender;
import com.example.kidhackbackend.global.notification.push.PushConfig;
import com.example.kidhackbackend.global.notification.push.PushPort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * "언제 무엇이 뜨는가" 를 고정한다.
 *
 * <p>예전에는 이 판단이 세 클래스의 {@code @ConditionalOnProperty} 에 흩어져 있었고,
 * 키를 못 읽어도 warn 만 남겨서 <b>운영에서 푸시가 조용히 죽어도 아무도 몰랐다.</b>
 * 특히 마지막 케이스(운영 + 키 없음 = 기동 거부)가 이번 변경의 핵심이다.
 */
class PushConfigTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(PushConfig.class);

    @Test
    void provider_가_mock_이면_실제로_보내지_않는_구현체가_뜬다() {
        contextRunner
                .withPropertyValues("app.push.provider=mock")
                .run(context -> assertThat(context.getBean(PushPort.class))
                        .isInstanceOf(MockPushSender.class));
    }

    @Test
    void provider_설정이_아예_없으면_mock_이_기본값이다() {
        contextRunner.run(context -> assertThat(context.getBean(PushPort.class))
                .isInstanceOf(MockPushSender.class));
    }

    @Test
    void 로컬은_키를_못_읽어도_mock_으로_폴백해서_앱이_뜬다() {
        contextRunner
                .withPropertyValues(
                        "app.push.provider=firebase",
                        "firebase.credentials-path=classpath:no-such-service-account.json")
                .run(context -> {
                    // 팀원 전원이 서비스 계정 키를 들고 있어야 개발이 되는 상황을 만들지 않는다.
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(PushPort.class)).isInstanceOf(MockPushSender.class);
                    assertThat(context.getBean(PushPort.class)).isNotInstanceOf(FirebasePushSender.class);
                });
    }

    @Test
    void 운영에서_키를_못_읽으면_기동을_거부한다() {
        contextRunner
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("prod"))
                .withPropertyValues(
                        "app.push.provider=firebase",
                        "firebase.credentials-path=file:/run/secrets/no-such-firebase.json")
                .run(context -> {
                    // 푸시가 죽은 채로 배포가 끝나는 것보다 컨테이너가 안 뜨는 편이 빨리 발견된다.
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("운영에서 Firebase 초기화에 실패");
                });
    }

    @Test
    void 운영이라도_provider_가_mock_이면_키를_보지_않는다() {
        contextRunner
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("prod"))
                .withPropertyValues("app.push.provider=mock")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(PushPort.class)).isInstanceOf(MockPushSender.class);
                });
    }
}