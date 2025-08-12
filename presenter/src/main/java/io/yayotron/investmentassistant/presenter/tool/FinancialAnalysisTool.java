package io.yayotron.investmentassistant.presenter.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import io.yayotron.investmentassistant.feeder.financials.FinancialsService;
import io.yayotron.investmentassistant.feeder.fmp.FmpService;
import io.yayotron.investmentassistant.feeder.sector.SectorPerformanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class FinancialAnalysisTool {

    private static final Logger logger = LoggerFactory.getLogger(FinancialAnalysisTool.class);

    private final FinancialsService financialsService;
    private final SectorPerformanceService sectorPerformanceService;
    private final FmpService fmpService;

    public FinancialAnalysisTool(FinancialsService financialsService,
                                 SectorPerformanceService sectorPerformanceService,
                                 FmpService fmpService) {
        this.financialsService = financialsService;
        this.sectorPerformanceService = sectorPerformanceService;
        this.fmpService = fmpService;
    }

    @Tool("Gets a comprehensive financial overview for a given company symbol, including market cap, P/E, EPS, and other key metrics.")
    public String getCompanyFinancialOverview(@P("The stock symbol of the company (e.g., AAPL, MSFT)") String symbol) {
        logger.info("Fetching company financial overview for symbol: {}", symbol);
        Map<String, String> overviewData = financialsService.getCompanyOverview(symbol);

        if (overviewData.containsKey("error")) {
            String errorMsg = overviewData.get("error");
            String errorDetails = overviewData.get("details");
            String combinedError = errorMsg + (errorDetails != null ? " (Details: " + errorDetails + ")" : "");
            logger.error("Error fetching company overview for {}: {}", symbol, combinedError);
            return String.format("Could not retrieve financial overview for %s. Reason: %s", symbol, combinedError);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Financial Overview for %s:\n", overviewData.getOrDefault("Symbol", symbol)));
        appendMetric(sb, "Market Cap", overviewData, "MarketCapitalization");
        appendMetric(sb, "EBITDA", overviewData, "EBITDA");
        appendMetric(sb, "P/E Ratio", overviewData, "PERatio");
        appendMetric(sb, "EPS", overviewData, "EPS");
        appendMetric(sb, "Revenue Per Share (TTM)", overviewData, "RevenuePerShareTTM");
        appendMetric(sb, "Gross Profit (TTM)", overviewData, "GrossProfitTTM");
        appendMetric(sb, "Diluted EPS (TTM)", overviewData, "DilutedEPSTTM");
        appendMetric(sb, "Quarterly Earnings Growth (YOY)", overviewData, "QuarterlyEarningsGrowthYOY");
        appendMetric(sb, "Quarterly Revenue Growth (YOY)", overviewData, "QuarterlyRevenueGrowthYOY");
        appendMetric(sb, "Analyst Target Price", overviewData, "AnalystTargetPrice");
        appendMetric(sb, "Trailing P/E", overviewData, "TrailingPE");
        appendMetric(sb, "Forward P/E", overviewData, "ForwardPE");
        appendMetric(sb, "Price to Sales Ratio (TTM)", overviewData, "PriceToSalesRatioTTM");
        appendMetric(sb, "Price to Book Ratio", overviewData, "PriceToBookRatio");
        appendMetric(sb, "EV to Revenue", overviewData, "EVToRevenue");
        appendMetric(sb, "EV to EBITDA", overviewData, "EVToEBITDA");
        appendMetric(sb, "Beta", overviewData, "Beta");
        appendMetric(sb, "Shares Outstanding", overviewData, "SharesOutstanding");
        appendMetric(sb, "Dividend Yield", overviewData, "DividendYield");
        appendMetric(sb, "Profit Margin", overviewData, "ProfitMargin");
        appendMetric(sb, "Return On Equity (TTM)", overviewData, "ReturnOnEquityTTM");
        appendMetric(sb, "Return On Assets (TTM)", overviewData, "ReturnOnAssetsTTM");
        appendMetric(sb, "Debt To Equity", overviewData, "DebtToEquity");
        appendMetric(sb, "Current Ratio", overviewData, "CurrentRatio");
        appendMetric(sb, "Book Value", overviewData, "BookValue");

        return sb.toString().trim();
    }

    private void appendMetric(StringBuilder sb, String displayName, Map<String, String> data, String dataKey) {
        sb.append(String.format("%s: %s\n", displayName, data.getOrDefault(dataKey, "N/A")));
    }

    @Tool("Gets a summary of annual and quarterly earnings (EPS) for a given company symbol.")
    public String getCompanyEarningsSummary(@P("The stock symbol of the company (e.g., AAPL, MSFT)") String symbol) {
        logger.info("Fetching company earnings summary for symbol: {}", symbol);
        Map<String, Object> earningsData = financialsService.getEarningsData(symbol);

        if (earningsData.containsKey("error")) {
            // Assuming error structure from FinancialsService when using ApiErrorHandler for Object return types
            Object errorObj = earningsData.get("error");
            String errorMsg;
            if (errorObj instanceof Map) { // Error from ApiErrorHandler wrapped in a map by the service
                 Map<String, String> errorMap = (Map<String, String>) errorObj;
                 errorMsg = errorMap.get("error") + (errorMap.containsKey("details") ? " (Details: " + errorMap.get("details") + ")" : "");
            } else if (errorObj instanceof String) { // Direct error string
                 errorMsg = (String) errorObj;
            } else {
                errorMsg = "Unknown error structure.";
            }
            logger.error("Error fetching company earnings for {}: {}", symbol, errorMsg);
            return String.format("Could not retrieve earnings summary for %s. Reason: %s", symbol, errorMsg);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Earnings Summary for %s:\n", earningsData.getOrDefault("symbol", symbol)));

        List<Map<String, String>> annualEarnings = (List<Map<String, String>>) earningsData.get("annualEarnings");
        sb.append("Annual Earnings:\n");
        if (annualEarnings != null && !annualEarnings.isEmpty()) {
            annualEarnings.stream().limit(3).forEach(earning ->
                sb.append(String.format("- %s: EPS $%s\n",
                    earning.getOrDefault("fiscalDateEnding", "N/A"),
                    earning.getOrDefault("reportedEPS", "N/A")))
            );
        } else {
            sb.append("- No annual earnings data available.\n");
        }

        List<Map<String, String>> quarterlyEarnings = (List<Map<String, String>>) earningsData.get("quarterlyEarnings");
        sb.append("Quarterly Earnings:\n");
        if (quarterlyEarnings != null && !quarterlyEarnings.isEmpty()) {
            quarterlyEarnings.stream().limit(5).forEach(earning -> // Limit to 5 for brevity
                sb.append(String.format("- %s: EPS $%s\n",
                    earning.getOrDefault("fiscalDateEnding", "N/A"),
                    earning.getOrDefault("reportedEPS", "N/A")))
            );
        } else {
            sb.append("- No quarterly earnings data available.\n");
        }
        return sb.toString().trim();
    }

    @Tool("Gets a summary of current sector performance across different timeframes.")
    public String getSectorPerformanceSummary() {
        logger.info("Fetching sector performance summary.");
        Map<String, Map<String, String>> performanceData = sectorPerformanceService.getSectorPerformance();

        if (performanceData.containsKey("error")) {
            Map<String, String> errorMap = performanceData.get("error"); // SectorPerformanceService nests the error map
            String errorMsg = errorMap.get("error");
            String errorDetails = errorMap.get("details");
            String combinedError = errorMsg + (errorDetails != null ? " (Details: " + errorDetails + ")" : "");
            logger.error("Error fetching sector performance: {}", combinedError);
            return "Could not retrieve sector performance. Reason: " + combinedError;
        }

        StringBuilder sb = new StringBuilder("Sector Performance:\n");
        if (performanceData.isEmpty()) {
            return "No sector performance data available at the moment.";
        }
        performanceData.forEach((rankCategory, sectors) -> {
            sb.append(String.format("%s:\n", rankCategory));
            sectors.forEach((sector, performance) ->
                sb.append(String.format("- %s: %s\n", sector, performance))
            );
        });
        return sb.toString().trim();
    }

    @Tool("Gets the average P/E ratio for a specific sector on a given stock exchange and lists P/E for other sectors.")
    public String getSectorPeerComparison(
            @P("The sector to get P/E ratio for (e.g., Technology, Healthcare)") String sector,
            @P("The stock exchange (e.g., NASDAQ, NYSE)") String exchange) {
        logger.info("Fetching sector P/E comparison for sector {} on exchange {}", sector, exchange);
        Map<String, Object> sectorPEData = fmpService.getSectorPERatios(exchange);

        if (sectorPEData.containsKey("error")) {
             Map<String, String> errorMap = (Map<String, String>) sectorPEData.get("error");
            String errorMsg = errorMap.get("error");
            String errorDetails = errorMap.get("details");
            String combinedError = errorMsg + (errorDetails != null ? " (Details: " + errorDetails + ")" : "");
            logger.error("Error fetching sector P/E data for exchange {}: {}", exchange, combinedError);
            return String.format("Could not retrieve sector P/E data for %s. Reason: %s", exchange, combinedError);
        }
        
        if (sectorPEData.isEmpty()) {
            return String.format("No sector P/E data found for exchange %s.", exchange);
        }

        String targetSectorKey = sectorPEData.keySet().stream()
                .filter(s -> s.equalsIgnoreCase(sector))
                .findFirst()
                .orElse(null);

        StringBuilder sb = new StringBuilder();
        if (targetSectorKey != null) {
            sb.append(String.format("Average P/E Ratio for %s sector on %s: %s.\n",
                    targetSectorKey, exchange, sectorPEData.get(targetSectorKey)));
        } else {
            sb.append(String.format("Sector P/E data not found for %s on %s.\n", sector, exchange));
        }

        sb.append("Other sectors on ").append(exchange).append(":\n");
        sectorPEData.forEach((sec, pe) -> {
            if (targetSectorKey == null || !sec.equalsIgnoreCase(targetSectorKey)) {
                sb.append(String.format("- %s: %s\n", sec, pe));
            }
        });

        return sb.toString().trim();
    }

    @Tool("Gets the average P/E ratio for a specific industry on a given stock exchange.")
    public String getIndustryPeerComparison(
            @P("The industry to get P/E ratio for (e.g., Software, Pharmaceuticals)") String industry,
            @P("The stock exchange (e.g., NASDAQ, NYSE)") String exchange) {
        logger.info("Fetching industry P/E comparison for industry {} on exchange {}", industry, exchange);
        Map<String, Object> industryPEData = fmpService.getIndustryPERatios(exchange);

        if (industryPEData.containsKey("error")) {
            Map<String, String> errorMap = (Map<String, String>) industryPEData.get("error");
            String errorMsg = errorMap.get("error");
            String errorDetails = errorMap.get("details");
            String combinedError = errorMsg + (errorDetails != null ? " (Details: " + errorDetails + ")" : "");
            logger.error("Error fetching industry P/E data for exchange {}: {}", exchange, combinedError);
            return String.format("Could not retrieve industry P/E data for %s. Reason: %s", exchange, combinedError);
        }
        
        if (industryPEData.isEmpty()) {
            return String.format("No industry P/E data found for exchange %s.", exchange);
        }

        String targetIndustryKey = industryPEData.keySet().stream()
                .filter(i -> i.equalsIgnoreCase(industry))
                .findFirst()
                .orElse(null);

        if (targetIndustryKey != null) {
            return String.format("Average P/E Ratio for %s industry on %s: %s.",
                    targetIndustryKey, exchange, industryPEData.get(targetIndustryKey));
        } else {
            return String.format("Industry P/E data not found for %s on %s. Available industries: %s",
                    industry, exchange, industryPEData.keySet().stream().collect(Collectors.joining(", ")));
        }
    }
}
