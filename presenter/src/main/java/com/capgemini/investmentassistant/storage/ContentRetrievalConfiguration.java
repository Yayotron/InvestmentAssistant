package com.capgemini.investmentassistant.storage;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.content.retriever.WebSearchContentRetriever;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.rag.query.router.LanguageModelQueryRouter;
import dev.langchain4j.rag.query.transformer.CompressingQueryTransformer;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.google.customsearch.GoogleCustomWebSearchEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ResourceUtils;

import java.io.FileNotFoundException;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Map;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocuments;

@Configuration
public class ContentRetrievalConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ContentRetrievalConfiguration.class);

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final String searchEngineApiKey;
    private final OllamaChatModel ollamaChatModel;

    public ContentRetrievalConfiguration(EmbeddingStore<TextSegment> embeddingStore,
                                         EmbeddingModel embeddingModel,
                                         @Value("${search.api.key}")
                                         String searchEngineApiKey,
                                         OllamaChatModel ollamaChatModel) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
        this.searchEngineApiKey = searchEngineApiKey;
        this.ollamaChatModel = ollamaChatModel;
    }

    @Bean
    public RetrievalAugmentor retrievalAugmentor(CompressingQueryTransformer searchQueryTransformer,
                                                 Map<ContentRetriever, String> contentRetrieverUsage) {
        return DefaultRetrievalAugmentor.builder()
                .queryTransformer(searchQueryTransformer)
                .queryRouter(new LanguageModelQueryRouter(
                        ollamaChatModel,
                        contentRetrieverUsage))
                .build();
    }

    @Bean
    public CompressingQueryTransformer searchQueryTransformer(
            @Value("${prompt.transformer.template}") String transformerTemplate) {
        return new CompressingQueryTransformer(
                ollamaChatModel, new PromptTemplate(transformerTemplate)
        );
    }

    @Bean
    public Map<ContentRetriever, String> contentRetrieverUsage(
            @Value("${prompt.search.description}") String searchRetrieverDescription,
            @Value("${prompt.embedding.description}") String embeddingRetrieverDescription
    ) {
        return Map.of(
                embeddingContentRetriever(), embeddingRetrieverDescription,
                webSearchEngineContentRetriever(), searchRetrieverDescription
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

    @Bean
    public ContentRetriever webSearchEngineContentRetriever() {
        WebSearchEngine webSearchEngine = GoogleCustomWebSearchEngine.builder()
                .csi("017576662512468239146:omuauf_lfve")
                .apiKey(searchEngineApiKey)
                .build();

        return WebSearchContentRetriever.builder()
                .webSearchEngine(webSearchEngine)
                .maxResults(3)
                .build();
    }
}
