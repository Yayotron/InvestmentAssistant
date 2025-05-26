package io.yayotron.investmentassistant.presenter.tool;

import io.yayotron.investmentassistant.feeder.financials.FinancialsService;
import io.yayotron.investmentassistant.feeder.fmp.FmpService;
import io.yayotron.investmentassistant.feeder.sector.SectorPerformanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
public class FinancialAnalysisToolTest {

    @Mock
    private FinancialsService financialsService;

    @Mock
    private SectorPerformanceService sectorPerformanceService;

    @Mock
    private FmpService fmpService;

    @InjectMocks
    private FinancialAnalysisTool financialAnalysisTool;

    private final String TEST_SYMBOL = "TESTC";

    // --- getCompanyFinancialOverview Tests ---
    @Test
    void getCompanyFinancialOverview_success() {
        Map<String, String> overviewData = new LinkedHashMap<>(); // Use LinkedHashMap to ensure order for assertion if needed
        overviewData.put("Symbol", TEST_SYMBOL);
        overviewData.put("MarketCapitalization", "100B");
        overviewData.put("PERatio", "20");
        overviewData.put("EPS", "5.00");
        // Add more metrics as defined in FinancialAnalysisTool.appendMetric calls
        overviewData.put("EBITDA", "10B");
        overviewData.put("RevenuePerShareTTM", "50");
        overviewData.put("GrossProfitTTM", "30B");
        overviewData.put("DilutedEPSTTM", "4.90");
        overviewData.put("QuarterlyEarningsGrowthYOY", "0.1");
        overviewData.put("QuarterlyRevenueGrowthYOY", "0.05");
        overviewData.put("AnalystTargetPrice", "200");
        overviewData.put("TrailingPE", "19");
        overviewData.put("ForwardPE", "18");
        overviewData.put("PriceToSalesRatioTTM", "5");
        overviewData.put("PriceToBookRatio", "4");
        overviewData.put("EVToRevenue", "6");
        overviewData.put("EVToEBITDA", "12");
        overviewData.put("Beta", "1.1");
        overviewData.put("SharesOutstanding", "2B");
        overviewData.put("DividendYield", "0.02");
        overviewData.put("ProfitMargin", "0.25");
        overviewData.put("ReturnOnEquityTTM", "0.15");
        overviewData.put("ReturnOnAssetsTTM", "0.10");
        overviewData.put("DebtToEquity", "0.5");
        overviewData.put("CurrentRatio", "1.5");
        overviewData.put("BookValue", "100");


        when(financialsService.getCompanyOverview(TEST_SYMBOL)).thenReturn(overviewData);

        String result = financialAnalysisTool.getCompanyFinancialOverview(TEST_SYMBOL);

        assertTrue(result.contains("Financial Overview for TESTC"));
        assertTrue(result.contains("Market Cap: 100B"));
        assertTrue(result.contains("P/E Ratio: 20"));
        assertTrue(result.contains("EPS: 5.00"));
        assertTrue(result.contains("EBITDA: 10B"));
        // ... (add more assertions for other metrics)
        assertFalse(result.contains("error"), "Result should not contain 'error' on success.");
    }

    @Test
    void getCompanyFinancialOverview_serviceError() {
        when(financialsService.getCompanyOverview(TEST_SYMBOL))
            .thenReturn(Map.of("error", "API Error", "details", "Limit reached"));

        String result = financialAnalysisTool.getCompanyFinancialOverview(TEST_SYMBOL);

        assertTrue(result.contains("Could not retrieve financial overview for TESTC. Reason: API Error (Details: Limit reached)"));
    }
    
    @Test
    void getCompanyFinancialOverview_serviceErrorNoDetails() {
        when(financialsService.getCompanyOverview(TEST_SYMBOL))
            .thenReturn(Map.of("error", "API Error No Details"));

        String result = financialAnalysisTool.getCompanyFinancialOverview(TEST_SYMBOL);

        assertTrue(result.contains("Could not retrieve financial overview for TESTC. Reason: API Error No Details"));
    }


