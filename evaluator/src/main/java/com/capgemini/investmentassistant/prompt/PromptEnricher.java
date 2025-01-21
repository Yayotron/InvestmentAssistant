package com.capgemini.investmentassistant.prompt;

@FunctionalInterface
public interface PromptEnricher {
    String enrichPrompt(String prompt);
}
