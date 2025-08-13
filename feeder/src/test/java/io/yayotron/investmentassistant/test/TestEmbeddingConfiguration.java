package io.yayotron.investmentassistant.test;

import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestEmbeddingConfiguration {

    @Bean
    @Primary
    public EmbeddingStore<?> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }
}
