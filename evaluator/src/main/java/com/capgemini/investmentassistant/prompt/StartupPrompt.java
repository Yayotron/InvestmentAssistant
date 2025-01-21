package com.capgemini.investmentassistant.prompt;

import com.capgemini.investmentassistant.model.OllamaRAG;
import com.capgemini.investmentassistant.model.OllamaSinglePrompt;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupPrompt implements ApplicationRunner {

    private final OllamaRAG ollamaRAG;

    public StartupPrompt(OllamaRAG ollamaRAG) {
        this.ollamaRAG = ollamaRAG;
    }

    @Override
    public void run(ApplicationArguments args) {
        System.out.println(ollamaRAG.askOllama("""
                Where do you think I should invest 500 USD?
                Feel free to make any assumptions to work with the data you have available, values in all documents are in USD$
                Give me a suggestion of what data could help you provide a more precise answer.

                Please provide an explanation of your thought process.
                """));
    }
}