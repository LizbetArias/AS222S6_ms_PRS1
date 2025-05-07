package pe.edu.vallegrande.report_service.controller;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import pe.edu.vallegrande.report_service.service.ReportService;
import reactor.core.publisher.Flux;

public class ReportControllerTest {

    @Test
    void testGetAllReports_shouldReturnOk() {
        // Mock del servicio
        ReportService mockService = Mockito.mock(ReportService.class);

        // Retorna lista vacía para simplificar
        Mockito.when(mockService.findFilteredReports(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()
        )).thenReturn(Flux.empty());

        // Crear instancia del controlador con el servicio mockeado
        ReportController controller = new ReportController(mockService);

        // Cliente web para testear el endpoint
        WebTestClient client = WebTestClient.bindToController(controller).build();

        // Simulación de petición GET sin filtros
        client.get().uri("/api/reports")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk(); // ✅ esperamos 200 OK
    }
}
