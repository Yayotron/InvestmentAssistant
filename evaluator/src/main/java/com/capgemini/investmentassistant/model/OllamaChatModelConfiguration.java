package com.capgemini.investmentassistant.model;

import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Configuration
public class OllamaChatModelConfiguration {

    private final String ollamaHost;
    private final String ollamaModel;

    public OllamaChatModelConfiguration(
            @Value("${ai.host}") String ollamaHost,
            @Value("${ai.model.name}") String ollamaModel) {
        this.ollamaHost = ollamaHost;
        this.ollamaModel = ollamaModel;
    }

    @Bean
    public OllamaChatModel model() {
        return OllamaChatModel.builder()
                .baseUrl(ollamaHost)
                .modelName(ollamaModel)
                .timeout(Duration.of(5, ChronoUnit.MINUTES))
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}