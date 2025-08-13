package io.yayotron.investmentassistant.model;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.rag.RetrievalAugmentor;
import io.yayotron.investmentassistant.feeder.alphavantage.AlphaVantageService;
import io.yayotron.investmentassistant.feeder.financials.FinancialsService;
import io.yayotron.investmentassistant.feeder.fmp.FmpService;
import io.yayotron.investmentassistant.feeder.sector.SectorPerformanceService;
import io.yayotron.investmentassistant.presenter.tool.FinancialAnalysisTool;
import io.yayotron.investmentassistant.presenter.tool.StockPriceTool;
import io.yayotron.investmentassistant.prompt.InvestmentAnalystPromptEnricher;
import io.yayotron.investmentassistant.prompt.SystemPromptEnricher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
public class OllamaRAGToolsIntegrationTest {

    @MockitoBean
    private OllamaChatModel ollamaChatModel; // Mock the LLM

    // Mock all underlying feeder services
    @MockitoBean
    private AlphaVantageService alphaVantageService;
    @MockitoBean private FinancialsService financialsService;
    @MockitoBean private SectorPerformanceService sectorPerformanceService;
    @MockitoBean private FmpService fmpService;

    @MockitoBean private RetrievalAugmentor retrievalAugmentor; // Mock as it's part of OllamaRAG constructor

    @Autowired private OllamaRAG ollamaRAG; // The system under test

    // TestConfig to provide necessary beans and import tools
    @Configuration
    @Import({OllamaRAG.class, StockPriceTool.class, FinancialAnalysisTool.class, InvestmentAnalystPromptEnricher.class})
    static class TestConfig {
        @Bean
        public List<SystemPromptEnricher> systemPromptEnrichers(InvestmentAnalystPromptEnricher enricher) {
            return Collections.singletonList(enricher);
        }
    }

    private void setupMockLLMResponse(String llmTextResponse) {
        // This mock simulates the LLM generating a simple text response.
        // For actual tool usage, the LLM would generate a structured response
        // indicating a tool call. AiServices intercepts this. For these tests,
        // we verify the *effect* of that interception (i.e., service method calls).
        when(ollamaChatModel.chat(anyList()))
            .thenReturn(ChatResponse.builder()
                    .aiMessage(AiMessage.from(llmTextResponse))
                    .build());
    }
    
    private void setupMockLLMForToolUse() {
        // This is a simplified mock. In reality, LangChain4j's AiServices
        // expects a specific AiMessage structure for tool execution.
        // However, by verifying the *service* calls, we confirm the tool was invoked.
        // The important part is that the LLM doesn't just answer, but triggers the tool.
        // For this test, we'll often make the LLM's response less important than verifying the mock service call.
        // A more advanced mock might return a `ToolExecutionRequest` if we wanted to test that part of Langchain4j.
        // For now, the `Assistant` interface abstracts this, and we test the side-effects (service calls).
        when(ollamaChatModel.chat(anyList()))
            .thenAnswer(invocation -> ChatResponse.builder()
                    .aiMessage(AiMessage.from("LLM processed tool response."))
                    .build());
    }


    @Test
    void whenAskingForStockPrice_thenAlphaVantageServiceIsCalled() {
        String symbol = "MSFT";
        String question = "What is the price of " + symbol + "?";
        Map<String, String> mockStockData = Map.of("symbol", symbol, "price", "450.00");
        
        when(alphaVantageService.getStockData(symbol)).thenReturn(mockStockData);
        setupMockLLMForToolUse(); // LLM is expected to use a tool

        ollamaRAG.askOllama(question);

        verify(alphaVantageService, times(1)).getStockData(symbol);
    }

    @Test
    void whenAskingForFinancialOverview_thenFinancialsServiceIsCalled() {
        String symbol = "AAPL";
        String question = "Tell me about the financials of " + symbol + ".";
        Map<String, String> mockOverviewData = Map.of("Symbol", symbol, "MarketCapitalization", "2.5T");

        when(financialsService.getCompanyOverview(symbol)).thenReturn(mockOverviewData);
        setupMockLLMForToolUse();

        ollamaRAG.askOllama(question);

        verify(financialsService, times(1)).getCompanyOverview(symbol);
    }

    @Test
    void whenAskingForEarnings_thenFinancialsServiceIsCalled() {
        String symbol = "GOOG";
        String question = "What are the recent earnings for " + symbol + "?";
        Map<String, Object> mockEarningsData = Map.of("symbol", symbol, "annualEarnings", Collections.emptyList());
        
        when(financialsService.getEarningsData(symbol)).thenReturn(mockEarningsData);
        setupMockLLMForToolUse();

        ollamaRAG.askOllama(question);

        verify(financialsService, times(1)).getEarningsData(symbol);
    }

    @Test
    void whenAskingForSectorPerformance_thenSectorPerformanceServiceIsCalled() {
        String question = "How are the market sectors performing?";
        Map<String, Map<String, String>> mockSectorData = Map.of("Real-Time Performance", Map.of("Technology", "1.0%"));
        
        when(sectorPerformanceService.getSectorPerformance()).thenReturn(mockSectorData);
        setupMockLLMForToolUse();

        ollamaRAG.askOllama(question);

        verify(sectorPerformanceService, times(1)).getSectorPerformance();
    }

    @Test
    void whenAskingForSectorPE_thenFmpServiceIsCalled() {
        String sector = "Technology";
        String exchange = "NASDAQ";
        String question = "What's the average P/E for the " + sector + " sector in " + exchange + "?";
        Map<String, Object> mockSectorPEData = Map.of(sector, 25.5);
        
        when(fmpService.getSectorPERatios(exchange)).thenReturn(mockSectorPEData);
        setupMockLLMForToolUse();

        ollamaRAG.askOllama(question);

        verify(fmpService, times(1)).getSectorPERatios(exchange);
    }

    @Test
    void whenAskingForIndustryPE_thenFmpServiceIsCalled() {
        String industry = "Software";
        String exchange = "NYSE";
        String question = "Compare P/E for the " + industry + " industry on " + exchange + ".";
        Map<String, Object> mockIndustryPEData = Map.of(industry, 30.0);

        when(fmpService.getIndustryPERatios(exchange)).thenReturn(mockIndustryPEData);
        setupMockLLMForToolUse();
        
        ollamaRAG.askOllama(question);

        verify(fmpService, times(1)).getIndustryPERatios(exchange);
    }
}
