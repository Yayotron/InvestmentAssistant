package io.yayotron.investmentassistant.model;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.moderation.DisabledModerationModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.service.AiServices;
import io.yayotron.investmentassistant.prompt.SystemPromptEnricher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OllamaRAG {

    private final Assistant assistant;

    public OllamaRAG(OllamaChatModel ollamaChatModel,
                     List<SystemPromptEnricher> systemPrompts,
                     RetrievalAugmentor retrievalAugmentor) {
        this.assistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(ollamaChatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(500))
                .moderationModel(new DisabledModerationModel())
                .retrievalAugmentor(retrievalAugmentor)
                .systemMessageProvider(o -> systemPrompts.stream()
                        .map(promptEnricher -> promptEnricher.enrichPrompt(o.toString()))
                        .collect(Collectors.joining("\n")))
                .build();
    }

    public String askOllama(String question) {
        return assistant.answer(question);
    }
}