    @Test
    void getCompanyFinancialOverview_withNAValues() {
        Map<String, String> overviewData = new LinkedHashMap<>();
        overviewData.put("Symbol", TEST_SYMBOL);
        overviewData.put("MarketCapitalization", "N/A");
        overviewData.put("PERatio", "25");
        when(financialsService.getCompanyOverview(TEST_SYMBOL)).thenReturn(overviewData);

        String result = financialAnalysisTool.getCompanyFinancialOverview(TEST_SYMBOL);

        assertTrue(result.contains("Market Cap: N/A"));
        assertTrue(result.contains("P/E Ratio: 25"));
    }

    // --- getCompanyEarningsSummary Tests ---
    @Test
    void getCompanyEarningsSummary_success() {
        Map<String, Object> earningsData = Map.of(
            "symbol", TEST_SYMBOL,
            "annualEarnings", List.of(
                Map.of("fiscalDateEnding", "2023-12-31", "reportedEPS", "10.00"),
                Map.of("fiscalDateEnding", "2022-12-31", "reportedEPS", "9.00")
            ),
            "quarterlyEarnings", List.of(
                Map.of("fiscalDateEnding", "2023-09-30", "reportedEPS", "2.50"),
                Map.of("fiscalDateEnding", "2023-06-30", "reportedEPS", "2.40")
            )
        );
        when(financialsService.getEarningsData(TEST_SYMBOL)).thenReturn(earningsData);

        String result = financialAnalysisTool.getCompanyEarningsSummary(TEST_SYMBOL);

        assertTrue(result.contains("Earnings Summary for TESTC"));
        assertTrue(result.contains("Annual Earnings:"));
        assertTrue(result.contains("- 2023-12-31: EPS $10.00"));
        assertTrue(result.contains("Quarterly Earnings:"));
        assertTrue(result.contains("- 2023-09-30: EPS $2.50"));
    }
    
    @Test
    void getCompanyEarningsSummary_limitEntries() {
        List<Map<String, String>> annual = Arrays.asList(
            Map.of("fiscalDateEnding", "2023", "reportedEPS", "4"),
            Map.of("fiscalDateEnding", "2022", "reportedEPS", "3"),
            Map.of("fiscalDateEnding", "2021", "reportedEPS", "2"),
            Map.of("fiscalDateEnding", "2020", "reportedEPS", "1")
        );
        List<Map<String, String>> quarterly = Arrays.asList(
            Map.of("fiscalDateEnding", "Q4-23", "reportedEPS", "1.0"),
            Map.of("fiscalDateEnding", "Q3-23", "reportedEPS", "0.9"),
            Map.of("fiscalDateEnding", "Q2-23", "reportedEPS", "0.8"),
            Map.of("fiscalDateEnding", "Q1-23", "reportedEPS", "0.7"),
            Map.of("fiscalDateEnding", "Q4-22", "reportedEPS", "0.6"),
            Map.of("fiscalDateEnding", "Q3-22", "reportedEPS", "0.5")
        );
        Map<String, Object> earningsData = Map.of("symbol", TEST_SYMBOL, "annualEarnings", annual, "quarterlyEarnings", quarterly);
        when(financialsService.getEarningsData(TEST_SYMBOL)).thenReturn(earningsData);
        String result = financialAnalysisTool.getCompanyEarningsSummary(TEST_SYMBOL);
        
        assertEquals(3, Arrays.stream(result.split("\n")).filter(line -> line.contains("Annual") && line.contains("EPS $")).count() -1); // -1 for "Annual Earnings:" line
        assertEquals(5, Arrays.stream(result.split("\n")).filter(line -> line.contains("Quarterly") && line.contains("EPS $")).count() -1); // -1 for "Quarterly Earnings:" line
        assertFalse(result.contains("2020: EPS $1")); // 4th annual should be excluded
        assertFalse(result.contains("Q3-22: EPS $0.5")); // 6th quarterly should be excluded
    }


