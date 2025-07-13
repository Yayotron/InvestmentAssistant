package io.yayotron.investmentassistant.feeder.fmp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpStatus;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FmpServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private FmpService fmpService;

    private final String DUMMY_API_KEY = "TEST_KEY_FMP";
    private final String INVALID_API_KEY_PLACEHOLDER = "YOUR_FMP_API_KEY";
    private final String TEST_EXCHANGE = "NASDAQ";

    @BeforeEach
    void setUp() {
        fmpService = new FmpService(DUMMY_API_KEY);
        ReflectionTestUtils.setField(fmpService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(fmpService, "objectMapper", objectMapper);
    }

    // --- getSectorPERatios Tests ---

    @Test
    void getSectorPERatios_success() throws JsonProcessingException {
        String mockJsonResponse = """
        [
            {"date" : "2024-03-18", "sector" : "Technology", "pe" : 30.5},
            {"date" : "2024-03-18", "sector" : "Healthcare", "pe" : 25.2},
            {"date" : "2024-03-18", "sector" : "Financials", "pe" : "15.0"}
        ]
        """;
        when(restTemplate.getForObject(contains("/sector_price_earning_ratio"), eq(String.class))).thenReturn(mockJsonResponse);

        Map<String, Object> sectorPEs = fmpService.getSectorPERatios(TEST_EXCHANGE);

        assertNotNull(sectorPEs);
        assertFalse(sectorPEs.containsKey("error"));
        assertEquals(30.5, (Double) sectorPEs.get("Technology"));
        assertEquals(25.2, (Double) sectorPEs.get("Healthcare"));
        assertEquals(15.0, (Double) sectorPEs.get("Financials")); // Test parsing String PE
        verify(restTemplate, times(1)).getForObject(anyString(), eq(String.class));
    }
    
    @Test
    void getSectorPERatios_unparseablePE() throws JsonProcessingException {
        String mockJsonResponse = """
        [
            {"date" : "2024-03-18", "sector" : "Technology", "pe" : "High"},
            {"date" : "2024-03-18", "sector" : "Healthcare", "pe" : 25.2}
        ]
        """;
        when(restTemplate.getForObject(contains("/sector_price_earning_ratio"), eq(String.class))).thenReturn(mockJsonResponse);
        Map<String, Object> sectorPEs = fmpService.getSectorPERatios(TEST_EXCHANGE);
        assertNull(sectorPEs.get("Technology")); // Should be null due to parsing error for "High"
        assertEquals(25.2, (Double) sectorPEs.get("Healthcare"));
    }


    @Test
    void getSectorPERatios_fmpApiErrorMessage() throws JsonProcessingException {
        String mockErrorResponse = "[{ \"Error Message\": \"Limit reached for API key.\" }]";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockErrorResponse);

        Map<String, Object> sectorPEs = fmpService.getSectorPERatios(TEST_EXCHANGE);

        assertTrue(sectorPEs.containsKey("error"));
        Map<String, String> errorDetails = (Map<String, String>) sectorPEs.get("error");
        assertEquals("FMP API Error", errorDetails.get("error"));
        assertEquals("Limit reached for API key.", errorDetails.get("details"));
    }

    @Test
    void getSectorPERatios_httpClientErrorException() {
        HttpClientErrorException clientErrorException = new HttpClientErrorException(
            HttpStatus.FORBIDDEN, "Forbidden", "{\"Error Message\":\"Invalid API Key\"}".getBytes(), Charset.defaultCharset()
        );
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenThrow(clientErrorException);

        Map<String, Object> sectorPEs = fmpService.getSectorPERatios(TEST_EXCHANGE);

        assertTrue(sectorPEs.containsKey("error"));
        Map<String, String> errorDetails = (Map<String, String>) sectorPEs.get("error");
        assertTrue(errorDetails.get("error").contains("FMP API client error for sector P/E"));
        assertTrue(errorDetails.get("details").contains("Invalid API Key"));
    }
    
    @Test
    void getSectorPERatios_emptyArrayResponse() {
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("[]");
        Map<String, Object> data = fmpService.getSectorPERatios(TEST_EXCHANGE);
        assertTrue(data.containsKey("error"));
        Map<String, String> errorDetails = (Map<String, String>) data.get("error");
        assertEquals("Empty data array for sector P/E ratios", errorDetails.get("error"));
    }

    @Test
    void getSectorPERatios_nullResponse() {
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(null);
        Map<String, Object> data = fmpService.getSectorPERatios(TEST_EXCHANGE);
        assertTrue(data.containsKey("error"));
        Map<String, String> errorDetails = (Map<String, String>) data.get("error");
        assertEquals("No data received from FMP API for sector P/E ratios", errorDetails.get("error"));
    }


    @Test
    void getSectorPERatios_apiKeyNotConfigured() {
        fmpService = new FmpService(INVALID_API_KEY_PLACEHOLDER);
        ReflectionTestUtils.setField(fmpService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(fmpService, "objectMapper", objectMapper);

        Map<String, Object> sectorPEs = fmpService.getSectorPERatios(TEST_EXCHANGE);
        assertTrue(sectorPEs.containsKey("error"));
        Map<String, String> errorDetails = (Map<String, String>) sectorPEs.get("error");
        assertEquals("FMP API key not configured", errorDetails.get("error"));
        verifyNoInteractions(restTemplate);
    }

    // --- getIndustryPERatios Tests (similar structure) ---

    @Test
    void getIndustryPERatios_success() throws JsonProcessingException {
        String mockJsonResponse = """
        [
            {"date" : "2024-03-18", "industry" : "Software", "pe" : 35.5},
            {"date" : "2024-03-18", "industry" : "Pharmaceuticals", "pe" : 22.1}
        ]
        """;
        when(restTemplate.getForObject(contains("/industry_price_earning_ratio"), eq(String.class))).thenReturn(mockJsonResponse);

        Map<String, Object> industryPEs = fmpService.getIndustryPERatios(TEST_EXCHANGE);

        assertNotNull(industryPEs);
        assertFalse(industryPEs.containsKey("error"));
        assertEquals(35.5, (Double) industryPEs.get("Software"));
        assertEquals(22.1, (Double) industryPEs.get("Pharmaceuticals"));
        verify(restTemplate, times(1)).getForObject(anyString(), eq(String.class));
    }

    @Test
    void getIndustryPERatios_fmpApiErrorMessage() throws JsonProcessingException {
        String mockErrorResponse = "[{ \"Error Message\": \"Invalid exchange for industry P/E.\" }]";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockErrorResponse);

        Map<String, Object> industryPEs = fmpService.getIndustryPERatios(TEST_EXCHANGE);

        assertTrue(industryPEs.containsKey("error"));
        Map<String, String> errorDetails = (Map<String, String>) industryPEs.get("error");
        assertEquals("FMP API Error", errorDetails.get("error"));
        assertEquals("Invalid exchange for industry P/E.", errorDetails.get("details"));
    }
    
    @Test
    void getIndustryPERatios_jsonParsingError() {
        String mockBadJsonResponse = "[{\"date\" : \"2024-03-18\", \"industry\" : \"Software\", \"pe\" : 35.5,"; // Malformed
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockBadJsonResponse);
        Map<String, Object> data = fmpService.getIndustryPERatios(TEST_EXCHANGE);

        assertTrue(data.containsKey("error"));
        Map<String, String> errorDetails = (Map<String, String>) data.get("error");
        assertEquals("Failed to parse FMP industry P/E response (exchange: " + TEST_EXCHANGE + ")", errorDetails.get("error"));
        assertTrue(errorDetails.get("details").toString().contains("Unexpected end-of-input"));
    }

    @Test
    void getIndustryPERatios_apiKeyNotConfigured() {
        fmpService = new FmpService(INVALID_API_KEY_PLACEHOLDER);
         ReflectionTestUtils.setField(fmpService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(fmpService, "objectMapper", objectMapper);

        Map<String, Object> industryPEs = fmpService.getIndustryPERatios(TEST_EXCHANGE);
        assertTrue(industryPEs.containsKey("error"));
        Map<String, String> errorDetails = (Map<String, String>) industryPEs.get("error");
        assertEquals("FMP API key not configured", errorDetails.get("error"));
        verifyNoInteractions(restTemplate);
    }
}
