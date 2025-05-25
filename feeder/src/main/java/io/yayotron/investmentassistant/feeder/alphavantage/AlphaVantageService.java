package io.yayotron.investmentassistant.feeder.alphavantage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@Service
public class AlphaVantageService {

    private static final String ALPHA_VANTAGE_URL = "https://www.alphavantage.co/query";

    private final String apiKey;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AlphaVantageService(@org.springframework.beans.factory.annotation.Value("${alphavantage.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Cacheable(value = "stockData", key = "#symbol")
    public Map<String, String> getStockData(String symbol) {
        if (apiKey == null || apiKey.isEmpty() || "YOUR_API_KEY".equals(apiKey)) {
            System.err.println("API key is not configured. Please set alphavantage.api.key in application.properties");
            return Collections.singletonMap("error", "API key not configured");
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(ALPHA_VANTAGE_URL)
                .queryParam("function", "GLOBAL_QUOTE")
                .queryParam("symbol", symbol)
                .queryParam("apikey", apiKey);

        try {
            String response = restTemplate.getForObject(builder.toUriString(), String.class);
            if (response != null) {
                Map<String, Object> fullResponse = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
                if (fullResponse.containsKey("Global Quote")) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> globalQuote = (Map<String, String>) fullResponse.get("Global Quote");
                    // Filter for specific fields if necessary, or return the whole quote
                    // For now, returning the relevant parts as per the initial thought
                    return Map.of(
                        "symbol", globalQuote.getOrDefault("01. symbol", "N/A"),
                        "price", globalQuote.getOrDefault("05. price", "N/A"),
                        "volume", globalQuote.getOrDefault("06. volume", "N/A"),
                        "latest trading day", globalQuote.getOrDefault("07. latest trading day", "N/A")
                    );
                } else if (fullResponse.containsKey("Error Message")) {
                    System.err.println("Alpha Vantage API Error: " + fullResponse.get("Error Message"));
                    return Collections.singletonMap("error", "API Error: " + fullResponse.get("Error Message"));
                } else if (fullResponse.containsKey("Information")) {
                    System.err.println("Alpha Vantage API Information: " + fullResponse.get("Information"));
                    return Collections.singletonMap("error", "API Info: " + fullResponse.get("Information"));
                }
            }
        } catch (IOException e) {
            System.err.println("Error parsing JSON response: " + e.getMessage());
            return Collections.singletonMap("error", "Failed to parse response");
        } catch (Exception e) {
            System.err.println("Error fetching stock data for " + symbol + ": " + e.getMessage());
            return Collections.singletonMap("error", "Failed to fetch data");
        }
        return Collections.singletonMap("error", "No data received or unexpected response format");
    }
}