    @Test
    void getCompanyEarningsSummary_serviceError() {
        // Simulating error structure where "error" key maps to another map from ApiErrorHandler
        Map<String, String> serviceErrorMap = Map.of("error", "Earnings API Error", "details", "Service unavailable");
        when(financialsService.getEarningsData(TEST_SYMBOL)).thenReturn(Map.of("error", serviceErrorMap));
        String result = financialAnalysisTool.getCompanyEarningsSummary(TEST_SYMBOL);
        assertTrue(result.contains("Could not retrieve earnings summary for TESTC. Reason: Earnings API Error (Details: Service unavailable)"));
    }
    
    @Test
    void getCompanyEarningsSummary_serviceErrorDirectString() {
        // Simulating error structure where "error" key maps directly to a string
        when(financialsService.getEarningsData(TEST_SYMBOL)).thenReturn(Map.of("error", "Direct Earnings API Error"));
        String result = financialAnalysisTool.getCompanyEarningsSummary(TEST_SYMBOL);
        assertTrue(result.contains("Could not retrieve earnings summary for TESTC. Reason: Direct Earnings API Error"));
    }


    @Test
    void getCompanyEarningsSummary_emptyLists() {
        Map<String, Object> earningsData = Map.of(
            "symbol", TEST_SYMBOL,
            "annualEarnings", Collections.emptyList(),
            "quarterlyEarnings", Collections.emptyList()
        );
        when(financialsService.getEarningsData(TEST_SYMBOL)).thenReturn(earningsData);
        String result = financialAnalysisTool.getCompanyEarningsSummary(TEST_SYMBOL);
        assertTrue(result.contains("- No annual earnings data available."));
        assertTrue(result.contains("- No quarterly earnings data available."));
    }

    // --- getSectorPerformanceSummary Tests ---
    @Test
    void getSectorPerformanceSummary_success() {
        Map<String, Map<String, String>> performanceData = new LinkedHashMap<>();
        performanceData.put("Real-Time Performance", Map.of("Technology", "1.5%", "Healthcare", "0.8%"));
        performanceData.put("1 Day Performance", Map.of("Technology", "0.5%", "Healthcare", "-0.2%"));
        when(sectorPerformanceService.getSectorPerformance()).thenReturn(performanceData);

        String result = financialAnalysisTool.getSectorPerformanceSummary();

        assertTrue(result.contains("Sector Performance:"));
        assertTrue(result.contains("Real-Time Performance:"));
        assertTrue(result.contains("- Technology: 1.5%"));
        assertTrue(result.contains("1 Day Performance:"));
    }

    @Test
    void getSectorPerformanceSummary_serviceError() {
        // SectorPerformanceService error structure: Map.of("error", actualErrorMapFromHandler)
        Map<String, String> actualError = Map.of("error", "Sector API Error", "details", "Auth failed");
        when(sectorPerformanceService.getSectorPerformance()).thenReturn(Map.of("error", actualError));
        String result = financialAnalysisTool.getSectorPerformanceSummary();
        assertTrue(result.contains("Could not retrieve sector performance. Reason: Sector API Error (Details: Auth failed)"));
    }
    
    @Test
    void getSectorPerformanceSummary_emptyData() {
        when(sectorPerformanceService.getSectorPerformance()).thenReturn(Collections.emptyMap());
        String result = financialAnalysisTool.getSectorPerformanceSummary();
        assertEquals("No sector performance data available at the moment.", result);
    }

