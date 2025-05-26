package io.yayotron.investmentassistant.prompt;

import io.yayotron.investmentassistant.PresenterApplication;
import io.yayotron.investmentassistant.feeder.alphavantage.AlphaVantageService;
import io.yayotron.investmentassistant.feeder.financials.FinancialsService;
import io.yayotron.investmentassistant.feeder.fmp.FmpService;
import io.yayotron.investmentassistant.feeder.sector.SectorPerformanceService;
import io.yayotron.investmentassistant.model.OllamaRAG;
import io.yayotron.investmentassistant.presenter.ChatConsole;
import io.yayotron.investmentassistant.presenter.health.CompanyHealthScorecardService;

import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.data.message.AiMessage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = PresenterApplication.class) // Ensure correct ApplicationContext is loaded
@ContextConfiguration(classes = {ChatConsole.class, OllamaRAG.class}) // Explicitly include beans needed for ChatConsole if not covered by PresenterApplication scan
public class ChatConsoleE2ETest {

    @Autowired
    private ChatConsole chatConsole;

    @MockBean
    private OllamaChatModel ollamaChatModel; // Mock the LLM

    // Mock all underlying data services
    @MockBean private AlphaVantageService alphaVantageService;
    @MockBean private FinancialsService financialsService;
    @MockBean private SectorPerformanceService sectorPerformanceService;
    @MockBean private FmpService fmpService;
    // CompanyHealthScorecardService is a presenter-layer service, but it uses feeder services.
    // If ChatConsole directly or indirectly depends on it, and its behavior needs to be controlled,
    // it might also need mocking or careful setup of its dependencies (financialsService).
    // For now, we assume its logic will run based on the mocked financialsService.
    @MockBean private CompanyHealthScorecardService companyHealthScorecardService;


    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final InputStream originalIn = System.in;
    private ExecutorService executorService;

