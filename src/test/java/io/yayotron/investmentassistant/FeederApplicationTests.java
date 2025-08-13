package io.yayotron.investmentassistant;

import io.yayotron.investmentassistant.test.TestEmbeddingConfiguration;
import org.junit.jupiter.api.Test;
import io.yayotron.investmentassistant.ingestor.DocumentIngestorRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = {"alphaVantage.url=http://localhost:1080/query"})
@Import(TestEmbeddingConfiguration.class)
class FeederApplicationTests {

	@MockBean
	private DocumentIngestorRunner documentIngestorRunner;

	@Test
	void contextLoads() {
	}

}
