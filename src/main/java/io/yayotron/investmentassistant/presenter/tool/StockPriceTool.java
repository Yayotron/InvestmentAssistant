package io.yayotron.investmentassistant.presenter.tool;

import dev.langchain4j.agent.tool.Tool;
import io.yayotron.investmentassistant.feeder.alphavantage.AlphaVantageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class StockPriceTool {

    private static final Logger logger = LoggerFactory.getLogger(StockPriceTool.class);
    private final AlphaVantageService alphaVantageService;

    public StockPriceTool(AlphaVantageService alphaVantageService) {
        this.alphaVantageService = alphaVantageService;
    }

    @Tool("Gets the latest stock price, volume, and last trading day for a given symbol.")
    public String getStockPrice(String symbol) {
        logger.info("Fetching stock price for symbol: {}", symbol);
        Map<String, String> stockData = alphaVantageService.getStockData(symbol);

        if (stockData.containsKey("error")) {
            String errorMsg = stockData.get("error");
            String errorDetails = stockData.get("details"); // As per ApiErrorHandler format
            String combinedError = "Error: " + errorMsg + (errorDetails != null ? " (Details: " + errorDetails + ")" : "");
            logger.error("Error fetching stock data for symbol {}: {}", symbol, combinedError);
            return "Error fetching stock data for " + symbol + ": " + combinedError;
        }

        // Defensive checks for expected keys, even if no "error" key is present
        String price = stockData.get("price");
        String volume = stockData.get("volume");
        String latestTradingDay = stockData.get("latest trading day");
        String actualSymbol = stockData.getOrDefault("symbol", symbol); // Use symbol from response if available

        boolean isDataIncomplete = price == null || volume == null || latestTradingDay == null;

        if (isDataIncomplete) {
            logger.warn("Incomplete stock data received for symbol {}. Price: {}, Volume: {}, Date: {}", 
                        actualSymbol, price, volume, latestTradingDay);
            return String.format("Data for %s is incomplete. Price: $%s, Volume: %s, Last trading day: %s.",
                    actualSymbol,
                    (price != null ? price : "N/A"),
                    (volume != null ? volume : "N/A"),
                    (latestTradingDay != null ? latestTradingDay : "N/A"));
        }
        
        // AlphaVantageService already defaults to "N/A" for missing values if not an error.
        // This explicit check here is more for if AlphaVantageService changed its contract
        // or if a field was unexpectedly missing from a non-error response.
        // The previous logic `stockData.getOrDefault("key", "N/A")` from AlphaVantageService's success path
        // means price, volume, latestTradingDay should always be present (even if "N/A").
        // The `isDataIncomplete` check with `== null` might be redundant if AlphaVantageService guarantees non-null values.
        // However, it's a good defensive check. Let's assume AlphaVantageService might return nulls for non-critical fields.

        logger.info("Successfully retrieved stock data for symbol: {}", actualSymbol);
        return String.format("Latest price for %s: $%s, Volume: %s, Last trading day: %s.",
                actualSymbol, 
                stockData.getOrDefault("price", "N/A"), // Use getOrDefault for safety, though checked above
                stockData.getOrDefault("volume", "N/A"),
                stockData.getOrDefault("latest trading day", "N/A"));
    }
}
