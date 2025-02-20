package io.yayotron.investmentassistant.model;

import dev.langchain4j.model.ollama.OllamaLanguageModel;
import io.yayotron.investmentassistant.prompt.FormatPromptEnricher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OllamaSinglePrompt {

    private final String ollamaHost;
    private final String ollamaModel;
    private final FormatPromptEnricher formatPromptEnricher;


    public OllamaSinglePrompt(@Value("${ai.host}") String ollamaHost,
                              @Value("${ai.model.name}") String ollamaModel,
                              FormatPromptEnricher formatPromptEnricher) {
        this.ollamaHost = ollamaHost;
        this.ollamaModel = ollamaModel;
        this.formatPromptEnricher = formatPromptEnricher;
    }

    public String askOllama(String question) {
        OllamaLanguageModel model = OllamaLanguageModel.builder()
                .baseUrl(ollamaHost)
                .modelName(ollamaModel)
                .format("json")
                .build();

        return model.generate(formatPromptEnricher.enrichPrompt(question)).content();
    }

}