    @BeforeEach
    public void setUpStreamsAndExecutor() {
        System.setOut(new PrintStream(outContent));
        executorService = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    public void restoreStreamsAndExecutor() {
        System.setOut(originalOut);
        System.setIn(originalIn);
        executorService.shutdownNow();
    }

    private Future<?> runChatConsoleWithInput(String input) {
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        return executorService.submit(() -> {
            try {
                chatConsole.run();
            } catch (Exception e) {
                // Catch exceptions like NoSuchElementException if System.in closes prematurely in test
                if (!(e instanceof java.util.NoSuchElementException)) {
                    e.printStackTrace();
                }
            }
        });
    }

    // --- Scenario 1: Full Fundamental Analysis ---
    @Test
    @Timeout(10) // Add timeout to prevent test hanging
    void testFullFundamentalAnalysis() throws Exception {
        String symbol = "MSFT";
        String userInput = "Can you give me a fundamental analysis of MSFT?\nexit\n";

        // 1. Mock Service Data
        when(financialsService.getCompanyOverview(symbol)).thenReturn(
            Map.of("Symbol", symbol, "MarketCapitalization", "2.8T", "PERatio", "35", "EPS", "10.0", "DividendYield", "0.01", "Beta", "0.9", "DebtToEquity", "0.5", "ProfitMargin", "0.3", "QuarterlyRevenueGrowthYOY", "0.15")
        );
        when(financialsService.getEarningsData(symbol)).thenReturn(
            Map.of("symbol", symbol, 
                   "annualEarnings", List.of(Map.of("fiscalDateEnding", "2023-06-30", "reportedEPS", "9.80")),
                   "quarterlyEarnings", List.of(Map.of("fiscalDateEnding", "2023-09-30", "reportedEPS", "2.50")))
        );
        when(alphaVantageService.getStockData(symbol)).thenReturn(
            Map.of("symbol", symbol, "price", "400.00", "volume", "20M", "latest trading day", "2024-03-20")
        );
        // Mocking CompanyHealthScorecardService to return a controlled ScorecardResult
        CompanyHealthScorecardService.ScorecardResult mockScorecard = new CompanyHealthScorecardService.ScorecardResult(
            75.0, 
            List.of("Profit Margin: 30.00% (Excellent)", "Quarterly Revenue Growth (YOY): 15.00% (Strong)"), 
            List.of("P/E Ratio: 35 (Potentially high)"), 
            "Overall financial health: Strong."
        );
        when(companyHealthScorecardService.calculateHealthScore(symbol)).thenReturn(mockScorecard);
        
        // For P/E comparison part of the prompt
        when(fmpService.getSectorPERatios(anyString())).thenReturn(Map.of("Technology", 28.0, "Software", 30.0));
        when(fmpService.getIndustryPERatios(anyString())).thenReturn(Map.of("Software - Infrastructure", 32.0));


        // 2. Mock LLM Behavior (Simplified: LLM returns a synthesized response)
        // This response should be what we expect the LLM to say *after* calling all tools.
        String expectedLLMResponse = String.join("\n",
            "Okay, here's a fundamental analysis for MSFT:",
            "Overall Summary & Health: Overall financial health: Strong. Score: 75.0/100",
            "Key Positives:",
            "- Profit Margin: 30.00% (Excellent)",
            "- Quarterly Revenue Growth (YOY): 15.00% (Strong)",
            "Key Negatives:",
            "- P/E Ratio: 35 (Potentially high)",
            "Key Financial Metrics: MSFT has a Market Cap of 2.8T, P/E Ratio of 35, and EPS of 10.0. Dividend Yield is 1.00%. Beta is 0.9. Debt to Equity is 0.5. Profit Margin is 30.00%. Quarterly Revenue Growth YOY is 15.00%. Current price: $400.00.",
            "Earnings Performance: Recent annual EPS (2023-06-30) was $9.80. Recent quarterly EPS (2023-09-30) was $2.50.",
            "Strengths: Strong profit margin and revenue growth.",
            "Weaknesses/Risks: The P/E ratio of 35 might be considered high by some investors.",
            "Valuation Context: The Technology sector P/E is around 28.0. The Software - Infrastructure industry P/E is around 32.0. MSFT's P/E of 35 is slightly above these averages.",
            "Investment Outlook (Cautious): MSFT shows strong financial health and growth. However, its valuation should be considered in the context of the broader market and its peers.",
            "Disclaimer: I am not a financial advisor. Please consult with a professional before making any investment decisions."
        );
        when(ollamaChatModel.generate(anyList())).thenReturn(Response.from(AiMessage.from(expectedLLMResponse)));

        // 3. Run ChatConsole
        Future<?> consoleRunFuture = runChatConsoleWithInput(userInput);
        consoleRunFuture.get(5, TimeUnit.SECONDS); // Wait for console to process "exit"

        // 4. Assertions
        String output = outContent.toString();
        System.out.println("Captured Output:\n" + output); // For debugging during test development

        assertTrue(output.contains("Overall financial health: Strong."), "Missing Scorecard Summary");
        assertTrue(output.contains("Score: 75.0/100"), "Missing Scorecard Score");
        assertTrue(output.contains("Profit Margin: 30.00% (Excellent)"), "Missing Key Positive");
        assertTrue(output.contains("P/E Ratio: 35 (Potentially high)"), "Missing Key Negative");
        assertTrue(output.contains("Market Cap of 2.8T"), "Missing Market Cap from Overview");
        assertTrue(output.contains("Current price: $400.00"), "Missing Stock Price");
        assertTrue(output.contains("Recent annual EPS (2023-06-30) was $9.80"), "Missing Earnings Data");
        assertTrue(output.contains("Technology sector P/E is around 28.0"), "Missing Sector P/E comparison");
        assertTrue(output.contains("Disclaimer: I am not a financial advisor"), "Missing Disclaimer");
    }

    // --- Scenario 4: Error Handling ---
    @Test
    @Timeout(10)
    void testErrorHandlingWhenServiceFails() throws Exception {
        String symbol = "FAIL";
        String userInput = "Analyze " + symbol + "\nexit\n";

        // 1. Mock Service Data (FinancialsService returns an error)
        when(financialsService.getCompanyOverview(symbol))
            .thenReturn(Map.of("error", "Service connection failed", "details", "Timeout connecting to API"));
        
        // Other services might not even be called if the first essential one fails.
        // If health scorecard is called even after overview error, mock it too.
        when(companyHealthScorecardService.calculateHealthScore(symbol)).thenReturn(
            new CompanyHealthScorecardService.ScorecardResult(0, Collections.emptyList(), Collections.emptyList(), "Could not calculate health score for FAIL: Error fetching company overview: Service connection failed (Details: Timeout connecting to API)")
        );


        // 2. Mock LLM Behavior (Simplified: LLM returns a synthesized error message)
        // This is what the LLM should say after attempting to use tools and one fails critically.
        // The prompt instructs the LLM to use tools. If a tool returns an error, the LLM should report that.
        // The FinancialAnalysisTool itself will format the error from the service.
        // So, the LLM should incorporate that formatted error.
        String expectedLLMResponse = "I encountered an error while trying to analyze " + symbol + ". " +
                                     "Could not retrieve financial overview for FAIL. Reason: Service connection failed (Details: Timeout connecting to API)" +
                                     "\nDisclaimer: I am not a financial advisor. Please consult with a professional before making any investment decisions.";

        when(ollamaChatModel.generate(anyList())).thenReturn(Response.from(AiMessage.from(expectedLLMResponse)));
        
        // 3. Run ChatConsole
        Future<?> consoleRunFuture = runChatConsoleWithInput(userInput);
        consoleRunFuture.get(5, TimeUnit.SECONDS);

        // 4. Assertions
        String output = outContent.toString();
        System.out.println("Captured Output (Error Test):\n" + output);

        assertTrue(output.contains("Could not retrieve financial overview for FAIL"), "Missing specific error message part 1.");
        assertTrue(output.contains("Reason: Service connection failed (Details: Timeout connecting to API)"), "Missing specific error message part 2.");
        assertTrue(output.contains("Disclaimer: I am not a financial advisor"), "Missing Disclaimer in error output.");
    }
}
