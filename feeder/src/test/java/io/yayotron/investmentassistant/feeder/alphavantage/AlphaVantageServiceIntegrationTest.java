package io.yayotron.investmentassistant.feeder.alphavantage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest(properties = "alphaVantage.url=http://localhost:1080/query")
@ActiveProfiles("test")
class AlphaVantageServiceIntegrationTest {

    @Autowired
    private AlphaVantageService alphaVantageService;

    private ClientAndServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = startClientAndServer(1080);
    }

    @AfterEach
    void tearDown() {
        mockServer.stop();
    }

    @Test
    @DisplayName("Given a valid stock symbol, when getStockData is called, then the company profile is returned")
    void givenValidStockSymbol_whenGetStockDataIsCalled_thenCompanyProfileIsReturned() throws IOException {
        // given
        String symbol = "IBM";
        String mockJsonResponse = new String(Objects.requireNonNull(this.getClass().getResourceAsStream("/json/alphavantage/success.json")).readAllBytes(), StandardCharsets.UTF_8);

        mockServer
                .when(request()
                        .withMethod("GET")
                        .withQueryStringParameter("function", "GLOBAL_QUOTE")
                        .withQueryStringParameter("symbol", symbol)
                )
                .respond(response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockJsonResponse)
                );

        // when
        Map<String, String> stockData = alphaVantageService.getStockData(symbol);

        // then
        assertEquals("IBM", stockData.get("symbol"));
        assertEquals("167.5000", stockData.get("price"));
        assertEquals("1234567", stockData.get("volume"));
        assertEquals("2024-03-15", stockData.get("latest trading day"));
        assertFalse(stockData.containsKey("error"));
    }
}
