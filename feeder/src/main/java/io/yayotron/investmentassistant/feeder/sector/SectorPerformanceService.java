package io.yayotron.investmentassistant.feeder.sector;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.yayotron.investmentassistant.feeder.common.ApiErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class SectorPerformanceService {

    private static final Logger logger = LoggerFactory.getLogger(SectorPerformanceService.class);
    private static final String ALPHA_VANTAGE_URL = "https://www.alphavantage.co/query";
    private final String apiKey;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ApiErrorHandler apiErrorHandler;

    public SectorPerformanceService(@Value("${alphavantage.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.apiErrorHandler = new ApiErrorHandler(); // Instantiate directly
    }

    @Cacheable(value = "sectorPerformance")
    public Map<String, Map<String, String>> getSectorPerformance() {
        if (isApiKeyInvalid()) {
            logger.warn("AlphaVantage API key is not configured for SectorPerformanceService.");
            // The return type is Map<String, Map<String, String>>, error needs to conform or be handled by caller.
            // For now, returning a map that signals an error at the top level.
            return Collections.singletonMap("error", apiErrorHandler.createSingletonErrorResponse("API key not configured", logger));
        }

        logger.info("Fetching sector performance data.");
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(ALPHA_VANTAGE_URL)
                .queryParam("function", "SECTOR")
                .queryParam("apikey", apiKey);

        try {
            String response = restTemplate.getForObject(builder.toUriString(), String.class);
            if (response == null || response.isEmpty()) {
                logger.warn("No response received from AlphaVantage for sector performance.");
                return Collections.singletonMap("error", apiErrorHandler.createSingletonErrorResponse("No data received from API for sector performance", logger));
            }

            Map<String, Object> rawData = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
            Optional<Map<String, String>> apiError = apiErrorHandler.handleAlphaVantageError(rawData, "SectorPerformanceService.getSectorPerformance", "N/A_SECTOR_ENDPOINT", logger);
            if (apiError.isPresent()) {
                return Collections.singletonMap("error", apiError.get());
            }
            
            Map<String, Map<String, String>> result = new LinkedHashMap<>(); // Preserve order of ranks
            boolean dataFound = false;
            for (Map.Entry<String, Object> entry : rawData.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("Rank ")) { // Process only rank categories
                    String cleanedRankName = key.substring(key.indexOf(":") + 1).trim();
                    if (entry.getValue() instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, String> sectors = (Map<String, String>) entry.getValue();
                        result.put(cleanedRankName, sectors);
                        dataFound = true;
                    } else {
                        logger.warn("Expected a Map for rank category '{}', but found: {}", key, entry.getValue().getClass().getSimpleName());
                    }
                }
            }
            
            if (!dataFound) {
                 logger.warn("No 'Rank X' data found in sector performance response. Response: {}", response.substring(0, Math.min(response.length(), 1000)));
                 return Collections.singletonMap("error", apiErrorHandler.createErrorResponse("Unexpected response format or no sector performance data found.", "Response: " + response.substring(0, Math.min(response.length(), 1000)), logger));
            }
            
            logger.info("Successfully fetched sector performance data.");
            return result;

        } catch (IOException e) {
            return Collections.singletonMap("error", apiErrorHandler.createErrorResponse("Failed to parse sector performance response", logger, e));
        } catch (Exception e) {
            return Collections.singletonMap("error", apiErrorHandler.createErrorResponse("Failed to fetch sector performance data", logger, e));
        }
    }

    private boolean isApiKeyInvalid() {
        return apiKey == null || apiKey.isEmpty() || "YOUR_API_KEY".equals(apiKey);
    }
}
