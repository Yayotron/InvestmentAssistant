package io.yayotron.investmentassistant.prompt;

import io.yayotron.investmentassistant.model.RAGConfiguration;
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
    private RAGConfiguration RAGConfiguration;

    private ChatConsole chatConsole;

    @BeforeEach
    void setUp() {
        chatConsole = new ChatConsole(RAGConfiguration);
    }

    @Test
    void givenMultiLinePrompt_itsReadUntilEOP() {
        String input = "Hello Ollama\nHello Ollama2\n<<EOP>>\nexit\n";
        InputStream in = new ByteArrayInputStream(input.getBytes());
        System.setIn(in);

        when(RAGConfiguration.ask(anyString())).thenReturn("Response from Ollama");

        chatConsole.run(mock(ApplicationArguments.class));

        verify(RAGConfiguration).ask("Hello Ollama\nHello Ollama2\n");
    }
}