package io.yayotron.investmentassistant.presenter.tool;

import io.yayotron.investmentassistant.feeder.alphavantage.AlphaVantageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    void getStockPrice_success() {
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
    void getStockPrice_success_dataNotAvailable() {
        String symbol = "NA_SYM";
        Map<String, String> mockData = Map.of(
                "symbol", "NA_SYM"
                // Price, volume, date are missing
        );

        when(alphaVantageService.getStockData(symbol)).thenReturn(mockData);

        String result = stockPriceTool.getStockPrice(symbol);

        String expected = "Latest price for NA_SYM: $N/A, Volume: N/A, Last trading day: N/A.";
        assertEquals(expected, result);
    }
    
    @Test
    void getStockPrice_errorFromService() {
        String symbol = "ERR";
        Map<String, String> errorData = Map.of("error", "API limit reached");

        when(alphaVantageService.getStockData(symbol)).thenReturn(errorData);

        String result = stockPriceTool.getStockPrice(symbol);

        String expected = "Error fetching stock data for ERR: API limit reached";
        assertEquals(expected, result);
    }

    @Test
    void getStockPrice_serviceReturnsEmptyMap() {
        String symbol = "EMPTY";
        Map<String, String> emptyData = Map.of();

        when(alphaVantageService.getStockData(symbol)).thenReturn(emptyData);

        String result = stockPriceTool.getStockPrice(symbol);

        // Symbol is taken from input if not in response, others default to N/A
        String expected = "Latest price for EMPTY: $N/A, Volume: N/A, Last trading day: N/A.";
        assertEquals(expected, result);
    }
}
