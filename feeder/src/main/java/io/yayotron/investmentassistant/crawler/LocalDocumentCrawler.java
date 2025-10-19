package io.yayotron.investmentassistant.crawler;

import dev.langchain4j.data.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.FileNotFoundException;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.stream.Stream;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocuments;

@Service
public class LocalDocumentCrawler {

    private static final Logger logger = LoggerFactory.getLogger(LocalDocumentCrawler.class);

    public List<Document> crawlDocuments() {
        try {
            List<Document> allDocuments = Stream.concat(loadDocuments(
                                    ResourceUtils.getFile("classpath:data/").toPath(),
                                    pathMatcher(".csv")
                            ).stream(),
                            loadDocuments(
                                    ResourceUtils.getFile("classpath:data/").toPath(),
                                    pathMatcher(".pdf")
                            ).stream())
                    .toList();

            logger.info("Crawled {} documents from local storage", allDocuments.size());

            return allDocuments;
        } catch (FileNotFoundException e) {
            logger.error("Error loading documents", e);
            throw new RuntimeException(e);
        }
    }

    private PathMatcher pathMatcher(String fileExtension) {
        return path -> path.getFileName().toString().endsWith(fileExtension);
    }
}
