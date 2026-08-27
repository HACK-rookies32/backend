package com.example.kidhackbackend.users.chat.config;

import com.example.kidhackbackend.users.chat.broadcast.RedisChatBroadcaster;
import com.example.kidhackbackend.users.chat.broadcast.RedisChatSubscriber;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 이 인스턴스를 채팅 채널 구독자로 등록한다.
 *
 * 컨테이너가 없으면 발행만 되고 아무도 듣지 않으므로 메시지가 조용히 사라진다.
 * 발행부와 같은 조건(app.chat.broker=redis)으로 켜고 꺼야 한쪽만 살아 있는 상태가 안 된다.
 */
@Configuration
@ConditionalOnProperty(name = "app.chat.broker", havingValue = "redis")
public class RedisChatConfig {

    @Bean
    public RedisMessageListenerContainer chatMessageListenerContainer(
            RedisConnectionFactory connectionFactory, RedisChatSubscriber subscriber) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer() {
            @Override
            public boolean isAutoStartup() {
                // 기본값(true)이면 컨테이너가 lifecycle start 단계에서 동기적으로 구독을
                // 시도하고, Redis 에 못 붙으면 그대로 ApplicationContextException 이 되어
                // 앱 전체가 기동하지 못한다. 그러면 채팅 하나 때문에 위치추적·결제·알림까지
                // 같이 죽는다. 시작은 RedisChatListenerStarter 가 기동 이후에 따로 맡는다.
                return false;
            }
        };
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, new ChannelTopic(RedisChatBroadcaster.CHANNEL));
        return container;
    }
}
