package io.yayotron.investmentassistant.presenter.health;

import io.yayotron.investmentassistant.feeder.financials.FinancialsService;
import io.yayotron.investmentassistant.feeder.fmp.FmpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CompanyHealthScorecardService {

    private static final Logger logger = LoggerFactory.getLogger(CompanyHealthScorecardService.class);

    private final FinancialsService financialsService;
    private final FmpService fmpService; // Will be used later, injected now

    public CompanyHealthScorecardService(FinancialsService financialsService, FmpService fmpService) {
        this.financialsService = financialsService;
        this.fmpService = fmpService;
    }

    // --- Data Structures ---
    public record ScorecardResult(double score, List<String> keyPositives, List<String> keyNegatives, String summaryMessage) {}
    public record MetricScore(String metricName, double score, String assessment, String valueRepresentation) {}

    // --- Helper method for parsing ---
    private Double parseDoubleMetric(String metricValue, String metricName, String symbol) {
        if (metricValue == null || "N/A".equalsIgnoreCase(metricValue) || metricValue.isEmpty()) {
            logger.debug("Metric '{}' for symbol {} is N/A or null/empty.", metricName, symbol);
            return null;
        }
        try {
            // Remove percentage signs if present for metrics like growth rates
            return Double.parseDouble(metricValue.replace("%", ""));
        } catch (NumberFormatException e) {
            logger.warn("Could not parse metric '{}' for symbol {}. Value: '{}'. Error: {}", metricName, symbol, metricValue, e.getMessage());
            return null;
        }
    }

    @Cacheable("companyHealthScore")
    public ScorecardResult calculateHealthScore(String symbol) {
        logger.info("Calculating health scorecard for symbol: {}", symbol);

        Map<String, Object> overviewData = financialsService.getCompanyOverview(symbol);
        if (overviewData.containsKey("error")) {
            String errorMsg = overviewData.get("error").toString();
            String errorDetails = overviewData.get("details").toString();
            String combinedError = "Error fetching company overview: " + errorMsg + (errorDetails != null ? " (Details: " + errorDetails + ")" : "");
            logger.error("Cannot calculate health score for {}: {}", symbol, combinedError);
            return new ScorecardResult(0, Collections.emptyList(), Collections.emptyList(), combinedError);
        }

        List<MetricScore> metricScores = new ArrayList<>();
        double totalScore = 0.0;

        // --- Profitability Metrics (40%) ---
        Double profitMargin = parseDoubleMetric(overviewData.get("ProfitMargin").toString(), "Profit Margin", symbol);
        if (profitMargin != null) {
            double score = 0;
            String assessment;
            if (profitMargin > 0.15) { score = 20; assessment = "Excellent"; }
            else if (profitMargin > 0.05) { score = 10; assessment = "Good"; }
            else if (profitMargin > 0) { score = 5; assessment = "Fair"; }
            else { score = 0; assessment = "Poor"; }
            metricScores.add(new MetricScore("Profit Margin", score, assessment, String.format("%.2f%%", profitMargin * 100)));
            totalScore += score;
        } else {
             metricScores.add(new MetricScore("Profit Margin", 0, "N/A", "N/A"));
        }


        Double returnOnEquity = parseDoubleMetric(overviewData.get("ReturnOnEquityTTM").toString(), "Return on Equity (TTM)", symbol);
        if (returnOnEquity != null) {
            double score = 0;
            String assessment;
            if (returnOnEquity > 0.15) { score = 20; assessment = "Excellent"; }
            else if (returnOnEquity > 0.05) { score = 10; assessment = "Good"; }
            else if (returnOnEquity > 0) { score = 5; assessment = "Fair"; }
            else { score = 0; assessment = "Poor"; }
            metricScores.add(new MetricScore("Return on Equity (TTM)", score, assessment, String.format("%.2f%%", returnOnEquity * 100)));
            totalScore += score;
        } else {
            metricScores.add(new MetricScore("Return on Equity (TTM)", 0, "N/A", "N/A"));
        }

        // --- Liquidity & Debt Metrics (30%) ---
        Double currentRatio = parseDoubleMetric(overviewData.get("CurrentRatio").toString(), "Current Ratio", symbol);
        if (currentRatio != null) {
            double score = 0;
            String assessment;
            if (currentRatio > 1.5) { score = 15; assessment = "Good"; }
            else if (currentRatio > 1.0) { score = 7.5; assessment = "Acceptable"; }
            else { score = 0; assessment = "Poor"; }
            metricScores.add(new MetricScore("Current Ratio", score, assessment, String.format("%.2f", currentRatio)));
            totalScore += score;
        } else {
            metricScores.add(new MetricScore("Current Ratio", 0, "N/A", "N/A"));
        }

        Double debtToEquity = parseDoubleMetric(overviewData.get("DebtToEquity").toString(), "Debt to Equity", symbol);
        if (debtToEquity != null) {
            double score = 0;
            String assessment;
            if (debtToEquity < 0.5) { score = 15; assessment = "Low"; } // Lower is better
            else if (debtToEquity < 1.0) { score = 7.5; assessment = "Moderate"; }
            else { score = 0; assessment = "High"; }
            metricScores.add(new MetricScore("Debt to Equity", score, assessment, String.format("%.2f", debtToEquity)));
            totalScore += score;
        } else {
            metricScores.add(new MetricScore("Debt to Equity", 0, "N/A", "N/A"));
        }

        // --- Growth Metrics (30%) ---
        // Values from Alpha Vantage for growth are usually direct percentages, e.g., "0.15" for 15%
        Double quarterlyRevenueGrowth = parseDoubleMetric(overviewData.get("QuarterlyRevenueGrowthYOY").toString(), "Quarterly Revenue Growth (YOY)", symbol);
        if (quarterlyRevenueGrowth != null) {
            double score = 0;
            String assessment;
            if (quarterlyRevenueGrowth > 0.10) { score = 15; assessment = "Strong"; }
            else if (quarterlyRevenueGrowth > 0) { score = 7.5; assessment = "Moderate"; }
            else { score = 0; assessment = "Low/Negative"; }
            metricScores.add(new MetricScore("Quarterly Revenue Growth (YOY)", score, assessment, String.format("%.2f%%", quarterlyRevenueGrowth * 100)));
            totalScore += score;
        } else {
            metricScores.add(new MetricScore("Quarterly Revenue Growth (YOY)", 0, "N/A", "N/A"));
        }

        Double quarterlyEarningsGrowth = parseDoubleMetric(overviewData.get("QuarterlyEarningsGrowthYOY").toString(), "Quarterly Earnings Growth (YOY)", symbol);
        if (quarterlyEarningsGrowth != null) {
            double score = 0;
            String assessment;
            if (quarterlyEarningsGrowth > 0.10) { score = 15; assessment = "Strong"; }
            else if (quarterlyEarningsGrowth > 0) { score = 7.5; assessment = "Moderate"; }
            else { score = 0; assessment = "Low/Negative"; }
            metricScores.add(new MetricScore("Quarterly Earnings Growth (YOY)", score, assessment, String.format("%.2f%%", quarterlyEarningsGrowth * 100)));
            totalScore += score;
        } else {
            metricScores.add(new MetricScore("Quarterly Earnings Growth (YOY)", 0, "N/A", "N/A"));
        }
        
        // --- Generate Summary ---
        List<String> keyPositives = new ArrayList<>();
        List<String> keyNegatives = new ArrayList<>();

        for (MetricScore ms : metricScores) {
            if (ms.score() > 0) { // Simplified: any positive score is a "positive" point for summary
                 // More nuanced positive/negative thresholds could be set per metric type
                if (Objects.equals(ms.metricName(), "Debt to Equity")) { // Lower is better for D/E
                    if ("Low".equals(ms.assessment()) || "Moderate".equals(ms.assessment())) {
                        keyPositives.add(String.format("%s: %s (%s)", ms.metricName(), ms.valueRepresentation(), ms.assessment()));
                    } else if ("High".equals(ms.assessment())) {
                         keyNegatives.add(String.format("%s: %s (%s)", ms.metricName(), ms.valueRepresentation(), ms.assessment()));
                    }
                } else { // Higher is generally better for others
                    if ("Excellent".equals(ms.assessment()) || "Good".equals(ms.assessment()) || "Strong".equals(ms.assessment())) {
                        keyPositives.add(String.format("%s: %s (%s)", ms.metricName(), ms.valueRepresentation(), ms.assessment()));
                    } else if ("Poor".equals(ms.assessment()) || "Low/Negative".equals(ms.assessment())) {
                         keyNegatives.add(String.format("%s: %s (%s)", ms.metricName(), ms.valueRepresentation(), ms.assessment()));
                    }
                }
            } else if ("N/A".equals(ms.assessment())) {
                 // Optionally add N/A metrics to a separate list or ignore in summary
            } else { // Score is 0, indicating a negative aspect
                 keyNegatives.add(String.format("%s: %s (%s)", ms.metricName(), ms.valueRepresentation(), ms.assessment()));
            }
        }


        String summaryMessage;
        if (totalScore >= 70) summaryMessage = "Overall financial health: Strong.";
        else if (totalScore >= 50) summaryMessage = "Overall financial health: Moderate.";
        else if (totalScore > 0) summaryMessage = "Overall financial health: Fair, with areas for improvement.";
        else summaryMessage = "Overall financial health: Weak, requires careful review.";
        
        if (metricScores.stream().allMatch(ms -> "N/A".equals(ms.assessment()))) {
            summaryMessage = "Could not calculate health score due to missing data for all metrics.";
        }


        logger.info("Calculated health score for {}: {}", symbol, totalScore);
        return new ScorecardResult(totalScore, keyPositives, keyNegatives, summaryMessage);
    }
}
