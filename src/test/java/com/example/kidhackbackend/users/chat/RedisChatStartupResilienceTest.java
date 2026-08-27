package com.example.kidhackbackend.users.chat;

import com.example.kidhackbackend.users.chat.broadcast.ChatBroadcastPort;
import com.example.kidhackbackend.users.chat.broadcast.RedisChatBroadcaster;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 운영 설정(broker=redis)에서 Redis 에 붙지 못하더라도 앱은 기동해야 한다.
 *
 * 리스너 컨테이너를 그대로 lifecycle 에 맡기면 여기서 ApplicationContextException 이 나고,
 * 채팅 하나 때문에 위치추적·결제·알림까지 전부 죽는다. CI 와 개발 PC 에는 Redis 가 떠 있지
 * 않으므로 이 테스트가 자연스럽게 그 경로를 탄다.
 */
@SpringBootTest(properties = "app.chat.broker=redis")
class RedisChatStartupResilienceTest {

    @Autowired
    ApplicationContext ctx;

    @Test
    void Redis_가_없어도_앱은_기동한다() {
        assertThat(ctx.getBeanNamesForType(ChatBroadcastPort.class)).hasSize(1);
        assertThat(ctx.getBean(ChatBroadcastPort.class)).isInstanceOf(RedisChatBroadcaster.class);
    }
}
