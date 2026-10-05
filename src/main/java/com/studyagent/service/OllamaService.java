package com.studyagent.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class OllamaService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OllamaService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:11434")
                .build();
    }

    public void generateResponse(
            String message,
            Consumer<String> onChunk,
            Consumer<Throwable> onError,
            Runnable onComplete
    ) {
        try {
            Map<String, Object> request = Map.of(
                    "model", "qwen2.5-coder:7b",
                    "prompt", message,
                    "stream", true,
                    "options", Map.of(
                            "num_predict", 4000,
                            "temperature", 0.7
                    )
            );

            restClient.post()
                    .uri("/api/generate")
                    .body(request)
                    .exchange((req, response) -> {

                        if (response.getStatusCode().isError()) {
                            throw new RuntimeException(
                                    "Ollama HTTP error: "
                                            + response.getStatusCode()
                            );
                        }

                        try (BufferedReader reader =
                                     new BufferedReader(
                                             new InputStreamReader(
                                                     response.getBody(),
                                                     StandardCharsets.UTF_8
                                             )
                                     )) {

                            String line;

                            while ((line = reader.readLine()) != null) {

                                if (line.isBlank()) {
                                    continue;
                                }

                                JsonNode json =
                                        objectMapper.readTree(line);

                                JsonNode textNode =
                                        json.get("response");

                                if (textNode != null) {
                                    String chunk =
                                            textNode.asText();

                                    if (!chunk.isEmpty()) {
                                        onChunk.accept(chunk);
                                    }
                                }

                                JsonNode errorNode =
                                        json.get("error");

                                if (errorNode != null) {
                                    throw new RuntimeException(
                                            errorNode.asText()
                                    );
                                }
                            }
                        }

                        return null;
                    });

            onComplete.run();

        } catch (Exception e) {
            onError.accept(e);
        }
    }
}