package com.example.kidhackbackend.users.chat.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 접속 경로는 /ws-chat 유지 (nginx/ALB 프록시 규칙이 이 경로에 걸려 있어 바꾸지 않는다).
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*") // [VULN-CHAT-3] CORS 전체 허용
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 구독 prefix /sub, 발행 prefix /pub (관례). 구독 /sub/chat/{roomId}, 발행 /pub/chat/{roomId}.
        // 이 값들은 STOMP 프레임 헤더라 리버스 프록시와 무관하다.
        registry.enableSimpleBroker("/sub");
        registry.setApplicationDestinationPrefixes("/pub");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}