package com.studyagent.controller;

import com.studyagent.dto.ChatRequest;
import com.studyagent.dto.ChatResponse;
import com.studyagent.service.OllamaService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

@Component
public class AiWebSocketHandler extends TextWebSocketHandler {

    private final OllamaService ollamaService;
    private final ObjectMapper objectMapper;

    public AiWebSocketHandler(
            OllamaService ollamaService,
            ObjectMapper objectMapper
    ) {
        this.ollamaService = ollamaService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(
            WebSocketSession session
    ) {

        System.out.println(
                "AI WebSocket connected: "
                        + session.getId()
        );
    }

    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message
    ) {

        try {

            ChatRequest request =
                    objectMapper.readValue(
                            message.getPayload(),
                            ChatRequest.class
                    );

            if (request.getMessage() == null ||
                    request.getMessage().isBlank()) {

                sendError(
                        session,
                        "Message cannot be empty"
                );

                return;
            }

            Thread.startVirtualThread(() -> {

                ollamaService.generateResponse(

                        request.getMessage(),

                        // Ollama response chunks
                        chunk -> {

                            sendChunk(
                                    session,
                                    chunk
                            );
                        },

                        // Explicit Throwable type
                        (Throwable error) -> {

                            String errorMessage =
                                    error.getMessage();

                            if (errorMessage == null ||
                                    errorMessage.isBlank()) {

                                errorMessage =
                                        "Unknown Ollama error";
                            }

                            sendError(
                                    session,
                                    errorMessage
                            );
                        },

                        // Ollama completed
                        () -> {

                            sendDone(session);
                        }
                );
            });

        } catch (Exception e) {

            sendError(
                    session,
                    "Invalid request: "
                            + e.getMessage()
            );
        }
    }

    private void sendChunk(
            WebSocketSession session,
            String content
    ) {

        try {

            if (!session.isOpen()) {
                return;
            }

            ChatResponse response =
                    new ChatResponse(
                            "chunk",
                            content
                    );

            String json =
                    objectMapper.writeValueAsString(
                            response
                    );

            synchronized (session) {

                session.sendMessage(
                        new TextMessage(json)
                );
            }

        } catch (Exception e) {

            System.err.println(
                    "Failed to send chunk: "
                            + e.getMessage()
            );
        }
    }

    private void sendDone(
            WebSocketSession session
    ) {

        try {

            if (!session.isOpen()) {
                return;
            }

            ChatResponse response =
                    new ChatResponse(
                            "done",
                            ""
                    );

            String json =
                    objectMapper.writeValueAsString(
                            response
                    );

            synchronized (session) {

                session.sendMessage(
                        new TextMessage(json)
                );
            }

        } catch (Exception e) {

            System.err.println(
                    "Failed to send done message: "
                            + e.getMessage()
            );
        }
    }

    private void sendError(
            WebSocketSession session,
            String error
    ) {

        try {

            if (!session.isOpen()) {
                return;
            }

            ChatResponse response =
                    new ChatResponse(
                            "error",
                            error
                    );

            String json =
                    objectMapper.writeValueAsString(
                            response
                    );

            synchronized (session) {

                session.sendMessage(
                        new TextMessage(json)
                );
            }

        } catch (Exception e) {

            System.err.println(
                    "Failed to send error: "
                            + e.getMessage()
            );
        }
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status
    ) {

        System.out.println(
                "AI WebSocket disconnected: "
                        + session.getId()
        );
    }
}