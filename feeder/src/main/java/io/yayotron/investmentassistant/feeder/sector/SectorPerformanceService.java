package io.yayotron.investmentassistant.feeder.sector;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SectorPerformanceService {

    private static final String ALPHA_VANTAGE_URL = "https://www.alphavantage.co/query";
    private final String apiKey;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public SectorPerformanceService(@Value("${alphavantage.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Cacheable(value = "sectorPerformance")
    public Map<String, Map<String, String>> getSectorPerformance() {
        if (isApiKeyInvalid()) {
            return Collections.singletonMap("error", Collections.singletonMap("message", "API key not configured"));
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(ALPHA_VANTAGE_URL)
                .queryParam("function", "SECTOR")
                .queryParam("apikey", apiKey);

        try {
            String response = restTemplate.getForObject(builder.toUriString(), String.class);
            if (response == null || response.isEmpty()) {
                return Collections.singletonMap("error", Collections.singletonMap("message", "No data received from API for sector performance"));
            }

            // Check for API limit or error messages
            if (response.contains("Error Message") || response.contains("Information") || response.contains("Note")) {
                 // Alpha Vantage sometimes returns a "Note" for the SECTOR endpoint if the API key is free tier.
                 Map<String, Object> errorResponse = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
                 if (errorResponse.containsKey("Error Message")) {
                    return Collections.singletonMap("error", Collections.singletonMap("message", "API Error: " + errorResponse.get("Error Message")));
                 }
                 if (errorResponse.containsKey("Information")) {
                    return Collections.singletonMap("error", Collections.singletonMap("message", "API Info: " + errorResponse.get("Information")));
                 }
                 if (errorResponse.containsKey("Note")) { // Treat API notes as errors for this context, as it usually means limited data
                    return Collections.singletonMap("error", Collections.singletonMap("message", "API Note: " + errorResponse.get("Note")));
                 }
            }

            Map<String, Object> rawData = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
            Map<String, Map<String, String>> result = new LinkedHashMap<>(); // Preserve order of ranks

            for (Map.Entry<String, Object> entry : rawData.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("Rank ")) { // Process only rank categories
                    // Clean up the rank category name (e.g., "Rank A: Real-Time Performance" -> "Real-Time Performance")
                    String cleanedRankName = key.substring(key.indexOf(":") + 1).trim();
                    if (entry.getValue() instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, String> sectors = (Map<String, String>) entry.getValue();
                        result.put(cleanedRankName, sectors);
                    }
                }
            }
            
            if (result.isEmpty() && !rawData.isEmpty()) {
                // This might happen if the response structure is not as expected but not an API error.
                // For example, if there are no "Rank X" keys but other data exists.
                return Collections.singletonMap("error", Collections.singletonMap("message", "Unexpected response format or no sector performance data found."));
            }
             if (result.isEmpty() && rawData.isEmpty()){
                 return Collections.singletonMap("error", Collections.singletonMap("message", "Empty response received from API."));
             }


            return result;

        } catch (IOException e) {
            System.err.println("Error parsing JSON response for sector performance: " + e.getMessage());
            return Collections.singletonMap("error", Collections.singletonMap("message", "Failed to parse sector performance response"));
        } catch (Exception e) {
            System.err.println("Error fetching sector performance: " + e.getMessage());
            return Collections.singletonMap("error", Collections.singletonMap("message", "Failed to fetch sector performance data"));
        }
    }

    private boolean isApiKeyInvalid() {
        return apiKey == null || apiKey.isEmpty() || "YOUR_API_KEY".equals(apiKey);
    }
}
