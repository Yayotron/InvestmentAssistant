package io.yayotron.investmentassistant.feeder.alphavantage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.yayotron.investmentassistant.feeder.common.ApiErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Service
public class AlphaVantageService {

    private static final Logger logger = LoggerFactory.getLogger(AlphaVantageService.class);
    private static final String ALPHA_VANTAGE_URL = "https://www.alphavantage.co/query";

    private final String apiKey;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ApiErrorHandler apiErrorHandler;

    public AlphaVantageService(@org.springframework.beans.factory.annotation.Value("${alphavantage.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.apiErrorHandler = new ApiErrorHandler(); // Instantiate directly
    }
    
    private boolean isApiKeyInvalid() {
        return apiKey == null || apiKey.isEmpty() || "YOUR_API_KEY".equals(apiKey);
    }

    @Cacheable(value = "stockData", key = "#symbol")
    public Map<String, String> getStockData(String symbol) {
        if (isApiKeyInvalid()) {
            logger.warn("AlphaVantage API key is not configured.");
            return apiErrorHandler.createSingletonErrorResponse("API key not configured", logger);
        }
        
        logger.info("Fetching stock data for symbol: {}", symbol);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(ALPHA_VANTAGE_URL)
                .queryParam("function", "GLOBAL_QUOTE")
                .queryParam("symbol", symbol)
                .queryParam("apikey", apiKey);

        try {
            String response = restTemplate.getForObject(builder.toUriString(), String.class);
            if (response == null || response.isEmpty()) {
                logger.warn("No response received from AlphaVantage for symbol {}", symbol);
                return apiErrorHandler.createSingletonErrorResponse("No data received from API for stock data", logger);
            }

            Map<String, Object> fullResponse = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});

            Optional<Map<String, String>> apiError = apiErrorHandler.handleAlphaVantageError(fullResponse, "AlphaVantageService.getStockData", symbol, logger);
            if (apiError.isPresent()) {
                return apiError.get();
            }

            if (fullResponse.containsKey("Global Quote")) {
                @SuppressWarnings("unchecked")
                Map<String, String> globalQuote = (Map<String, String>) fullResponse.get("Global Quote");
                if (globalQuote.isEmpty()) {
                     logger.warn("Global Quote data is empty for symbol {}", symbol);
                     return apiErrorHandler.createSingletonErrorResponse("Global Quote data is empty for symbol: " + symbol, logger);
                }
                logger.info("Successfully fetched stock data for symbol: {}", symbol);
                return Map.of(
                    "symbol", globalQuote.getOrDefault("01. symbol", "N/A"),
                    "price", globalQuote.getOrDefault("05. price", "N/A"),
                    "volume", globalQuote.getOrDefault("06. volume", "N/A"),
                    "latest trading day", globalQuote.getOrDefault("07. latest trading day", "N/A")
                );
            } else {
                logger.warn("Response for symbol {} did not contain 'Global Quote'. Response: {}", symbol, response.substring(0, Math.min(response.length(), 500)));
                return apiErrorHandler.createErrorResponse("Response format error: 'Global Quote' missing.", "Response: " + response.substring(0, Math.min(response.length(), 500)), logger);
            }
        } catch (IOException e) {
            return apiErrorHandler.createErrorResponse("Failed to parse stock data response for symbol " + symbol, logger, e);
        } catch (Exception e) {
            return apiErrorHandler.createErrorResponse("Failed to fetch stock data for symbol " + symbol, logger, e);
        }
    }
}
