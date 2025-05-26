package io.yayotron.investmentassistant.presenter.tool;

import io.yayotron.investmentassistant.feeder.alphavantage.AlphaVantageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StockPriceToolTest {

    @Mock
    private AlphaVantageService alphaVantageService;

    @InjectMocks
    private StockPriceTool stockPriceTool;

    @Test
    void getStockPrice_success_allDataPresent() {
        String symbol = "MSFT";
        Map<String, String> mockData = Map.of(
                "symbol", "MSFT",
                "price", "400.00",
                "volume", "25000000",
                "latest trading day", "2024-03-15"
        );
        when(alphaVantageService.getStockData(symbol)).thenReturn(mockData);
        String result = stockPriceTool.getStockPrice(symbol);
        String expected = "Latest price for MSFT: $400.00, Volume: 25000000, Last trading day: 2024-03-15.";
        assertEquals(expected, result);
    }

    @Test
    void getStockPrice_success_someDataIsNAFromService() {
        String symbol = "NA_SYM";
        // AlphaVantageService is expected to return "N/A" for fields it couldn't find.
        Map<String, String> mockData = Map.of(
                "symbol", "NA_SYM",
                "price", "N/A",
                "volume", "12345", // volume present
                "latest trading day", "N/A"
        );
        when(alphaVantageService.getStockData(symbol)).thenReturn(mockData);
        String result = stockPriceTool.getStockPrice(symbol);
        // The tool should format this as received.
        String expected = "Latest price for NA_SYM: $N/A, Volume: 12345, Last trading day: N/A.";
        assertEquals(expected, result);
    }

    @Test
    void getStockPrice_incompleteData_missingKey() {
        String symbol = "INCOMPLETE";
        Map<String, String> mockData = new HashMap<>(); // Use HashMap to allow modification
        mockData.put("symbol", "INCOMPLETE");
        mockData.put("price", "150.00");
        // "volume" key is missing
        mockData.put("latest trading day", "2024-03-15");

        when(alphaVantageService.getStockData(symbol)).thenReturn(mockData);
        String result = stockPriceTool.getStockPrice(symbol);
        String expected = "Data for INCOMPLETE is incomplete. Price: $150.00, Volume: N/A, Last trading day: 2024-03-15.";
        assertEquals(expected, result);
    }
    
    @Test
    void getStockPrice_incompleteData_nullValue() {
        String symbol = "NULL_VAL";
        Map<String, String> mockData = new HashMap<>();
        mockData.put("symbol", "NULL_VAL");
        mockData.put("price", "150.00");
        mockData.put("volume", null); // volume key present but value is null
        mockData.put("latest trading day", "2024-03-15");

        when(alphaVantageService.getStockData(symbol)).thenReturn(mockData);
        String result = stockPriceTool.getStockPrice(symbol);
        String expected = "Data for NULL_VAL is incomplete. Price: $150.00, Volume: N/A, Last trading day: 2024-03-15.";
        assertEquals(expected, result);
    }


    @Test
    void getStockPrice_errorFromService_withDetails() {
        String symbol = "ERR_D";
        Map<String, String> errorData = Map.of(
                "error", "AlphaVantage API error",
                "details", "Invalid API key provided."
        );
        when(alphaVantageService.getStockData(symbol)).thenReturn(errorData);
        String result = stockPriceTool.getStockPrice(symbol);
        String expected = "Error fetching stock data for ERR_D: Error: AlphaVantage API error (Details: Invalid API key provided.)";
        assertEquals(expected, result);
    }
    
    @Test
    void getStockPrice_errorFromService_noDetails() {
        String symbol = "ERR_ND";
        Map<String, String> errorData = Map.of("error", "API rate limit exceeded");
        when(alphaVantageService.getStockData(symbol)).thenReturn(errorData);
        String result = stockPriceTool.getStockPrice(symbol);
        String expected = "Error fetching stock data for ERR_ND: Error: API rate limit exceeded";
        assertEquals(expected, result);
    }

    @Test
    void getStockPrice_serviceReturnsEmptyMap_treatedAsIncomplete() {
        String symbol = "EMPTY_MAP";
        Map<String, String> emptyData = Collections.emptyMap(); // Service returns an empty map (no error key)
        when(alphaVantageService.getStockData(symbol)).thenReturn(emptyData);
        String result = stockPriceTool.getStockPrice(symbol);
        // Symbol from input is used, others are N/A due to missing keys
        String expected = "Data for EMPTY_MAP is incomplete. Price: $N/A, Volume: N/A, Last trading day: N/A.";
        assertEquals(expected, result);
    }
}
