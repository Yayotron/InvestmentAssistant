package io.yayotron.investmentassistant.feeder.common;

import org.slf4j.Logger;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class ApiErrorHandler {

    public Optional<Map<String, Object>> handleAlphaVantageError(Map<String, Object> responseMap, String serviceName, String symbol, Logger logger) {
        if (responseMap == null) {
            return Optional.empty(); // Or handle as an error itself if a null map is unexpected
        }

        String errorMessage = null;
        if (responseMap.containsKey("Error Message")) {
            errorMessage = (String) responseMap.get("Error Message");
        } else if (responseMap.containsKey("Information")) {
            errorMessage = (String) responseMap.get("Information");
        } else if (responseMap.containsKey("Note")) { // Specific to some Alpha Vantage endpoints like SECTOR
            errorMessage = (String) responseMap.get("Note");
        }


        if (errorMessage != null) {
            String logMsg = String.format("Alpha Vantage API error/info in %s for symbol %s: %s", serviceName, symbol, errorMessage);
            logger.warn(logMsg); // Using WARN as these are API-side issues or informational notes we treat as issues
            return Optional.of(Collections.singletonMap("error", String.format("API Error in %s for %s: %s", serviceName, symbol, errorMessage)));
        }
        return Optional.empty();
    }

    public Map<String, Object> createErrorResponse(String message, Logger logger, Exception e) {
        String detailedMessage = (e != null) ? e.getMessage() : "N/A";
        logger.error(message + (e != null ? ": " + detailedMessage : ""), e); // Log with exception stack trace
        return Map.of("error", message, "details", detailedMessage);
    }
    
    public Map<String, Object> createErrorResponse(String message, String details, Logger logger) {
        logger.error(message + ": " + details);
        return Map.of("error", message, "details", details);
    }

    public Map<String, Object> createSingletonErrorResponse(String message, Logger logger) {
        logger.error(message);
        return Collections.singletonMap("error", message);
    }
}
