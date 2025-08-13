package io.yayotron.investmentassistant.prompt;

@FunctionalInterface
public interface PromptEnricher {
    String enrichPrompt(String prompt);
}
