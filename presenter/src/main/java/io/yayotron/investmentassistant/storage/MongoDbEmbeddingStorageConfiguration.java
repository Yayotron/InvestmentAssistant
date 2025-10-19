package io.yayotron.investmentassistant.storage;

import com.mongodb.client.MongoClient;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.mongodb.MongoDbEmbeddingStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(EmbeddingModel.class)
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }
}
