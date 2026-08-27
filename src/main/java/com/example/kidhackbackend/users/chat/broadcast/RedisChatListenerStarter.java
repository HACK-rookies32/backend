package com.example.kidhackbackend.users.chat.broadcast;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 채팅 채널 구독을 기동 이후에 시작하고, 실패하면 계속 다시 시도한다.
 *
 * 컨테이너를 lifecycle 에 맡기면 Redis 에 못 붙었을 때 앱이 아예 뜨지 않는다.
 * 여기서 시작을 떼어내 두면 Redis 가 없어도 앱은 뜨고, 채팅만 같은 인스턴스 안에서만
 * 동작하는 상태로 degrade 한다 (RedisChatBroadcaster 의 폴백).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.chat.broker", havingValue = "redis")
public class RedisChatListenerStarter {

    private static final long RETRY_INTERVAL_MS = 30_000;

    private final RedisMessageListenerContainer container;

    /**
     * 컨테이너의 running 플래그를 신뢰하지 않는다. start() 가 도중에 실패해도 플래그가
     * 켜진 채 남을 수 있고, 그러면 이후 재시도가 조용히 무시된다.
     */
    private final AtomicBoolean subscribed = new AtomicBoolean(false);

    // 이름으로 특정한다. Spring Boot 가 자동설정으로 만드는 redisMessageListenerContainer
    // 가 따로 있어서 타입만으로는 후보가 둘이 된다. 그쪽은 리스너가 비어 있어 구독을
    // 시도하지 않으므로 건드리지 않는다.
    public RedisChatListenerStarter(
            @Qualifier("chatMessageListenerContainer") RedisMessageListenerContainer container) {
        this.container = container;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startWhenReady() {
        tryStart();
    }

    /** Redis 가 앱보다 늦게 올라오는 경우를 위해 붙을 때까지 두드린다. */
    @Scheduled(fixedDelay = RETRY_INTERVAL_MS)
    public void retryUntilSubscribed() {
        if (!subscribed.get()) {
            tryStart();
        }
    }

    private void tryStart() {
        try {
            container.start();
            subscribed.set(true);
            log.info("채팅 Redis 구독 시작됨 (channel={})", RedisChatBroadcaster.CHANNEL);
        } catch (RuntimeException e) {
            subscribed.set(false);
            // 실패한 컨테이너를 그대로 두면 다음 start() 가 이미 실행 중으로 보고 그냥
            // 반환해 버린다. 상태를 되돌려야 재시도가 실제로 다시 붙는다.
            try {
                container.stop();
            } catch (RuntimeException ignored) {
                // 시작도 못 한 컨테이너라 정지에서 나는 예외는 의미가 없다.
            }
            log.warn("채팅 Redis 구독 실패. 인스턴스 간 실시간 전달이 되지 않는 상태이며 "
                            + "같은 인스턴스에 붙은 사용자끼리만 동작한다. {}ms 뒤 재시도. 원인={}",
                    RETRY_INTERVAL_MS, e.getMessage());
        }
    }
}
