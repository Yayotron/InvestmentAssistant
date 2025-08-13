package io.yayotron.investmentassistant.feeder.financials;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FinancialsServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private FinancialsService financialsService;

    private final String DUMMY_API_KEY = "TEST_KEY_FINANCIALS";
    private final String INVALID_API_KEY_PLACEHOLDER = "YOUR_API_KEY";

    @BeforeEach
    void setUp() {
        financialsService = new FinancialsService(DUMMY_API_KEY, "https://www.alphavantage.co/query");
        ReflectionTestUtils.setField(financialsService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(financialsService, "objectMapper", objectMapper);
    }

    // --- getCompanyOverview Tests ---

    @Test
    void getCompanyOverview_success() {
        String symbol = "AAPL";
        String mockJsonResponse = """
        {
            "Symbol": "AAPL",
            "MarketCapitalization": "2500000000000",
            "EBITDA": "100000000000",
            "PERatio": "25.5",
            "EPS": "5.60",
            "RevenuePerShareTTM": "22.0",
            "GrossProfitTTM": "150000000000",
            "DilutedEPSTTM": "5.55",
            "QuarterlyEarningsGrowthYOY": "0.15",
            "QuarterlyRevenueGrowthYOY": "0.10",
            "AnalystTargetPrice": "190.0",
            "TrailingPE": "26.0",
            "ForwardPE": "24.0",
            "PriceToSalesRatioTTM": "7.0",
            "PriceToBookRatio": "40.0",
            "EVToRevenue": "7.5",
            "EVToEBITDA": "20.0",
            "Beta": "1.2",
            "SharesOutstanding": "16000000000",
            "DividendYield": "0.006",
            "ProfitMargin": "0.25",
            "ReturnOnEquityTTM": "0.50",
            "ReturnOnAssetsTTM": "0.10",
            "DebtToEquity": "1.5",
            "CurrentRatio": "1.0",
            "BookValue": "4.00"
        }
        """;
        when(restTemplate.getForObject(contains("OVERVIEW") , eq(String.class))).thenReturn(mockJsonResponse);

        Map<String, Object> overview = financialsService.getCompanyOverview(symbol);

        assertNotNull(overview);
        assertEquals("AAPL", overview.get("Symbol"));
        assertEquals("2500000000000", overview.get("MarketCapitalization"));
        assertEquals("25.5", overview.get("PERatio"));
        assertNull(overview.get("NonExistentMetric")); // Test for a metric not in the list
        assertFalse(overview.containsKey("error"));
        verify(restTemplate, times(1)).getForObject(anyString(), eq(String.class));
    }
    
    @Test
    void getCompanyOverview_metricsMissingOrNone() {
        String symbol = "TEST";
        String mockJsonResponse = """
        {
            "Symbol": "TEST",
            "MarketCapitalization": "None",
            "EBITDA": "",
            "PERatio": null
        }
        """;
        // EPS is missing entirely
        when(restTemplate.getForObject(contains("OVERVIEW"), eq(String.class))).thenReturn(mockJsonResponse);

        Map<String, Object> overview = financialsService.getCompanyOverview(symbol);

        assertNotNull(overview);
        assertEquals("TEST", overview.get("Symbol"));
        assertEquals("N/A", overview.get("MarketCapitalization"));
        assertEquals("N/A", overview.get("EBITDA"));
        assertEquals("N/A", overview.get("PERatio"));
        assertEquals("N/A", overview.get("EPS")); // Metric not present in JSON
        assertFalse(overview.containsKey("error"));
    }

    @Test
    void getCompanyOverview_apiError() {
        String symbol = "ERROR";
        String mockErrorResponse = "{ \"Error Message\": \"Invalid API call for overview.\" }";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockErrorResponse);

        Map<String, Object> overview = financialsService.getCompanyOverview(symbol);

        assertTrue(overview.containsKey("error"));
        assertTrue(overview.get("error").toString().contains("API Error in FinancialsService.getCompanyOverview for ERROR: Invalid API call for overview."));
    }

    @Test
    void getCompanyOverview_apiInformation() {
        String symbol = "INFO";
        String mockInfoResponse = "{ \"Information\": \"API call frequency limit reached.\" }";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockInfoResponse);

        Map<String, Object> overview = financialsService.getCompanyOverview(symbol);

        assertTrue(overview.containsKey("error"));
        assertTrue(overview.get("error").toString().contains("API Error in FinancialsService.getCompanyOverview for INFO: API call frequency limit reached."));
    }
    
    @Test
    void getCompanyOverview_emptyResponseFromApi() {
        String symbol = "EMPTY_RESP";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("");
        Map<String, Object> data = financialsService.getCompanyOverview(symbol);
        assertTrue(data.containsKey("error"));
        assertEquals("No data received from API for company overview", data.get("error"));
    }

    @Test
    void getCompanyOverview_nullResponseFromApi() {
        String symbol = "NULL_RESP";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(null);
        Map<String, Object> data = financialsService.getCompanyOverview(symbol);
        assertTrue(data.containsKey("error"));
        assertEquals("No data received from API for company overview", data.get("error"));
    }
    
    @Test
    void getCompanyOverview_emptyDataObject() {
        String symbol = "EMPTY_DATA";
        String mockJsonResponse = "{}"; // Empty JSON object
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockJsonResponse);
        Map<String, Object> data = financialsService.getCompanyOverview(symbol);
        assertTrue(data.containsKey("error"));
        assertEquals("No overview data found for symbol: " + symbol, data.get("error"));
        assertEquals("Symbol may be invalid or delisted.", data.get("details"));

    }
    
    @Test
    void getCompanyOverview_symbolNullInResponse() {
        String symbol = "SYMBOL_NULL";
        String mockJsonResponse = "{ \"Symbol\": null }"; // Symbol is null
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockJsonResponse);
        Map<String, Object> data = financialsService.getCompanyOverview(symbol);
        assertTrue(data.containsKey("error"));
        assertEquals("No overview data found for symbol: " + symbol, data.get("error"));
         assertEquals("Symbol may be invalid or delisted.", data.get("details"));
    }


    @Test
    void getCompanyOverview_apiKeyNotConfigured() {
        financialsService = new FinancialsService(INVALID_API_KEY_PLACEHOLDER, "https://www.alphavantage.co/query");
        ReflectionTestUtils.setField(financialsService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(financialsService, "objectMapper", objectMapper);
        
        Map<String, Object> overview = financialsService.getCompanyOverview("ANY");
        assertTrue(overview.containsKey("error"));
        assertEquals("API key not configured", overview.get("error"));
        verifyNoInteractions(restTemplate);
    }

    // --- getEarningsData Tests ---

    @Test
    void getEarningsData_success() {
        String symbol = "MSFT";
        String mockJsonResponse = """
        {
            "symbol": "MSFT",
            "annualEarnings": [
                {"fiscalDateEnding": "2023-06-30", "reportedEPS": "9.60"},
                {"fiscalDateEnding": "2022-06-30", "reportedEPS": "9.20"}
            ],
            "quarterlyEarnings": [
                {"fiscalDateEnding": "2023-09-30", "reportedEPS": "2.99", "reportedDate": "2023-10-24", "surprise": "0.34", "surprisePercentage": "12.83"},
                {"fiscalDateEnding": "2023-06-30", "reportedEPS": "2.69"}
            ]
        }
        """;
        when(restTemplate.getForObject(contains("EARNINGS"), eq(String.class))).thenReturn(mockJsonResponse);

        Map<String, Object> earnings = financialsService.getEarningsData(symbol);

        assertNotNull(earnings);
        assertEquals("MSFT", earnings.get("symbol"));
        assertFalse(earnings.containsKey("error"));

        List<Map<String, String>> annual = (List<Map<String, String>>) earnings.get("annualEarnings");
        assertNotNull(annual);
        assertEquals(2, annual.size());
        assertEquals("9.60", annual.get(0).get("reportedEPS"));

        List<Map<String, String>> quarterly = (List<Map<String, String>>) earnings.get("quarterlyEarnings");
        assertNotNull(quarterly);
        assertEquals(2, quarterly.size());
        assertEquals("2.99", quarterly.get(0).get("reportedEPS"));
        verify(restTemplate, times(1)).getForObject(anyString(), eq(String.class));
    }
    
    @Test
    void getEarningsData_missingFieldsInEarningsList() {
        String symbol = "TEST_INC";
        String mockJsonResponse = """
        {
            "symbol": "TEST_INC",
            "annualEarnings": [
                {"fiscalDateEnding": "2023-12-31"} 
            ],
            "quarterlyEarnings": [
                {"reportedEPS": "1.00"} 
            ]
        }
        """;
        when(restTemplate.getForObject(contains("EARNINGS"), eq(String.class))).thenReturn(mockJsonResponse);
        Map<String, Object> earnings = financialsService.getEarningsData(symbol);

        List<Map<String, String>> annual = (List<Map<String, String>>) earnings.get("annualEarnings");
        assertEquals("N/A", annual.get(0).get("reportedEPS"));
        assertEquals("2023-12-31", annual.get(0).get("fiscalDateEnding"));

        List<Map<String, String>> quarterly = (List<Map<String, String>>) earnings.get("quarterlyEarnings");
        assertEquals("1.00", quarterly.get(0).get("reportedEPS"));
        assertEquals("N/A", quarterly.get(0).get("fiscalDateEnding"));
    }


    @Test
    void getEarningsData_apiError() {
        String symbol = "ERROR";
        String mockErrorResponse = "{ \"Error Message\": \"Invalid API call for earnings.\" }";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockErrorResponse);

        Map<String, Object> earnings = financialsService.getEarningsData(symbol); // Return type is Map<String, Object>
        // Error map from handler is Map<String, String>. Check if it's nested under "error" key.
        assertTrue(earnings.containsKey("error"));
        Object errorValue = earnings.get("error");
        assertTrue(errorValue instanceof String); // ApiErrorHandler.handleAlphaVantageError returns Map<String, String>
        assertTrue(((String) errorValue).contains("API Error in FinancialsService.getEarningsData for ERROR: Invalid API call for earnings."));
    }

    @Test
    void getEarningsData_emptyAnnualAndQuarterlyArrays() {
        String symbol = "NO_EARNINGS";
        String mockJsonResponse = """
        {
            "symbol": "NO_EARNINGS",
            "annualEarnings": [],
            "quarterlyEarnings": []
        }
        """;
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockJsonResponse);
        Map<String, Object> earnings = financialsService.getEarningsData(symbol);
        assertTrue(((List) earnings.get("annualEarnings")).isEmpty());
        assertTrue(((List) earnings.get("quarterlyEarnings")).isEmpty());
        assertFalse(earnings.containsKey("error"));
    }
    
    @Test
    void getEarningsData_missingEarningsKeys() {
        String symbol = "MISSING_KEYS";
        String mockJsonResponse = "{ \"symbol\": \"MISSING_KEYS\" }"; // No annualEarnings or quarterlyEarnings keys
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockJsonResponse);
        Map<String, Object> earnings = financialsService.getEarningsData(symbol);
        assertTrue(((List) earnings.get("annualEarnings")).isEmpty());
        assertTrue(((List) earnings.get("quarterlyEarnings")).isEmpty());
        assertFalse(earnings.containsKey("error"));
    }


    @Test
    void getEarningsData_apiKeyNotConfigured() {
        financialsService = new FinancialsService(INVALID_API_KEY_PLACEHOLDER, "https://www.alphavantage.co/query");
        ReflectionTestUtils.setField(financialsService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(financialsService, "objectMapper", objectMapper);
        
        Map<String, Object> earnings = financialsService.getEarningsData("ANY");
        assertTrue(earnings.containsKey("error"));
        assertEquals("API key not configured", earnings.get("error"));
        verifyNoInteractions(restTemplate);
    }
    
    @Test
    void getEarningsData_jsonParsingError() {
        String symbol = "BAD_JSON_EARNINGS";
        String mockBadJsonResponse = "{ \"symbol\": \"BAD_JSON_EARNINGS\", \"annualEarnings\": [{\"fiscalDateEnding\": \"2023-12-31\""; // Incomplete JSON
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockBadJsonResponse);
        Map<String, Object> data = financialsService.getEarningsData(symbol);

        assertTrue(data.containsKey("error"));
        assertEquals("Failed to parse earnings data response for symbol " + symbol, data.get("error"));
        assertTrue(data.get("details").toString().contains("Unexpected end-of-input"));
    }
}
