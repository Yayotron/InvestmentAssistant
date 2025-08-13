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
    public static final String END_OF_PROMPT_TOKEN = "<<EOP>>";

    private final OllamaRAG ollamaRAG;

    public ChatConsole(OllamaRAG ollamaRAG) {
        this.ollamaRAG = ollamaRAG;
    }

    @Override
    public void run(ApplicationArguments args) {
        String query = "";
        while (!query.contentEquals("exit")) {
            Scanner scanner = new Scanner(System.in);
            System.out.print(">>>");

            query = readAllLines(scanner);
            logger.info("Asking Ollama: {}", query);
            try {
                String response = ollamaRAG.askOllama(query);
                logger.info("Answer from Ollama: {}", response);
            } catch (Exception e) {
                logger.error("Error while asking Ollama: {}", e.getMessage());
            }
        }
    }

    private static String readAllLines(Scanner scanner) {
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while (!(line = scanner.nextLine()).equalsIgnoreCase(END_OF_PROMPT_TOKEN)) {
            stringBuilder.append(line).append(System.lineSeparator());
        }
        return stringBuilder.toString();
    }
}