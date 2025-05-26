package io.yayotron.investmentassistant.presenter.tool;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolP;
import io.yayotron.investmentassistant.presenter.health.CompanyHealthScorecardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CompanyHealthTool {

    private static final Logger logger = LoggerFactory.getLogger(CompanyHealthTool.class);
    private final CompanyHealthScorecardService companyHealthScorecardService;

    public CompanyHealthTool(CompanyHealthScorecardService companyHealthScorecardService) {
        this.companyHealthScorecardService = companyHealthScorecardService;
    }

    @Tool("Gets a financial health scorecard for a given company, including an overall score, key positives, and key negatives.")
    public String getCompanyHealthScorecard(@ToolP("The stock symbol of the company, e.g., AAPL, MSFT") String symbol) {
        logger.info("Fetching company health scorecard for symbol: {}", symbol);

        try {
            CompanyHealthScorecardService.ScorecardResult scorecard = companyHealthScorecardService.calculateHealthScore(symbol);

            // Check if the summary message itself indicates an error returned from the service
            // (e.g., "Could not calculate health score for XYZ: Error fetching company overview...")
            if (scorecard.summaryMessage() != null && 
                (scorecard.summaryMessage().startsWith("Could not calculate health score") || scorecard.summaryMessage().startsWith("Error fetching"))) {
                logger.warn("Health scorecard calculation for symbol {} resulted in an error: {}", symbol, scorecard.summaryMessage());
                return scorecard.summaryMessage(); // Return the error summary directly
            }
            
            if (scorecard.score() == 0 && scorecard.keyPositives().isEmpty() && scorecard.keyNegatives().isEmpty() && scorecard.summaryMessage().contains("missing data for all metrics")){
                 logger.warn("Health scorecard for symbol {} could not be generated due to missing data.", symbol);
                return scorecard.summaryMessage(); // "Could not calculate health score due to missing data for all metrics."
            }


            StringBuilder sb = new StringBuilder();
            sb.append(scorecard.summaryMessage()).append("\n");
            sb.append(String.format("Score: %.1f/100\n", scorecard.score()));

            if (scorecard.keyPositives() != null && !scorecard.keyPositives().isEmpty()) {
                sb.append("Key Positives:\n");
                scorecard.keyPositives().forEach(p -> sb.append("- ").append(p).append("\n"));
            } else {
                sb.append("Key Positives: None identified or data insufficient.\n");
            }

            if (scorecard.keyNegatives() != null && !scorecard.keyNegatives().isEmpty()) {
                sb.append("Key Negatives:\n");
                scorecard.keyNegatives().forEach(n -> sb.append("- ").append(n).append("\n"));
            } else {
                sb.append("Key Negatives: None identified or data sufficient.\n");
            }
            
            logger.info("Successfully generated health scorecard for symbol: {}", symbol);
            return sb.toString().trim();

        } catch (Exception e) {
            logger.error("Unexpected error while fetching company health scorecard for symbol {}: {}", symbol, e.getMessage(), e);
            return String.format("An unexpected error occurred while generating the health scorecard for %s.", symbol);
        }
    }
}
