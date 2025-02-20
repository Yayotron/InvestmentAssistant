package io.yayotron.investmentassistant.prompt;

import io.yayotron.investmentassistant.model.OllamaRAG;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
public class ChatConsole implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(ChatConsole.class);

    private final OllamaRAG ollamaRAG;

    public ChatConsole(OllamaRAG ollamaRAG) {
        this.ollamaRAG = ollamaRAG;
    }

    @Override
    public void run(ApplicationArguments args) {
        String query = "";
        while (!query.contentEquals("exit")) {
            Scanner myObj = new Scanner(System.in);
            System.out.print(">>>");
            query = myObj.nextLine();
            logger.info("Asking Ollama: {}", query);
            try {
                String response = ollamaRAG.askOllama(query);
                logger.info("Answer from Ollama: {}", response);
            } catch (Exception e) {
                logger.error("Error while asking Ollama: {}", e.getMessage());
            }
        }
    }
}