package io.yayotron.investmentassistant.prompt;

import org.springframework.stereotype.Component;

@Component
public class LimitationPromptEnricher implements SystemPromptEnricher {

    @Override
    public String enrichPrompt(String prompt) {
        return """
                Consider investment in stock market and cryptocurrencies only.
                There will be a steady monthly investment of up to 1000 PLN per month.
                Consider different markets in different countries.
                User can only use Revolut or Binance for investments.
                When answering, user is only interested in specific instruments.
                """
                + prompt;
    }
}
