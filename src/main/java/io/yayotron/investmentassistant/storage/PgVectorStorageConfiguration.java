package io.yayotron.investmentassistant.storage;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

@Profile("!test")
@Configuration
public class PgVectorStorageConfiguration {

    private final DataSource dataSource;

    public PgVectorStorageConfiguration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(EmbeddingModel embeddingModel) {
        return PgVectorEmbeddingStore.datasourceBuilder()
                .datasource(dataSource)
                .table("ai.internal_data_files")
                .createTable(true)
                .dimension(embeddingModel.dimension())
                .build();
    }
}
