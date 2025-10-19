package io.yayotron.investmentassistant.storage;

import com.mongodb.client.MongoClient;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.mongodb.MongoDbEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoDbEmbeddingStorageConfiguration {

    private final MongoClient mongoClient;

    public MongoDbEmbeddingStorageConfiguration(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return MongoDbEmbeddingStore.builder()
                .fromClient(mongoClient)
                .databaseName("ai")
                .collectionName("internal_data_files")
                .indexName("embeddings")
                .createIndex(true)
                .build();
    }
}
