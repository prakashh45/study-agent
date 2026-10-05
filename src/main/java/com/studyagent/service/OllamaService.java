package com.studyagent.service;

import com.google.genai.Client;
import com.google.genai.ResponseStream;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
public class OllamaService {

    private final Client client;

    public OllamaService() {
        String apiKey = System.getenv("GEMINI_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY environment variable is not configured"
            );
        }

        this.client = Client.builder()
                .apiKey(apiKey)
                .build();
    }

    public void generateResponse(
            String message,
            Consumer<String> onChunk,
            Consumer<Throwable> onError,
            Runnable onComplete
    ) {

        try {

            String systemPrompt = """
                    You are StudyAgent AI, a helpful study assistant.

                    Your job is to help students understand concepts clearly.

                    Rules:
                    - Explain concepts in simple language.
                    - Give real-world examples.
                    - For programming questions, provide correct code.
                    - Use headings and bullet points when useful.
                    - Give detailed answers when the question requires it.
                    - Do not unnecessarily shorten the answer.
                    - Help the student learn rather than simply giving an answer.
                    """;

            Content systemInstruction =
                    Content.fromParts(
                            Part.fromText(systemPrompt)
                    );

            GenerateContentConfig config =
                    GenerateContentConfig.builder()
                            .systemInstruction(systemInstruction)
                            .temperature(0.7f)
                            .maxOutputTokens(4000)
                            .build();

            try (ResponseStream<GenerateContentResponse> responseStream =
                         client.models.generateContentStream(
                                 "gemini-3.6-flash",
                                 message,
                                 config
                         )) {

                for (GenerateContentResponse response : responseStream) {

                    String text = response.text();

                    if (text != null && !text.isEmpty()) {
                        onChunk.accept(text);
                    }
                }
            }

            onComplete.run();

        } catch (Throwable e) {
            onError.accept(e);
        }
    }
}