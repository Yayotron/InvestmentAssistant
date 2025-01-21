package com.capgemini.investmentassistant.model;

import com.capgemini.investmentassistant.prompt.SystemPromptEnricher;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OllamaRAG {

    private static final Logger logger = LoggerFactory.getLogger(OllamaRAG.class);

    private final OllamaChatModel ollamaChatModel;
    private final List<SystemPromptEnricher> systemPrompts;
    private final RetrievalAugmentor retrievalAugmentor;


    public OllamaRAG(OllamaChatModel ollamaChatModel,
                     List<SystemPromptEnricher> systemPrompts,
                     RetrievalAugmentor retrievalAugmentor) {
        this.ollamaChatModel = ollamaChatModel;
        this.systemPrompts = systemPrompts;
        this.retrievalAugmentor = retrievalAugmentor;
    }

    public String askOllama(String question) {
        logger.info("Asking Ollama {}...", question);

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(ollamaChatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .retrievalAugmentor(retrievalAugmentor)
                .systemMessageProvider(o -> systemPrompts.stream()
                        .map(promptEnricher -> promptEnricher.enrichPrompt(o.toString()))
                        .collect(Collectors.joining("\n")))
                .build();

        return assistant.answer(question);
    }
}
