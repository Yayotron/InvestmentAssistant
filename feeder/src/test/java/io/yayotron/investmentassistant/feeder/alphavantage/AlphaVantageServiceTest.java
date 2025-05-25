package io.yayotron.investmentassistant.feeder.alphavantage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AlphaVantageServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Spy // Using Spy for ObjectMapper to allow real JSON parsing
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AlphaVantageService alphaVantageService;

    private final String DUMMY_API_KEY = "TEST_KEY";

    @BeforeEach
    void setUp() {
        // Initialize AlphaVantageService with a dummy API key for tests
        alphaVantageService = new AlphaVantageService(DUMMY_API_KEY);
        // Inject the mocked RestTemplate and spied ObjectMapper
        ReflectionTestUtils.setField(alphaVantageService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(alphaVantageService, "objectMapper", objectMapper);

        // Basic CacheManager setup for testing @Cacheable
        // In a Spring Boot test, you might use @SpringBootTest and auto-configured CacheManager
        // For a plain unit test, manually creating one is simpler.
        CacheManager cacheManager = new ConcurrentMapCacheManager("stockData");
        // The AlphaVantageService isn't managed by Spring in this unit test,
        // so @Cacheable won't work automatically.
        // To test caching properly here, we'd need Spring context or a proxy.
        // For now, the existing tests cover the logic *within* getStockData.
        // A separate integration test would be better for @Cacheable.
        // However, if we wanted to force it, we would need to wrap alphaVantageService
        // in a proxy that handles @Cacheable, which is complex for a unit test.
        //
        // Let's assume for now that @Cacheable is tested in an integration test.
        // The provided structure is a unit test, not an integration test.
        // So, I will skip direct testing of @Cacheable here and focus on logic.
        // If direct testing of @Cacheable in this *unit* test is strictly required,
        // the test setup needs to be significantly more complex (e.g. using AOP proxying manually).

        // The alternative is to use Spring's testing support.
        // Let's proceed by adding a test that *would* show caching if it were an integration test,
        // acknowledging it won't truly test @Cacheable in this pure Mockito setup without Spring context.
    }

    // This test is more of an integration test for caching.
    // For it to work correctly with @Cacheable, Spring context is needed.
    // In a pure unit test like this, it will just call the method twice without cache interaction.
    // I will add it with comments explaining this limitation.
    @Test
    void getStockData_cachingBehavior_requiresSpringContextToFullyTest() throws Exception {
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

        // First call - should hit the restTemplate
        Map<String, String> stockData1 = alphaVantageService.getStockData(symbol);
        assertNotNull(stockData1);
        assertEquals("IBM_CACHE", stockData1.get("symbol"));

        // Second call - in a true @Cacheable scenario, this should NOT hit restTemplate
        Map<String, String> stockData2 = alphaVantageService.getStockData(symbol);
        assertNotNull(stockData2);
        assertEquals("IBM_CACHE", stockData2.get("symbol"));

        // In this unit test setup (without Spring managing the bean and its aspects like @Cacheable),
        // restTemplate will be called twice. An integration test is needed for true @Cacheable verification.
        // If this were an integration test with Spring context, we would expect times(1).
        verify(restTemplate, times(2)).getForObject(anyString(), eq(String.class)); 
        // To properly test @Cacheable, this test should be in a Spring Boot test environment.
        // For example, using @SpringBootTest and injecting the service.
    }


    @Test
    void getStockData_success() throws Exception {
        String symbol = "IBM";
        String mockJsonResponse = """
        {
            "Global Quote": {
                "01. symbol": "IBM",
                "02. open": "167.0000",
                "03. high": "168.0000",
                "04. low": "166.5000",
                "05. price": "167.5000",
                "06. volume": "1234567",
                "07. latest trading day": "2024-03-15",
                "08. previous close": "166.0000",
                "09. change": "1.5000",
                "10. change percent": "0.9036%"
            }
        }
        """;

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockJsonResponse);

        Map<String, String> stockData = alphaVantageService.getStockData(symbol);

        assertNotNull(stockData);
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
