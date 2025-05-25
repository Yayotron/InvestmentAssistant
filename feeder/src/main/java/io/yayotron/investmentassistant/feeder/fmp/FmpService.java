package io.yayotron.investmentassistant.feeder.fmp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FmpService {

    private static final String FMP_API_URL_V4 = "https://financialmodelingprep.com/api/v4";
    private final String apiKey;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public FmpService(@Value("${financialmodelingprep.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Cacheable(value = "fmpSectorPE", key = "#exchange")
    public Map<String, Object> getSectorPERatios(String exchange) {
        if (isApiKeyInvalid()) {
            return Collections.singletonMap("error", "FMP API key not configured");
        }

        String currentDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(FMP_API_URL_V4 + "/sector_price_earning_ratio")
                .queryParam("date", currentDate)
                .queryParam("exchange", exchange)
                .queryParam("apikey", apiKey);

        try {
            String response = restTemplate.getForObject(builder.toUriString(), String.class);
            if (response == null || response.isEmpty()) {
                return Collections.singletonMap("error", "No data received from FMP API for sector P/E ratios");
            }

            List<Map<String, Object>> rawData = objectMapper.readValue(response, new TypeReference<List<Map<String, Object>>>() {});
            if (rawData.isEmpty()) {
                return Collections.singletonMap("error", "Empty data array received for sector P/E ratios for exchange: " + exchange + " on date: " + currentDate);
            }
            
            // Check for FMP's specific error message structure, which might be inside a list if the API call itself was "successful" but returned an error object.
            if (rawData.size() == 1 && rawData.get(0).containsKey("Error Message")) {
                 return Collections.singletonMap("error", "FMP API Error: " + rawData.get(0).get("Error Message"));
            }


            return rawData.stream()
                    .filter(item -> item.containsKey("sector") && item.containsKey("pe") && item.get("pe") != null)
                    .collect(Collectors.toMap(
                            item -> (String) item.get("sector"),
                            item -> {
                                Object peValue = item.get("pe");
                                if (peValue instanceof Number) {
                                    return ((Number) peValue).doubleValue();
                                }
                                try {
                                    return Double.parseDouble(String.valueOf(peValue));
                                } catch (NumberFormatException e) {
                                    return null; // Or some other indicator of unparseable PE
                                }
                            }
                    ));

        } catch (HttpClientErrorException e) {
            System.err.println("FMP API error for sector P/E (" + exchange + "): " + e.getResponseBodyAsString());
            return Collections.singletonMap("error", "FMP API client error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (IOException e) {
            System.err.println("Error parsing JSON response for FMP sector P/E (" + exchange + "): " + e.getMessage());
            return Collections.singletonMap("error", "Failed to parse FMP sector P/E response");
        } catch (Exception e) {
            System.err.println("Error fetching FMP sector P/E for " + exchange + ": " + e.getMessage());
            return Collections.singletonMap("error", "Failed to fetch FMP sector P/E data");
        }
    }

    @Cacheable(value = "fmpIndustryPE", key = "#exchange")
    public Map<String, Object> getIndustryPERatios(String exchange) {
        if (isApiKeyInvalid()) {
            return Collections.singletonMap("error", "FMP API key not configured");
        }

        String currentDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(FMP_API_URL_V4 + "/industry_price_earning_ratio")
                .queryParam("date", currentDate)
                .queryParam("exchange", exchange)
                .queryParam("apikey", apiKey);
        try {
            String response = restTemplate.getForObject(builder.toUriString(), String.class);
            if (response == null || response.isEmpty()) {
                return Collections.singletonMap("error", "No data received from FMP API for industry P/E ratios");
            }

            List<Map<String, Object>> rawData = objectMapper.readValue(response, new TypeReference<List<Map<String, Object>>>() {});
             if (rawData.isEmpty()) {
                return Collections.singletonMap("error", "Empty data array received for industry P/E ratios for exchange: " + exchange + " on date: " + currentDate);
            }

            if (rawData.size() == 1 && rawData.get(0).containsKey("Error Message")) {
                 return Collections.singletonMap("error", "FMP API Error: " + rawData.get(0).get("Error Message"));
            }

            return rawData.stream()
                    .filter(item -> item.containsKey("industry") && item.containsKey("pe") && item.get("pe") != null)
                    .collect(Collectors.toMap(
                            item -> (String) item.get("industry"),
                             item -> {
                                Object peValue = item.get("pe");
                                if (peValue instanceof Number) {
                                    return ((Number) peValue).doubleValue();
                                }
                                try {
                                    return Double.parseDouble(String.valueOf(peValue));
                                } catch (NumberFormatException e) {
                                    return null; 
                                }
                            }
                    ));
        } catch (HttpClientErrorException e) {
            System.err.println("FMP API error for industry P/E (" + exchange + "): " + e.getResponseBodyAsString());
            return Collections.singletonMap("error", "FMP API client error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (IOException e) {
            System.err.println("Error parsing JSON response for FMP industry P/E (" + exchange + "): " + e.getMessage());
            return Collections.singletonMap("error", "Failed to parse FMP industry P/E response");
        } catch (Exception e) {
            System.err.println("Error fetching FMP industry P/E for " + exchange + ": " + e.getMessage());
            return Collections.singletonMap("error", "Failed to fetch FMP industry P/E data");
        }
    }

    private boolean isApiKeyInvalid() {
        return apiKey == null || apiKey.isEmpty() || "YOUR_FMP_API_KEY".equals(apiKey);
    }
}
