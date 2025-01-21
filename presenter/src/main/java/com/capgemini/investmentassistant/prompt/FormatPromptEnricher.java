package com.capgemini.investmentassistant.prompt;

import org.springframework.stereotype.Component;

@Component
public class FormatPromptEnricher implements SystemPromptEnricher {

    @Override
    public String enrichPrompt(String prompt) {
        return "Please respond in plain text format.\n" + prompt;
    }
}
