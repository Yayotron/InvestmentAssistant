package io.yayotron.investmentassistant.ingestor;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import io.yayotron.investmentassistant.crawler.LocalDocumentCrawler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.List;

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
