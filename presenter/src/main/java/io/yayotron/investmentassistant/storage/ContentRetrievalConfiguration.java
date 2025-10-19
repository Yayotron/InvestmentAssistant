package io.yayotron.investmentassistant.storage;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.router.LanguageModelQueryRouter;
import dev.langchain4j.rag.query.transformer.CompressingQueryTransformer;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.Executors;

@Configuration
public class ContentRetrievalConfiguration {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final ChatModel chatModel;

    public ContentRetrievalConfiguration(EmbeddingStore<TextSegment> embeddingStore,
                                         EmbeddingModel embeddingModel,
                                         ChatModel chatModel) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
        this.chatModel = chatModel;
    }

    @Bean
    public RetrievalAugmentor retrievalAugmentor(CompressingQueryTransformer searchQueryTransformer,
                                                 Map<ContentRetriever, String> contentRetrieverUsage) {
        return DefaultRetrievalAugmentor.builder()
                .queryTransformer(searchQueryTransformer)
                .queryRouter(new LanguageModelQueryRouter(chatModel, contentRetrieverUsage))
                .executor(Executors.newFixedThreadPool(4))
                .build();
    }

    @Bean
    public CompressingQueryTransformer searchQueryTransformer(
            @Value("${prompt.transformer.template}") String transformerTemplate) {
        return new CompressingQueryTransformer(
                chatModel, new PromptTemplate(transformerTemplate)
        );
    }

    @Bean
    public Map<ContentRetriever, String> contentRetrieverUsage(
            @Value("${prompt.embedding.description}") String embeddingRetrieverDescription
    ) {
        return Map.of(
                embeddingContentRetriever(), embeddingRetrieverDescription
        );
    }

    @Bean
    public ContentRetriever embeddingContentRetriever() {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .displayName("EmbeddingContentRetriever")
                .build();
    }
}
