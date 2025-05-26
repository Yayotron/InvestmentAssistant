package io.yayotron.investmentassistant.feeder.alphavantage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
// Import Logger/LoggerFactory if test-specific logging is desired, though typically not for assertions
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AlphaVantageServiceTest {

    // private static final Logger logger = LoggerFactory.getLogger(AlphaVantageServiceTest.class); // If needed

    @Mock
    private RestTemplate restTemplate;

    @Spy // Using Spy for ObjectMapper to allow real JSON parsing and verify its usage if needed
    private ObjectMapper objectMapper = new ObjectMapper();

    // @InjectMocks cannot be used if we manually pass mocks in constructor / set them via reflection
    private AlphaVantageService alphaVantageService;

    private final String DUMMY_API_KEY = "TEST_KEY_ALPHA";
    private final String INVALID_API_KEY_PLACEHOLDER = "YOUR_API_KEY";


    @BeforeEach
    void setUp() {
        // Initialize AlphaVantageService with a dummy API key for tests
        // ApiErrorHandler is instantiated within AlphaVantageService, so not mocked here.
        alphaVantageService = new AlphaVantageService(DUMMY_API_KEY);
        ReflectionTestUtils.setField(alphaVantageService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(alphaVantageService, "objectMapper", objectMapper);
        // Note: Caching tests are limited in pure unit tests without Spring context.
        // The @Cacheable annotation won't be active.
    }

    @Test
    void getStockData_cachingBehavior_noteAboutSpringContext() throws Exception {
        // This test primarily verifies the logic when called multiple times.
        // True @Cacheable testing requires Spring context.
        String symbol = "IBM_CACHE";
        String mockJsonResponse = """
        {
            "Global Quote": {
                "01. symbol": "IBM_CACHE",
                "05. price": "170.00",
                "06. volume": "2000000",
                "07. latest trading day": "2024-03-16"
            }
        }
        """;

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockJsonResponse);

        Map<String, String> stockData1 = alphaVantageService.getStockData(symbol);
        assertEquals("IBM_CACHE", stockData1.get("symbol"), "First call should fetch data.");

        Map<String, String> stockData2 = alphaVantageService.getStockData(symbol);
        assertEquals("IBM_CACHE", stockData2.get("symbol"), "Second call should also fetch data in this unit test.");

        // Verify RestTemplate was called twice because @Cacheable is not active without Spring context.
        verify(restTemplate, times(2)).getForObject(anyString(), eq(String.class));
        // A full integration test (@SpringBootTest) would be needed to verify times(1) due to caching.
    }

    @Test
    void getStockData_success() {
        String symbol = "IBM";
        String mockJsonResponse = """
        {
            "Global Quote": {
                "01. symbol": "IBM",
                "05. price": "167.5000",
                "06. volume": "1234567",
                "07. latest trading day": "2024-03-15"
            }
        }
        """;

        when(restTemplate.getForObject(contains("symbol=" + symbol), eq(String.class))).thenReturn(mockJsonResponse);

        Map<String, String> stockData = alphaVantageService.getStockData(symbol);

        assertEquals("IBM", stockData.get("symbol"));
        assertEquals("167.5000", stockData.get("price"));
        assertEquals("1234567", stockData.get("volume"));
        assertEquals("2024-03-15", stockData.get("latest trading day"));
        assertFalse(stockData.containsKey("error"));

        verify(restTemplate, times(1)).getForObject(anyString(), eq(String.class));
    }

    @Test
    void getStockData_apiError() throws Exception {
        String symbol = "ERROR_SYM";
        String mockErrorResponse = """
        {
            "Error Message": "Invalid API call. Please check your API key and parameters."
        }
        """;

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockErrorResponse);

        Map<String, String> stockData = alphaVantageService.getStockData(symbol);

        assertNotNull(stockData);
        assertTrue(stockData.containsKey("error"));
        assertTrue(stockData.get("error").contains("Invalid API call"));

        verify(restTemplate, times(1)).getForObject(anyString(), eq(String.class));
    }
    
    @Test
    void getStockData_apiInformation() throws Exception {
        String symbol = "INFO_SYM";
        String mockInfoResponse = """
        {
            "Information": "Thank you for using Alpha Vantage! Our standard API call frequency is 5 calls per minute and 100 calls per day."
        }
        """;

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockInfoResponse);

        Map<String, String> stockData = alphaVantageService.getStockData(symbol);

        assertNotNull(stockData);
        assertTrue(stockData.containsKey("error")); // We map "Information" to an "error" for simplicity in this client
        assertTrue(stockData.get("error").contains("Thank you for using Alpha Vantage"));

        verify(restTemplate, times(1)).getForObject(anyString(), eq(String.class));
    }

    @Test
    void getStockData_restTemplateException() {
        String symbol = "FAIL_SYM";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenThrow(new RuntimeException("Network error"));

        Map<String, String> stockData = alphaVantageService.getStockData(symbol);

        assertNotNull(stockData);
        assertTrue(stockData.containsKey("error"));
        assertEquals("Failed to fetch data", stockData.get("error"));

        verify(restTemplate, times(1)).getForObject(anyString(), eq(String.class));
    }
    
    @Test
    void getStockData_jsonParsingError() throws Exception {
        String symbol = "BAD_JSON";
        String mockBadJsonResponse = "{ \"Global Quote\": { \"01. symbol\": \"BAD\""; // Incomplete JSON

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockBadJsonResponse);

        Map<String, String> stockData = alphaVantageService.getStockData(symbol);

        assertNotNull(stockData);
        assertTrue(stockData.containsKey("error"));
        assertEquals("Failed to parse response", stockData.get("error"));
        
        verify(restTemplate, times(1)).getForObject(anyString(), eq(String.class));
    }

    @Test
    void getStockData_nullResponseFromApi() {
        String symbol = "NULL_RESP";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(null);

        Map<String, String> stockData = alphaVantageService.getStockData(symbol);

        assertNotNull(stockData);
        assertTrue(stockData.containsKey("error"));
        assertEquals("No data received or unexpected response format", stockData.get("error"));

        verify(restTemplate, times(1)).getForObject(anyString(), eq(String.class));
    }
    
    @Test
    void getStockData_emptyResponseFromApi() {
        String symbol = "EMPTY_RESP";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("");

        Map<String, String> stockData = alphaVantageService.getStockData(symbol);

        assertNotNull(stockData);
        assertTrue(stockData.containsKey("error"));
        // This will likely be a parsing error, depending on ObjectMapper's behavior with empty strings
        // For this test, we'll assume it leads to a parsing failure handled by the catch block
        assertEquals("Failed to parse response", stockData.get("error"));

        verify(restTemplate, times(1)).getForObject(anyString(), eq(String.class));
    }

    @Test
    void getStockData_apiKeyNotConfigured() {
        // Temporarily set API key to a "not configured" state
        ReflectionTestUtils.setField(alphaVantageService, "apiKey", "YOUR_API_KEY");
        
        String symbol = "ANY_SYM";
        Map<String, String> stockData = alphaVantageService.getStockData(symbol);
        
        assertNotNull(stockData);
        assertTrue(stockData.containsKey("error"));
        assertEquals("API key not configured", stockData.get("error"));
        
        // Restore API key for other tests
        ReflectionTestUtils.setField(alphaVantageService, "apiKey", DUMMY_API_KEY);
        verifyNoInteractions(restTemplate); // Ensure RestTemplate was not called
    }
}
