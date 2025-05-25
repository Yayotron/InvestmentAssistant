package io.yayotron.investmentassistant.feeder.financials;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.stream.Collectors;

@Service
public class FinancialsService {

    private static final String ALPHA_VANTAGE_URL = "https://www.alphavantage.co/query";
    private final String apiKey;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final List<String> OVERVIEW_METRICS = Arrays.asList(
        "Symbol", "MarketCapitalization", "EBITDA", "PERatio", "EPS",
        "RevenuePerShareTTM", "GrossProfitTTM", "DilutedEPSTTM",
        "QuarterlyEarningsGrowthYOY", "QuarterlyRevenueGrowthYOY",
        "AnalystTargetPrice", "TrailingPE", "ForwardPE", "PriceToSalesRatioTTM",
        "PriceToBookRatio", "EVToRevenue", "EVToEBITDA", "Beta",
        "SharesOutstanding", "DividendYield", "ProfitMargin", "ReturnOnEquityTTM",
        "ReturnOnAssetsTTM", "DebtToEquity", "CurrentRatio", "BookValue"
    );

    public FinancialsService(@Value("${alphavantage.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Cacheable(value = "companyOverview", key = "#symbol")
    public Map<String, String> getCompanyOverview(String symbol) {
        if (isApiKeyInvalid()) {
            return Collections.singletonMap("error", "API key not configured");
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(ALPHA_VANTAGE_URL)
                .queryParam("function", "OVERVIEW")
                .queryParam("symbol", symbol)
                .queryParam("apikey", apiKey);

        try {
            String response = restTemplate.getForObject(builder.toUriString(), String.class);
            if (response == null || response.isEmpty()) {
                return Collections.singletonMap("error", "No data received from API for company overview");
            }
            // Check for API limit or error messages
            if (response.contains("Error Message") || response.contains("Information")) {
                 Map<String, Object> errorResponse = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
                 if (errorResponse.containsKey("Error Message")) {
                    return Collections.singletonMap("error", "API Error: " + errorResponse.get("Error Message"));
                 }
                 if (errorResponse.containsKey("Information")) {
                    return Collections.singletonMap("error", "API Info: " + errorResponse.get("Information"));
                 }
            }


            Map<String, String> fullOverview = objectMapper.readValue(response, new TypeReference<Map<String, String>>() {});
            
            if (fullOverview.isEmpty() || (fullOverview.containsKey("Symbol") && fullOverview.get("Symbol") == null) ) {
                 return Collections.singletonMap("error", "No overview data found for symbol: " + symbol + ". The symbol may be invalid or delisted.");
            }

            Map<String, String> result = new HashMap<>();
            for (String metric : OVERVIEW_METRICS) {
                if (fullOverview.containsKey(metric) && fullOverview.get(metric) != null && !fullOverview.get(metric).equals("None") && !fullOverview.get(metric).isEmpty()) {
                    result.put(metric, fullOverview.get(metric));
                } else {
                    result.put(metric, "N/A");
                }
            }
            return result;

        } catch (IOException e) {
            System.err.println("Error parsing JSON response for company overview (" + symbol + "): " + e.getMessage());
            return Collections.singletonMap("error", "Failed to parse company overview response");
        } catch (Exception e) {
            System.err.println("Error fetching company overview for " + symbol + ": " + e.getMessage());
            return Collections.singletonMap("error", "Failed to fetch company overview data");
        }
    }

    @Cacheable(value = "earningsData", key = "#symbol")
    public Map<String, Object> getEarningsData(String symbol) {
        if (isApiKeyInvalid()) {
            return Collections.singletonMap("error", "API key not configured");
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(ALPHA_VANTAGE_URL)
                .queryParam("function", "EARNINGS")
                .queryParam("symbol", symbol)
                .queryParam("apikey", apiKey);

        try {
            String response = restTemplate.getForObject(builder.toUriString(), String.class);
             if (response == null || response.isEmpty()) {
                return Collections.singletonMap("error", "No data received from API for earnings data");
            }
            // Check for API limit or error messages
            if (response.contains("Error Message") || response.contains("Information")) {
                 Map<String, Object> errorResponse = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
                 if (errorResponse.containsKey("Error Message")) {
                    return Collections.singletonMap("error", "API Error: " + errorResponse.get("Error Message"));
                 }
                 if (errorResponse.containsKey("Information")) {
                    return Collections.singletonMap("error", "API Info: " + errorResponse.get("Information"));
                 }
            }
            
            Map<String, Object> rawEarningsData = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});

            if (rawEarningsData.isEmpty() || (rawEarningsData.containsKey("symbol") && rawEarningsData.get("symbol") == null) ) {
                 return Collections.singletonMap("error", "No earnings data found for symbol: " + symbol + ". The symbol may be invalid or delisted.");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("symbol", rawEarningsData.getOrDefault("symbol", symbol));
            
            if (rawEarningsData.containsKey("annualEarnings")) {
                result.put("annualEarnings", extractEarningsDetails((List<Map<String, String>>) rawEarningsData.get("annualEarnings")));
            } else {
                result.put("annualEarnings", Collections.emptyList());
            }

            if (rawEarningsData.containsKey("quarterlyEarnings")) {
                result.put("quarterlyEarnings", extractEarningsDetails((List<Map<String, String>>) rawEarningsData.get("quarterlyEarnings")));
            } else {
                result.put("quarterlyEarnings", Collections.emptyList());
            }
            
            return result;

        } catch (IOException e) {
            System.err.println("Error parsing JSON response for earnings data (" + symbol + "): " + e.getMessage());
            return Collections.singletonMap("error", "Failed to parse earnings data response");
        } catch (Exception e) {
            System.err.println("Error fetching earnings data for " + symbol + ": " + e.getMessage());
            return Collections.singletonMap("error", "Failed to fetch earnings data");
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
