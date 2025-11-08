package io.yayotron.investmentassistant.model;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class ChatGptModelConfiguration {

    private final String apiKey;
    private final String modelName;
    private final String baseUrl;

    public ChatGptModelConfiguration(
            @Value("${ai.chatgpt.api.key}") String apiKey,
            @Value("${ai.chatgpt.model.name}") String modelName,
            @Value("${ai.chatgpt.base.url:}") String baseUrl) {
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.baseUrl = baseUrl;
    }

    @Bean
    public OpenAiChatModel model() {
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .timeout(Duration.of(5, ChronoUnit.MINUTES))
                .logRequests(true)
                .logResponses(true);

        if (baseUrl != null && !baseUrl.isEmpty()) {
            builder.baseUrl(baseUrl);
        }

        return builder.build();
    }
}