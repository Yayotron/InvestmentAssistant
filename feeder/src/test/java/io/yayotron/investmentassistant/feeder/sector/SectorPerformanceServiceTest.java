package io.yayotron.investmentassistant.feeder.sector;

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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SectorPerformanceServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private SectorPerformanceService sectorPerformanceService;

    private final String DUMMY_API_KEY = "TEST_KEY_SECTOR";
    private final String INVALID_API_KEY_PLACEHOLDER = "YOUR_API_KEY";

    @BeforeEach
    void setUp() {
        sectorPerformanceService = new SectorPerformanceService(DUMMY_API_KEY);
        ReflectionTestUtils.setField(sectorPerformanceService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(sectorPerformanceService, "objectMapper", objectMapper);
    }

    @Test
    void getSectorPerformance_success() {
        String mockJsonResponse = """
        {
            "Rank A: Real-Time Performance": {
                "Energy": "1.23%",
                "Utilities": "0.55%"
            },
            "Rank B: 1 Day Performance": {
                "Financials": "-0.20%",
                "Technology": "0.80%"
            }
        }
        """;
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockJsonResponse);

        Map<String, Map<String, String>> performance = sectorPerformanceService.getSectorPerformance();

        assertNotNull(performance);
        assertFalse(performance.containsKey("error"));
        assertTrue(performance.containsKey("Real-Time Performance"));
        assertEquals("1.23%", performance.get("Real-Time Performance").get("Energy"));
        assertTrue(performance.containsKey("1 Day Performance"));
        assertEquals("0.80%", performance.get("1 Day Performance").get("Technology"));
        verify(restTemplate, times(1)).getForObject(anyString(), eq(String.class));
    }

    @Test
    void getSectorPerformance_apiError() {
        String mockErrorResponse = "{ \"Error Message\": \"Invalid API call for sector.\" }";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockErrorResponse);

        Map<String, Map<String, String>> performance = sectorPerformanceService.getSectorPerformance();

        assertNotNull(performance);
        assertTrue(performance.containsKey("error"));
        Map<String, String> errorDetails = performance.get("error"); // Error details are in a nested map
        assertTrue(errorDetails.get("error").contains("API Error in SectorPerformanceService.getSectorPerformance for N/A_SECTOR_ENDPOINT: Invalid API call for sector."));
    }
    
    @Test
    void getSectorPerformance_apiNote() {
        String mockNoteResponse = "{ \"Note\": \"Thank you for using Alpha Vantage! Our standard API call frequency is 5 calls per minute and 100 calls per day. Please visit https://www.alphavantage.co/premium/ if you would like to target a higher API call frequency.\" }";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockNoteResponse);

        Map<String, Map<String, String>> performance = sectorPerformanceService.getSectorPerformance();
        
        assertNotNull(performance);
        assertTrue(performance.containsKey("error"));
        Map<String, String> errorDetails = performance.get("error");
        assertTrue(errorDetails.get("error").contains("API Error in SectorPerformanceService.getSectorPerformance for N/A_SECTOR_ENDPOINT: Thank you for using Alpha Vantage!"));
    }

    @Test
    void getSectorPerformance_emptyResponse() {
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("");
        Map<String, Map<String, String>> performance = sectorPerformanceService.getSectorPerformance();
        assertTrue(performance.containsKey("error"));
        assertEquals("No data received from API for sector performance", performance.get("error").get("error"));
    }
    
    @Test
    void getSectorPerformance_nullResponse() {
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(null);
        Map<String, Map<String, String>> performance = sectorPerformanceService.getSectorPerformance();
        assertTrue(performance.containsKey("error"));
        assertEquals("No data received from API for sector performance", performance.get("error").get("error"));
    }


    @Test
    void getSectorPerformance_noRankData() {
        String mockJsonResponse = "{ \"Meta Data\": \"Some info\" }"; // No "Rank X" keys
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockJsonResponse);
        Map<String, Map<String, String>> performance = sectorPerformanceService.getSectorPerformance();
        assertTrue(performance.containsKey("error"));
        assertEquals("Unexpected response format or no sector performance data found.", performance.get("error").get("error"));
    }
    
    @Test
    void getSectorPerformance_emptyDataObject() {
        String mockJsonResponse = "{}"; // Empty JSON
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockJsonResponse);
        Map<String, Map<String, String>> performance = sectorPerformanceService.getSectorPerformance();
        assertTrue(performance.containsKey("error"));
         assertEquals("Unexpected response format or no sector performance data found.", performance.get("error").get("error"));
    }


    @Test
    void getSectorPerformance_apiKeyNotConfigured() {
        sectorPerformanceService = new SectorPerformanceService(INVALID_API_KEY_PLACEHOLDER);
        ReflectionTestUtils.setField(sectorPerformanceService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(sectorPerformanceService, "objectMapper", objectMapper);

        Map<String, Map<String, String>> performance = sectorPerformanceService.getSectorPerformance();
        assertTrue(performance.containsKey("error"));
        assertEquals("API key not configured", performance.get("error").get("error")); // Error is in nested map
        verifyNoInteractions(restTemplate);
    }
    
    @Test
    void getSectorPerformance_jsonParsingError() {
        String symbol = "BAD_JSON_SECTOR";
        String mockBadJsonResponse = "{ \"Rank A: Real-Time Performance\": { \"Energy\": \"1.23%\", "; // Incomplete JSON
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockBadJsonResponse);
        Map<String, Map<String, String>> data = sectorPerformanceService.getSectorPerformance();

        assertTrue(data.containsKey("error"));
        Map<String, String> errorDetails = data.get("error");
        assertEquals("Failed to parse sector performance response", errorDetails.get("error"));
        assertTrue(errorDetails.get("details").toString().contains("Unrecognized token 'BAD_JSON_SECTOR'"));
    }
}
