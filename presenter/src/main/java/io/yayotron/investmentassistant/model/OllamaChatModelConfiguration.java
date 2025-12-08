package io.yayotron.investmentassistant.model;

import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Configuration
public class OllamaChatModelConfiguration {

    private final String ollamaHost;
    private final String ollamaModel;
    private final String apiKey;

    public OllamaChatModelConfiguration(
            @Value("${ai.host}") String ollamaHost,
            @Value("${ai.api.key}") String apiKey,
            @Value("${ai.model.name}") String ollamaModel) {
        this.ollamaHost = ollamaHost;
        this.ollamaModel = ollamaModel;
        this.apiKey = apiKey;
    }

    @Bean
    public OllamaChatModel model(List<ChatModelListener> listeners) {
        return OllamaChatModel.builder()
                .customHeaders(Map.of("Authorization", "Bearer " + apiKey))
                .baseUrl(ollamaHost)
                .modelName(ollamaModel)
                .timeout(Duration.of(5, ChronoUnit.MINUTES))
                .logRequests(true)
                .logResponses(true)
                .listeners(listeners)
                .build();
    }
}