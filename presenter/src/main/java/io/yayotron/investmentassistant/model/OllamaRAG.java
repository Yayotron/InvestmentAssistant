package io.yayotron.investmentassistant.model;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.moderation.DisabledModerationModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.service.AiServices;
import io.yayotron.investmentassistant.prompt.SystemPromptEnricher;
import io.yayotron.investmentassistant.presenter.tool.CompanyHealthTool;
import io.yayotron.investmentassistant.presenter.tool.FinancialAnalysisTool;
import io.yayotron.investmentassistant.presenter.tool.StockPriceTool;
import org.springframework.stereotype.Component;

import java.util.Arrays; // Import for List.of equivalent if needed, or just use Arrays.asList
import java.util.List;
import java.util.stream.Collectors;

@Component
public class OllamaRAG {

    private final Assistant assistant;

    public OllamaRAG(OllamaChatModel ollamaChatModel,
                     List<SystemPromptEnricher> systemPrompts,
                     RetrievalAugmentor retrievalAugmentor,
                     StockPriceTool stockPriceTool,
                     FinancialAnalysisTool financialAnalysisTool,
                     CompanyHealthTool companyHealthTool) { // New tool parameter
        this.assistant = AiServices.builder(Assistant.class)
                .chatModel(ollamaChatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(500))
                .tools(Arrays.asList(stockPriceTool, financialAnalysisTool, companyHealthTool)) // Include all three tools
                .moderationModel(new DisabledModerationModel())
                .retrievalAugmentor(retrievalAugmentor)
                .systemMessageProvider(o -> systemPrompts.stream()
                        .map(promptEnricher -> promptEnricher.enrichPrompt(o.toString()))
                        .collect(Collectors.joining("\n")))
                .tools(List.of(stockPriceTool, financialAnalysisTool, companyHealthTool))
                .build();
    }

    public String askOllama(String question) {
        return assistant.answer(question);
    }
}