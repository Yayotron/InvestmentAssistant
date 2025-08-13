package io.yayotron.investmentassistant.feeder.financials;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FinancialsService {

    private static final Logger logger = LoggerFactory.getLogger(FinancialsService.class);
    private static final String ALPHA_VANTAGE_URL = "https://www.alphavantage.co/query";
    private final String apiKey;
    private final String alphaVantageUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ApiErrorHandler apiErrorHandler;

    private static final List<String> OVERVIEW_METRICS = Arrays.asList(
        "Symbol", "MarketCapitalization", "EBITDA", "PERatio", "EPS",
        "RevenuePerShareTTM", "GrossProfitTTM", "DilutedEPSTTM",
        "QuarterlyEarningsGrowthYOY", "QuarterlyRevenueGrowthYOY",
        "AnalystTargetPrice", "TrailingPE", "ForwardPE", "PriceToSalesRatioTTM",
        "PriceToBookRatio", "EVToRevenue", "EVToEBITDA", "Beta",
        "SharesOutstanding", "DividendYield", "ProfitMargin", "ReturnOnEquityTTM",
        "ReturnOnAssetsTTM", "DebtToEquity", "CurrentRatio", "BookValue"
    );

    public FinancialsService(@Value("${alphavantage.api.key}") String apiKey,
                             @Value("${alphaVantage.url}") String alphaVantageUrl) {
        this.apiKey = apiKey;
        this.alphaVantageUrl = alphaVantageUrl;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.apiErrorHandler = new ApiErrorHandler(); // Instantiate directly
    }

    @Cacheable(value = "companyOverview", key = "#symbol")
    public Map<String, Object> getCompanyOverview(String symbol) {
        if (isApiKeyInvalid()) {
            logger.warn("AlphaVantage API key is not configured for FinancialsService.");
            return apiErrorHandler.createSingletonErrorResponse("API key not configured", logger);
        }

        logger.info("Fetching company overview for symbol: {}", symbol);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(alphaVantageUrl)
                .queryParam("function", "OVERVIEW")
                .queryParam("symbol", symbol)
                .queryParam("apikey", apiKey);

        try {
            String response = restTemplate.getForObject(builder.toUriString(), String.class);
            if (response == null || response.isEmpty()) {
                logger.warn("No response received from AlphaVantage for company overview, symbol {}", symbol);
                return apiErrorHandler.createSingletonErrorResponse("No data received from API for company overview", logger);
            }

            Map<String, Object> rawResponseMap = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
            Optional<Map<String, String>> apiError = apiErrorHandler.handleAlphaVantageError(rawResponseMap, "FinancialsService.getCompanyOverview", symbol, logger);
            if (apiError.isPresent()) {
                return (Map)apiError.get();
            }
            
            // Re-cast to the expected type if no error, assuming it's Map<String, String> for overview
            Map<String, String> fullOverview = objectMapper.convertValue(rawResponseMap, new TypeReference<Map<String, String>>() {});

            if (fullOverview.isEmpty() || (fullOverview.containsKey("Symbol") && fullOverview.get("Symbol") == null)) {
                logger.warn("No company overview data found for symbol: {}. Symbol may be invalid or delisted. Response: {}", symbol, response.substring(0, Math.min(response.length(), 500)));
                return apiErrorHandler.createErrorResponse("No overview data found for symbol: " + symbol, "Symbol may be invalid or delisted.", logger);
            }

            Map<String, Object> result = new HashMap<>();
            for (String metric : OVERVIEW_METRICS) {
                String value = fullOverview.get(metric);
                if (value != null && !value.equals("None") && !value.isEmpty()) {
                    result.put(metric, value);
                } else {
                    logger.trace("Metric '{}' not available or 'None' for symbol {}", metric, symbol);
                    result.put(metric, "N/A");
                }
            }
            logger.info("Successfully fetched company overview for symbol: {}", symbol);
            return result;

        } catch (IOException e) {
            return apiErrorHandler.createErrorResponse("Failed to parse company overview response for symbol " + symbol, logger, e);
        } catch (Exception e) {
            return apiErrorHandler.createErrorResponse("Failed to fetch company overview for symbol " + symbol, logger, e);
        }
    }

    @Cacheable(value = "earningsData", key = "#symbol")
    public Map<String, Object> getEarningsData(String symbol) {
        if (isApiKeyInvalid()) {
            logger.warn("AlphaVantage API key is not configured for FinancialsService.");
            return new HashMap<>(apiErrorHandler.createSingletonErrorResponse("API key not configured", logger));
        }

        logger.info("Fetching earnings data for symbol: {}", symbol);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(ALPHA_VANTAGE_URL)
                .queryParam("function", "EARNINGS")
                .queryParam("symbol", symbol)
                .queryParam("apikey", apiKey);

        try {
            String response = restTemplate.getForObject(builder.toUriString(), String.class);
            if (response == null || response.isEmpty()) {
                logger.warn("No response received from AlphaVantage for earnings data, symbol {}", symbol);
                return new HashMap<>(apiErrorHandler.createSingletonErrorResponse("No data received from API for earnings data", logger));
            }
            
            Map<String, Object> rawEarningsData = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
            Optional<Map<String, String>> apiError = apiErrorHandler.handleAlphaVantageError(rawEarningsData, "FinancialsService.getEarningsData", symbol, logger);
            if (apiError.isPresent()) {
                return new HashMap<>(apiError.get());
            }

            if (rawEarningsData.isEmpty() || (rawEarningsData.containsKey("symbol") && rawEarningsData.get("symbol") == null)) {
                logger.warn("No earnings data found for symbol: {}. Symbol may be invalid or delisted. Response: {}", symbol, response.substring(0, Math.min(response.length(), 500)));
                return new HashMap<>(apiErrorHandler.createErrorResponse("No earnings data found for symbol: " + symbol, "Symbol may be invalid or delisted.", logger));
            }

            Map<String, Object> result = new HashMap<>();
            result.put("symbol", rawEarningsData.getOrDefault("symbol", symbol));
            
            if (rawEarningsData.containsKey("annualEarnings")) {
                result.put("annualEarnings", extractEarningsDetails((List<Map<String, String>>) rawEarningsData.get("annualEarnings")));
            } else {
                logger.debug("No 'annualEarnings' field found for symbol {}", symbol);
                result.put("annualEarnings", Collections.emptyList());
            }

            if (rawEarningsData.containsKey("quarterlyEarnings")) {
                result.put("quarterlyEarnings", extractEarningsDetails((List<Map<String, String>>) rawEarningsData.get("quarterlyEarnings")));
            } else {
                logger.debug("No 'quarterlyEarnings' field found for symbol {}", symbol);
                result.put("quarterlyEarnings", Collections.emptyList());
            }
            
            logger.info("Successfully fetched earnings data for symbol: {}", symbol);
            return result;

        } catch (IOException e) {
            return new HashMap<>(apiErrorHandler.createErrorResponse("Failed to parse earnings data response for symbol " + symbol, logger, e));
        } catch (Exception e) { // Catching broader exceptions
            return new HashMap<>(apiErrorHandler.createErrorResponse("Failed to fetch earnings data for symbol " + symbol, logger, e));
        }
    }

    private List<Map<String, String>> extractEarningsDetails(List<Map<String, String>> earningsList) {
        if (earningsList == null) {
            return Collections.emptyList();
        }
        return earningsList.stream()
            .map(earning -> {
                Map<String, String> details = new HashMap<>();
                details.put("fiscalDateEnding", earning.getOrDefault("fiscalDateEnding", "N/A"));
                details.put("reportedEPS", earning.getOrDefault("reportedEPS", "N/A"));
                return details;
            })
            .collect(Collectors.toList());
    }
    
    private boolean isApiKeyInvalid() {
        // Check against the placeholder value typically found in properties files
        return apiKey == null || apiKey.isEmpty() || "YOUR_API_KEY".equals(apiKey);
    }
}
