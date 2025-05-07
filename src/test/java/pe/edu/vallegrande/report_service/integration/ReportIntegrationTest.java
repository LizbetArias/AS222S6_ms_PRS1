package pe.edu.vallegrande.report_service.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.context.annotation.Import;
import pe.edu.vallegrande.report_service.config.TestSecurityConfig;
import pe.edu.vallegrande.report_service.util.DotenvInitializer;

@SpringBootTest
@AutoConfigureWebTestClient(timeout = "10000")
@TestPropertySource(properties = {
        "spring.r2dbc.url=${DB_URL}",
        "spring.r2dbc.username=${DB_USERNAME}",
        "spring.r2dbc.password=${DB_PASSWORD}"
})
@Import(TestSecurityConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ReportIntegrationTest {

    static {
        // ✅ Cargar .env antes de que el contexto se inicialice
        new DotenvInitializer();
    }

    @Autowired
    private WebTestClient client;

    @Test
    void getReports_shouldReturnOK() {
        client.get().uri("/api/reports")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();
    }
}
