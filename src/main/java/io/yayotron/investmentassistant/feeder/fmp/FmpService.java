package io.yayotron.investmentassistant.feeder.fmp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.yayotron.investmentassistant.feeder.common.ApiErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(FmpService.class);
    private static final String FMP_API_URL_V4 = "https://financialmodelingprep.com/api/v4";
    private final String apiKey;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ApiErrorHandler apiErrorHandler;

    public FmpService(@Value("${financialmodelingprep.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.apiErrorHandler = new ApiErrorHandler(); // Instantiate directly
    }

    @Cacheable(value = "fmpSectorPE", key = "#exchange")
    public Map<String, Object> getSectorPERatios(String exchange) {
        if (isApiKeyInvalid()) {
            logger.warn("FMP API key is not configured.");
            // This service returns Map<String, Object> but error handler returns Map<String, String>.
            // Need to adapt or make error handler more generic.
            // For now, wrapping the error map.
            return Collections.singletonMap("error", apiErrorHandler.createSingletonErrorResponse("FMP API key not configured", logger));
        }

        String currentDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        logger.info("Fetching FMP sector P/E ratios for exchange: {}, date: {}", exchange, currentDate);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(FMP_API_URL_V4 + "/sector_price_earning_ratio")
                .queryParam("date", currentDate)
                .queryParam("exchange", exchange)
                .queryParam("apikey", apiKey);

        try {
            String response = restTemplate.getForObject(builder.toUriString(), String.class);
            if (response == null || response.isEmpty()) {
                 logger.warn("No response received from FMP for sector P/E, exchange: {}, date: {}", exchange, currentDate);
                return Collections.singletonMap("error", apiErrorHandler.createSingletonErrorResponse("No data received from FMP API for sector P/E ratios", logger));
            }

            List<Map<String, Object>> rawData = objectMapper.readValue(response, new TypeReference<List<Map<String, Object>>>() {});
            if (rawData.isEmpty()) {
                logger.warn("Empty data array received for FMP sector P/E ratios for exchange: {}, date: {}", exchange, currentDate);
                return Collections.singletonMap("error", apiErrorHandler.createErrorResponse("Empty data array for sector P/E ratios", String.format("Exchange: %s, Date: %s", exchange, currentDate), logger));
            }
            
            if (rawData.size() == 1 && rawData.get(0).containsKey("Error Message")) {
                 String apiErrorMessage = (String) rawData.get(0).get("Error Message");
                 logger.warn("FMP API Error for sector P/E (exchange: {}, date: {}): {}", exchange, currentDate, apiErrorMessage);
                 return Collections.singletonMap("error", apiErrorHandler.createErrorResponse("FMP API Error", apiErrorMessage, logger));
            }

            Map<String, Object> result = rawData.stream()
                    .filter(item -> item.containsKey("sector") && item.containsKey("pe") && item.get("pe") != null)
                    .map(item -> {
                        try {
                            return Map.entry((String) item.get("sector"), Double.parseDouble(String.valueOf(item.get("pe"))));
                        } catch (NumberFormatException e) {
                            logger.warn("Could not parse P/E value '{}' for sector '{}'", item.get("pe"), item.get("sector"));
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1));
            logger.info("Successfully fetched FMP sector P/E ratios for exchange: {}, date: {}", exchange, currentDate);
            return result;

        } catch (HttpClientErrorException e) {
            return apiErrorHandler.createErrorResponse(String.format("FMP API client error for sector P/E (exchange: %s)", exchange), e.getResponseBodyAsString(), logger);
        } catch (IOException e) {
            return apiErrorHandler.createErrorResponse(String.format("Failed to parse FMP sector P/E response (exchange: %s)", exchange), logger, e);
        } catch (Exception e) {
            return apiErrorHandler.createErrorResponse(String.format("Failed to fetch FMP sector P/E data (exchange: %s)", exchange), logger, e);
        }
    }

    @Cacheable(value = "fmpIndustryPE", key = "#exchange")
    public Map<String, Object> getIndustryPERatios(String exchange) {
        if (isApiKeyInvalid()) {
            logger.warn("FMP API key is not configured.");
            return apiErrorHandler.createSingletonErrorResponse("FMP API key not configured", logger);
        }

        String currentDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        logger.info("Fetching FMP industry P/E ratios for exchange: {}, date: {}", exchange, currentDate);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(FMP_API_URL_V4 + "/industry_price_earning_ratio")
                .queryParam("date", currentDate)
                .queryParam("exchange", exchange)
                .queryParam("apikey", apiKey);
        try {
            String response = restTemplate.getForObject(builder.toUriString(), String.class);
            if (response == null || response.isEmpty()) {
                logger.warn("No response received from FMP for industry P/E, exchange: {}", exchange);
                return apiErrorHandler.createSingletonErrorResponse("No data received from FMP API for industry P/E ratios", logger);
            }

            List<Map<String, Object>> rawData = objectMapper.readValue(response, new TypeReference<List<Map<String, Object>>>() {});
            if (rawData.isEmpty()) {
                logger.warn("Empty data array received for FMP industry P/E ratios for exchange: {}", exchange);
                return apiErrorHandler.createErrorResponse("Empty data array for industry P/E ratios", "Exchange: " + exchange, logger);
            }

            if (rawData.size() == 1 && rawData.get(0).containsKey("Error Message")) {
                String apiErrorMessage = (String) rawData.get(0).get("Error Message");
                logger.warn("FMP API Error for industry P/E (exchange: {}): {}", exchange, apiErrorMessage);
                return apiErrorHandler.createErrorResponse("FMP API Error", apiErrorMessage, logger);
            }

            return rawData.stream()
                    .filter(item -> item.containsKey("industry") && item.containsKey("pe") && item.get("pe") != null)
                    .map(item -> {
                        try {
                            return Map.entry((String) item.get("industry"), Double.parseDouble(String.valueOf(item.get("pe"))));
                        } catch (NumberFormatException e) {
                            logger.warn("Could not parse P/E value '{}' for industry '{}'", item.get("pe"), item.get("industry"));
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1));

        } catch (HttpClientErrorException e) {
            return apiErrorHandler.createErrorResponse(String.format("FMP API client error for industry P/E (exchange: %s)", exchange), e.getResponseBodyAsString(), logger);
        } catch (IOException e) {
            return apiErrorHandler.createErrorResponse(String.format("Failed to parse FMP industry P/E response (exchange: %s)", exchange), logger, e);
        } catch (Exception e) {
            return apiErrorHandler.createErrorResponse(String.format("Failed to fetch FMP industry P/E data (exchange: %s)", exchange), logger, e);
        }
    }

    private boolean isApiKeyInvalid() {
        return apiKey == null || apiKey.isEmpty() || "YOUR_FMP_API_KEY".equals(apiKey);
    }
}
