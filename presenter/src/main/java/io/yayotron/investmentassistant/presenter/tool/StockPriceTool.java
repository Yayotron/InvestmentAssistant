package io.yayotron.investmentassistant.presenter.tool;

import dev.langchain4j.agent.tool.Tool;
import io.yayotron.investmentassistant.feeder.alphavantage.AlphaVantageService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class StockPriceTool {

    private final AlphaVantageService alphaVantageService;

    public StockPriceTool(AlphaVantageService alphaVantageService) {
        this.alphaVantageService = alphaVantageService;
    }

    @Tool("Gets the latest stock price, volume, and last trading day for a given symbol.")
    public String getStockPrice(String symbol) {
        Map<String, String> stockData = alphaVantageService.getStockData(symbol);

        if (stockData.containsKey("error")) {
            return "Error fetching stock data for " + symbol + ": " + stockData.get("error");
        }

        String price = stockData.getOrDefault("price", "N/A");
        String volume = stockData.getOrDefault("volume", "N/A");
        String latestTradingDay = stockData.getOrDefault("latest trading day", "N/A");
        String actualSymbol = stockData.getOrDefault("symbol", symbol); // Use symbol from response if available

        return String.format("Latest price for %s: $%s, Volume: %s, Last trading day: %s.",
                actualSymbol, price, volume, latestTradingDay);
    }
}
