package org.itmo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // Включает обработку сообщений по WebSocket
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Клиенты будут подписываться на темы, начинающиеся с /topic (например, /topic/bands/updates)
        config.enableSimpleBroker("/topic");

        // Префикс для сообщений, которые клиенты будут отправлять на сервер (если нужно)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Регистрируем эндпоинт, к которому подключается клиент (фронтенд)
        // .withSockJS() нужен для поддержки браузеров, не поддерживающих WebSockets напрямую
        registry.addEndpoint("/ws").withSockJS();
    }
}