    // --- getSectorPeerComparison Tests ---
    @Test
    void getSectorPeerComparison_sectorFound() {
        Map<String, Object> peData = Map.of("Technology", 25.0, "Healthcare", 20.0, "finance", 15.0); // Note: finance lowercase
        when(fmpService.getSectorPERatios("NASDAQ")).thenReturn(peData);

        String result = financialAnalysisTool.getSectorPeerComparison("Technology", "NASDAQ");
        assertTrue(result.contains("Average P/E Ratio for Technology sector on NASDAQ: 25.0."));
        assertTrue(result.contains("- Healthcare: 20.0"));
        assertTrue(result.contains("- finance: 15.0"));
        
        result = financialAnalysisTool.getSectorPeerComparison("Finance", "NASDAQ"); // Test case-insensitivity
        assertTrue(result.contains("Average P/E Ratio for finance sector on NASDAQ: 15.0."));
    }

    @Test
    void getSectorPeerComparison_sectorNotFound() {
        Map<String, Object> peData = Map.of("Healthcare", 20.0, "Financials", 15.0);
        when(fmpService.getSectorPERatios("NASDAQ")).thenReturn(peData);
        String result = financialAnalysisTool.getSectorPeerComparison("Technology", "NASDAQ");
        assertTrue(result.contains("Sector P/E data not found for Technology on NASDAQ."));
        assertTrue(result.contains("- Healthcare: 20.0"));
    }

    @Test
    void getSectorPeerComparison_serviceError() {
        Map<String, String> actualError = Map.of("error", "FMP Sector PE Error");
        when(fmpService.getSectorPERatios("NASDAQ")).thenReturn(Map.of("error", actualError));
        String result = financialAnalysisTool.getSectorPeerComparison("Technology", "NASDAQ");
        assertTrue(result.contains("Could not retrieve sector P/E data for NASDAQ. Reason: FMP Sector PE Error"));
    }
    
    @Test
    void getSectorPeerComparison_emptyServiceResponse() {
        when(fmpService.getSectorPERatios("LSE")).thenReturn(Collections.emptyMap());
        String result = financialAnalysisTool.getSectorPeerComparison("Technology", "LSE");
        assertEquals("No sector P/E data found for exchange LSE.", result);
    }


    // --- getIndustryPeerComparison Tests ---
    @Test
    void getIndustryPeerComparison_industryFound() {
        Map<String, Object> peData = Map.of("Software", 30.0, "Pharmaceuticals", 22.0, "Banks", 12.0);
        when(fmpService.getIndustryPERatios("NYSE")).thenReturn(peData);

        String result = financialAnalysisTool.getIndustryPeerComparison("Software", "NYSE");
        assertEquals("Average P/E Ratio for Software industry on NYSE: 30.0.", result);
    }

    @Test
    void getIndustryPeerComparison_industryNotFound() {
        Map<String, Object> peData = Map.of("Pharmaceuticals", 22.0, "Banks", 12.0);
        when(fmpService.getIndustryPERatios("NYSE")).thenReturn(peData);
        String result = financialAnalysisTool.getIndustryPeerComparison("Software", "NYSE");
        assertTrue(result.contains("Industry P/E data not found for Software on NYSE."));
        assertTrue(result.contains("Available industries: Pharmaceuticals, Banks") || result.contains("Available industries: Banks, Pharmaceuticals"));
    }

    @Test
    void getIndustryPeerComparison_serviceError() {
        Map<String, String> actualError = Map.of("error", "FMP Industry PE Error", "details", "Bad request");
        when(fmpService.getIndustryPERatios("NYSE")).thenReturn(Map.of("error", actualError));
        String result = financialAnalysisTool.getIndustryPeerComparison("Software", "NYSE");
        assertTrue(result.contains("Could not retrieve industry P/E data for NYSE. Reason: FMP Industry PE Error (Details: Bad request)"));
    }
    
    @Test
    void getIndustryPeerComparison_emptyServiceResponse() {
        when(fmpService.getIndustryPERatios("ASX")).thenReturn(Collections.emptyMap());
        String result = financialAnalysisTool.getIndustryPeerComparison("Mining", "ASX");
        assertEquals("No industry P/E data found for exchange ASX.", result);
    }
}
