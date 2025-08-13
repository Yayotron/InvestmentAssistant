package io.yayotron.investmentassistant.prompt;

import io.yayotron.investmentassistant.model.OllamaRAG;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatConsoleTest {

    @Mock
    private OllamaRAG ollamaRAG;

    private ChatConsole chatConsole;

    @BeforeEach
    void setUp() {
        chatConsole = new ChatConsole(ollamaRAG);
    }

    @Test
    void givenMultiLinePrompt_itsReadUntilEOP() {
        String input = "Hello Ollama\nHello Ollama2\n<<EOP>>\nexit\n";
        InputStream in = new ByteArrayInputStream(input.getBytes());
        System.setIn(in);

        when(ollamaRAG.askOllama(anyString())).thenReturn("Response from Ollama");

        chatConsole.run(mock(ApplicationArguments.class));

        verify(ollamaRAG).askOllama("Hello Ollama\nHello Ollama2\n");
    }
}