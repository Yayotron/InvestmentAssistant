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
                I am a software engineer.
                I earn 10 000 PLN a month
                I have a spare 1000 per month
                I am interested in buying a new house in Poland
                                
                My goal is to do it in 1 year time.
                """));
    }
}