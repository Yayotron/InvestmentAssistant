package com.capgemini.investmentassistant.prompt;

import org.springframework.stereotype.Component;

@Component
public class InvestmentAnalystPromptEnricher implements SystemPromptEnricher {

    @Override
    public String enrichPrompt(String prompt) {
        return """
                You are an investment analyst.
                Your task is to perform a price forecast for the available financial instruments for the next 3 months.
                """
                + prompt;
    }
}
