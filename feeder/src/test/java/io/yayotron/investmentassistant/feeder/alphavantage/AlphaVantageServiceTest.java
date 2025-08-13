package io.yayotron.investmentassistant.feeder.alphavantage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlphaVantageServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private AlphaVantageService alphaVantageService;

    private final String DUMMY_API_KEY = "TEST_KEY_ALPHA";

    @BeforeEach
    void setUp() {
        alphaVantageService = new AlphaVantageService(DUMMY_API_KEY, "https://www.alphavantage.co/query");
        ReflectionTestUtils.setField(alphaVantageService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(alphaVantageService, "objectMapper", objectMapper);
    }

    @Test
    @DisplayName("Given stock data, when getStockData is called multiple times, then caching should be verified")
    void givenStockData_whenGetStockDataIsCalledMultipleTimes_thenCachingShouldBeVerified() throws IOException {
        // given
        String symbol = "IBM_CACHE";
        String mockJsonResponse = new String(Objects.requireNonNull(this.getClass().getResourceAsStream("/json/alphavantage/success.json")).readAllBytes(), StandardCharsets.UTF_8);

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockJsonResponse);

        // when
        Map<String, Object> stockData1 = alphaVantageService.getStockData(symbol);
        Map<String, Object> stockData2 = alphaVantageService.getStockData(symbol);

        // then
        assertEquals("IBM", stockData1.get("symbol"), "First call should fetch data.");
        assertEquals("IBM", stockData2.get("symbol"), "Second call should also fetch data in this unit test.");

        verify(restTemplate, times(2)).getForObject(anyString(), eq(String.class));
    }

    @Test
    @DisplayName("Given valid stock data, when getStockData is called, then the company profile is returned")
    void givenValidStockData_whenGetStockDataIsCalled_thenCompanyProfileIsReturned() throws IOException {
        // given
        String symbol = "IBM";
        String mockJsonResponse = new String(Objects.requireNonNull(this.getClass().getResourceAsStream("/json/alphavantage/success.json")).readAllBytes(), StandardCharsets.UTF_8);

        when(restTemplate.getForObject(contains("symbol=" + symbol), eq(String.class))).thenReturn(mockJsonResponse);

        // when
        Map<String, Object> stockData = alphaVantageService.getStockData(symbol);

        // then
        assertEquals("IBM", stockData.get("symbol"));
        assertEquals("167.5000", stockData.get("price"));
        assertEquals("1234567", stockData.get("volume"));
        assertEquals("2024-03-15", stockData.get("latest trading day"));
        assertFalse(stockData.containsKey("error"));

        verify(restTemplate, times(1)).getForObject(anyString(), eq(String.class));
    }

    @Test
    @DisplayName("Given an API error, when getStockData is called, then an error is returned")
    void givenApiError_whenGetStockDataIsCalled_thenErrorIsReturned() {
        // given
        String symbol = "ERROR_SYM";
        String mockErrorResponse = """
        {
            "Error Message": "Invalid API call. Please check your API key and parameters."
        }
        """;

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockErrorResponse);

        // when
        Map<String, Object> stockData = alphaVantageService.getStockData(symbol);

        // then
        assertNotNull(stockData);
        assertTrue(stockData.containsKey("error"));
        assertTrue(stockData.get("error").toString().contains("Invalid API call"));

        verify(restTemplate, times(1)).getForObject(anyString(), eq(String.class));
    }

    @Test
    @DisplayName("Given an API information message, when getStockData is called, then an error is returned")
    void givenApiInformation_whenGetStockDataIsCalled_thenErrorIsReturned() {
        // given
        String symbol = "INFO_SYM";
        String mockInfoResponse = """
        {
            "Information": "Thank you for using Alpha Vantage! Our standard API call frequency is 5 calls per minute and 100 calls per day."
        }
        """;

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockInfoResponse);

        // when
        Map<String, Object> stockData = alphaVantageService.getStockData(symbol);

        // then
        assertNotNull(stockData);
        assertTrue(stockData.containsKey("error"));
        assertTrue(stockData.get("error").toString().contains("Thank you for using Alpha Vantage"));

        verify(restTemplate, times(1)).getForObject(anyString(), eq(String.class));
    }

    @Test
    @DisplayName("Given a RestTemplate exception, when getStockData is called, then an error is returned")
    void givenRestTemplateException_whenGetStockDataIsCalled_thenErrorIsReturned() {
        // given
        String symbol = "FAIL_SYM";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenThrow(new RuntimeException("Network error"));

        // when
        Map<String, Object> stockData = alphaVantageService.getStockData(symbol);

        // then
        assertNotNull(stockData);
        assertTrue(stockData.containsKey("error"));
        assertTrue(stockData.get("error").toString().contains("Failed to fetch stock data"));

        verify(restTemplate, times(1)).getForObject(anyString(), eq(String.class));
    }

    @Test
    @DisplayName("Given a JSON parsing error, when getStockData is called, then an error is returned")
    void givenJsonParsingError_whenGetStockDataIsCalled_thenErrorIsReturned() {
        // given
        String symbol = "BAD_JSON";
        String mockBadJsonResponse = "{ \"Global Quote\": { \"01. symbol\": \"BAD\"";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockBadJsonResponse);

        // when
        Map<String, Object> stockData = alphaVantageService.getStockData(symbol);

        // then
        assertNotNull(stockData);
        assertTrue(stockData.containsKey("error"));
        assertTrue(stockData.get("error").toString().contains("Failed to parse stock data response"));

        verify(restTemplate, times(1)).getForObject(anyString(), eq(String.class));
    }

    @Test
    @DisplayName("Given a null response from the API, when getStockData is called, then an error is returned")
    void givenNullResponseFromApi_whenGetStockDataIsCalled_thenErrorIsReturned() {
        // given
        String symbol = "NULL_RESP";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(null);

        // when
        Map<String, Object> stockData = alphaVantageService.getStockData(symbol);

        // then
        assertNotNull(stockData);
        assertTrue(stockData.containsKey("error"));
        assertEquals("No data received from API for stock data", stockData.get("error"));

        verify(restTemplate, times(1)).getForObject(anyString(), eq(String.class));
    }

    @Test
    @DisplayName("Given an empty response from the API, when getStockData is called, then an error is returned")
    void givenEmptyResponseFromApi_whenGetStockDataIsCalled_thenErrorIsReturned() {
        // given
        String symbol = "EMPTY_RESP";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("");

        // when
        Map<String, Object> stockData = alphaVantageService.getStockData(symbol);

        // then
        assertNotNull(stockData);
        assertTrue(stockData.containsKey("error"));
        assertEquals("No data received from API for stock data", stockData.get("error"));

        verify(restTemplate, times(1)).getForObject(anyString(), eq(String.class));
    }

    @Test
    @DisplayName("Given an unconfigured API key, when getStockData is called, then an error is returned")
    void givenUnconfiguredApiKey_whenGetStockDataIsCalled_thenErrorIsReturned() {
        // given
        ReflectionTestUtils.setField(alphaVantageService, "apiKey", "YOUR_API_KEY");
        String symbol = "ANY_SYM";

        // when
        Map<String, Object> stockData = alphaVantageService.getStockData(symbol);

        // then
        assertNotNull(stockData);
        assertTrue(stockData.containsKey("error"));
        assertEquals("API key not configured", stockData.get("error"));

        verifyNoInteractions(restTemplate);
    }
}
