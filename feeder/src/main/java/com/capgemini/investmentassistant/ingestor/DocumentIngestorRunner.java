package com.capgemini.investmentassistant.ingestor;

import com.capgemini.investmentassistant.crawler.LocalDocumentCrawler;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.FileNotFoundException;
import java.nio.file.PathMatcher;
import java.util.List;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocuments;

@Service
public class DocumentIngestorRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DocumentIngestorRunner.class);

    private final LocalDocumentCrawler localDocumentCrawler;
    private final EmbeddingStoreIngestor ingestor;

    public DocumentIngestorRunner(LocalDocumentCrawler localDocumentCrawler,
                                  EmbeddingStoreIngestor ingestor) {
        this.localDocumentCrawler = localDocumentCrawler;
        this.ingestor = ingestor;
    }


    @Override
    public void run(String... args) {
        List<Document> documents = localDocumentCrawler.crawlDocuments();
        ingestor.ingest(documents);
        logger.info("Successfully ingested {} documents", documents.size());
    }

}
