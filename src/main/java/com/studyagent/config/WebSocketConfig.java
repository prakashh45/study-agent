package com.studyagent.config;

import com.studyagent.controller.AiWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final AiWebSocketHandler aiWebSocketHandler;

    public WebSocketConfig(AiWebSocketHandler aiWebSocketHandler) {
        this.aiWebSocketHandler = aiWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(
            WebSocketHandlerRegistry registry
    ) {

        registry.addHandler(
                aiWebSocketHandler,
                "/ws/ai"
        ).setAllowedOrigins("*");

        System.out.println("WebSocket endpoint registered: /ws/ai");
    }
}