package pe.edu.vallegrande.report_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import pe.edu.vallegrande.report_service.dto.ReportDto;
import pe.edu.vallegrande.report_service.dto.ReportWithWorkshopsDto;
import pe.edu.vallegrande.report_service.dto.ReportWorkshopDto;
import pe.edu.vallegrande.report_service.model.Report;
import pe.edu.vallegrande.report_service.model.ReportWorkshop;
import pe.edu.vallegrande.report_service.model.WorkshopCache;
import pe.edu.vallegrande.report_service.repository.ReportRepository;
import pe.edu.vallegrande.report_service.repository.ReportWorkshopRepository;
import pe.edu.vallegrande.report_service.repository.WorkshopCacheRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ReportServiceTest {

    private ReportService reportService;
    private ReportRepository reportRepository;
    private ReportWorkshopRepository reportWorkshopRepository;
    private WorkshopCacheRepository workshopCacheRepository;
    private SupabaseStorageService supabaseStorageService;

    @BeforeEach
    void setUp() {
        reportRepository = Mockito.mock(ReportRepository.class);
        reportWorkshopRepository = Mockito.mock(ReportWorkshopRepository.class);
        workshopCacheRepository = Mockito.mock(WorkshopCacheRepository.class);
        supabaseStorageService = Mockito.mock(SupabaseStorageService.class);

        reportService = new ReportService(
                reportRepository,
                reportWorkshopRepository,
                workshopCacheRepository,
                supabaseStorageService
        );
    }

    @Test
    void testFindFilteredReportsWithoutFilters() {
        // Arrange
        Report report1 = createSampleReport(1, 2023, "Q1", "Descripción 1", "A");
        Report report2 = createSampleReport(2, 2023, "Q2", "Descripción 2", "A");

        ReportWorkshop workshop1 = createSampleWorkshop(1, 1, 101, "Taller 1",
                null, null, "Descripción taller 1");
        ReportWorkshop workshop2 = createSampleWorkshop(2, 2, 102, "Taller 2",
                null, null, "Descripción taller 2");

        WorkshopCache cache1 = new WorkshopCache();
        cache1.setId(101);
        cache1.setName("Taller 1 desde cache");
        cache1.setDateStart(LocalDate.of(2023, 1, 1));
        cache1.setDateEnd(LocalDate.of(2023, 3, 31));
        cache1.setStatus("A");

        WorkshopCache cache2 = new WorkshopCache();
        cache2.setId(102);
        cache2.setName("Taller 2 desde cache");
        cache2.setDateStart(LocalDate.of(2023, 4, 1));
        cache2.setDateEnd(LocalDate.of(2023, 6, 30));
        cache2.setStatus("A");

        when(reportRepository.findAll()).thenReturn(Flux.just(report1, report2));
        when(reportWorkshopRepository.findByReportId(1)).thenReturn(Flux.just(workshop1));
        when(reportWorkshopRepository.findByReportId(2)).thenReturn(Flux.just(workshop2));
        when(workshopCacheRepository.findById(101)).thenReturn(Mono.just(cache1));
        when(workshopCacheRepository.findById(102)).thenReturn(Mono.just(cache2));

        // Act & Assert
        StepVerifier.create(reportService.findFilteredReports(null, null, null, null, null))
                .expectNextCount(2)
                .verifyComplete();
    }


    @Test
    void testFindFilteredReportsWithStatusFilter() {
        // Arrange
        Report report1 = createSampleReport(1, 2023, "Q1", "Descripción 1", "A");

        ReportWorkshop workshop1 = createSampleWorkshop(1, 1, 101, "Taller 1",
                null, null, "Descripción taller 1");

        WorkshopCache cache1 = new WorkshopCache();
        cache1.setId(101);
        cache1.setName("Taller 1 desde cache");
        cache1.setDateStart(LocalDate.of(2023, 1, 1));
        cache1.setDateEnd(LocalDate.of(2023, 3, 31));
        cache1.setStatus("A");

        when(reportRepository.findByStatus("A")).thenReturn(Flux.just(report1));
        when(reportWorkshopRepository.findByReportId(1)).thenReturn(Flux.just(workshop1));
        when(workshopCacheRepository.findById(101)).thenReturn(Mono.just(cache1));

        // Act & Assert
        StepVerifier.create(reportService.findFilteredReports("A", null, null, null, null))
                .expectNextMatches(reportWithWorkshops ->
                        reportWithWorkshops.getReport().getStatus().equals("A") &&
                                reportWithWorkshops.getWorkshops().size() == 1
                )
                .verifyComplete();
    }

    @Test
    void testFindFilteredReportsWithAllFilters() {
        // Arrange
        Report report1 = createSampleReport(1, 2023, "Q2", "Descripción 1", "A");

        // Taller asociado al reporte, con workshopId
        ReportWorkshop workshop1 = createSampleWorkshop(1, 1, 101, "Taller 1",
                null, null, "Descripción taller 1");

        // Simula que hay cache del taller
        WorkshopCache cache = new WorkshopCache();
        cache.setId(101);
        cache.setName("Taller desde cache");
        cache.setDateStart(LocalDate.of(2023, 4, 10)); // Está dentro del filtro
        cache.setDateEnd(LocalDate.of(2023, 5, 20));   // Está dentro del filtro
        cache.setStatus("A");

        when(reportRepository.findByStatus("A")).thenReturn(Flux.just(report1));
        when(reportWorkshopRepository.findByReportId(1)).thenReturn(Flux.just(workshop1));
        when(workshopCacheRepository.findById(101)).thenReturn(Mono.just(cache));

        LocalDate startDate = LocalDate.of(2023, 4, 1);
        LocalDate endDate = LocalDate.of(2023, 6, 30);

        // Act & Assert
        StepVerifier.create(reportService.findFilteredReports("A", "Q2", 2023, startDate, endDate))
                .expectNextMatches(reportWithWorkshops ->
                        reportWithWorkshops.getReport().getStatus().equals("A") &&
                                reportWithWorkshops.getReport().getTrimester().equals("Q2") &&
                                reportWithWorkshops.getReport().getYear().equals(2023) &&
                                reportWithWorkshops.getWorkshops().size() == 1 &&
                                reportWithWorkshops.getWorkshops().get(0).getWorkshopName().equals("Taller desde cache") &&
                                reportWithWorkshops.getWorkshops().get(0).getWorkshopDateStart().equals(LocalDate.of(2023, 4, 10)) &&
                                reportWithWorkshops.getWorkshops().get(0).getWorkshopDateEnd().equals(LocalDate.of(2023, 5, 20))
                )
                .verifyComplete();
    }

    @Test
    void testFindByIdWithDateFilter() {
        // Arrange
        Report report1 = createSampleReport(1, 2023, "Q1", "Descripción 1", "A");

        ReportWorkshop workshop1 = createSampleWorkshop(1, 1, 101, "Taller 1",
                null, null, "Descripción taller 1");

        WorkshopCache cache1 = new WorkshopCache();
        cache1.setId(101);
        cache1.setName("Taller 1 desde cache");
        cache1.setDateStart(LocalDate.of(2023, 1, 15));
        cache1.setDateEnd(LocalDate.of(2023, 2, 28));
        cache1.setStatus("A");

        when(reportRepository.findById(1)).thenReturn(Mono.just(report1));
        when(reportWorkshopRepository.findByReportId(1)).thenReturn(Flux.just(workshop1));
        when(workshopCacheRepository.findById(101)).thenReturn(Mono.just(cache1));

        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 3, 31);

        // Act & Assert
        StepVerifier.create(reportService.findByIdWithDateFilter(1, startDate, endDate))
                .expectNextMatches(reportWithWorkshops ->
                        reportWithWorkshops.getReport().getId().equals(1) &&
                                reportWithWorkshops.getWorkshops().size() == 1
                )
                .verifyComplete();
    }


    @Test
    void testCreate() {
        // Arrange
        ReportDto reportDto = new ReportDto();
        reportDto.setYear(2023);
        reportDto.setTrimester("Q3");
        reportDto.setDescription("Nuevo reporte");
        reportDto.setSchedule("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA"); // Base64 image

        ReportWorkshopDto workshopDto = new ReportWorkshopDto();
        workshopDto.setWorkshopName("Nuevo taller");
        workshopDto.setWorkshopDateStart(LocalDate.of(2023, 7, 1));
        workshopDto.setWorkshopDateEnd(LocalDate.of(2023, 9, 30));
        workshopDto.setDescription("Descripción del nuevo taller");
        workshopDto.setImageUrl(new String[]{"data:image/jpeg;base64,/9j/4AAQSkZJR"}); // Base64 image

        ReportWithWorkshopsDto requestDto = new ReportWithWorkshopsDto();
        requestDto.setReport(reportDto);
        requestDto.setWorkshops(List.of(workshopDto));

        Report savedReport = createSampleReport(1, 2023, "Q3", "Nuevo reporte", "A");
        savedReport.setSchedule("https://storage.example.com/reports/schedules/schedule123.png");

        ReportWorkshop savedWorkshop = createSampleWorkshop(1, 1, null, "Nuevo taller",
                LocalDate.of(2023, 7, 1), LocalDate.of(2023, 9, 30), "Descripción del nuevo taller");
        savedWorkshop.setImageUrl(new String[]{"https://storage.example.com/reports/workshops/image123.jpg"});

        when(supabaseStorageService.uploadBase64Image(eq("reports/schedules"), anyString()))
                .thenReturn(Mono.just("https://storage.example.com/reports/schedules/schedule123.png"));
        when(supabaseStorageService.uploadBase64Image(eq("reports/workshops"), anyString()))
                .thenReturn(Mono.just("https://storage.example.com/reports/workshops/image123.jpg"));
        when(reportRepository.save(any(Report.class))).thenReturn(Mono.just(savedReport));
        when(reportWorkshopRepository.saveAll(anyList())).thenReturn(Flux.just(savedWorkshop));

        // Act & Assert
        StepVerifier.create(reportService.create(requestDto))
                .expectNextMatches(result ->
                        result.getReport().getYear().equals(2023) &&
                                result.getReport().getTrimester().equals("Q3") &&
                                result.getReport().getStatus().equals("A") &&
                                result.getWorkshops().size() == 1 &&
                                result.getWorkshops().get(0).getWorkshopName().equals("Nuevo taller")
                )
                .verifyComplete();
    }

    @Test
    void testUpdate() {
        // Arrange
        Integer reportId = 1;

        ReportDto reportDto = new ReportDto();
        reportDto.setId(reportId);
        reportDto.setYear(2023);
        reportDto.setTrimester("Q3");
        reportDto.setDescription("Reporte actualizado");
        reportDto.setSchedule("https://storage.example.com/reports/schedules/old-schedule.png");

        ReportWorkshopDto workshopDto = new ReportWorkshopDto();
        workshopDto.setId(1);
        workshopDto.setReportId(reportId);
        workshopDto.setWorkshopName("Taller actualizado");
        workshopDto.setWorkshopDateStart(LocalDate.of(2023, 7, 1));
        workshopDto.setWorkshopDateEnd(LocalDate.of(2023, 9, 30));
        workshopDto.setDescription("Descripción actualizada");
        workshopDto.setImageUrl(new String[]{"https://storage.example.com/reports/workshops/image123.jpg"});

        ReportWithWorkshopsDto requestDto = new ReportWithWorkshopsDto();
        requestDto.setReport(reportDto);
        requestDto.setWorkshops(List.of(workshopDto));

        Report existingReport = createSampleReport(reportId, 2023, "Q2", "Descripción original", "A");
        existingReport.setSchedule("https://storage.example.com/reports/schedules/old-schedule.png");

        ReportWorkshop existingWorkshop = createSampleWorkshop(1, reportId, null, "Taller original",
                LocalDate.of(2023, 4, 1), LocalDate.of(2023, 6, 30), "Descripción original");
        existingWorkshop.setImageUrl(new String[]{"https://storage.example.com/reports/workshops/old-image.jpg", "https://storage.example.com/reports/workshops/to-be-deleted.jpg"});

        Report updatedReport = createSampleReport(reportId, 2023, "Q3", "Reporte actualizado", "A");
        updatedReport.setSchedule("https://storage.example.com/reports/schedules/old-schedule.png");

        ReportWorkshop updatedWorkshop = createSampleWorkshop(1, reportId, null, "Taller actualizado",
                LocalDate.of(2023, 7, 1), LocalDate.of(2023, 9, 30), "Descripción actualizada");
        updatedWorkshop.setImageUrl(new String[]{"https://storage.example.com/reports/workshops/image123.jpg"});

        when(reportRepository.findById(reportId)).thenReturn(Mono.just(existingReport));
        when(reportWorkshopRepository.findByReportId(reportId)).thenReturn(Flux.just(existingWorkshop));
        when(reportRepository.save(any(Report.class))).thenReturn(Mono.just(updatedReport));
        when(reportWorkshopRepository.saveAll(anyList())).thenReturn(Flux.just(updatedWorkshop));
        when(supabaseStorageService.deleteImage(anyString())).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(reportService.update(reportId, requestDto))
                .expectNextMatches(result ->
                        result.getReport().getYear().equals(2023) &&
                                result.getReport().getTrimester().equals("Q3") &&
                                result.getReport().getDescription().equals("Reporte actualizado") &&
                                result.getWorkshops().size() == 1 &&
                                result.getWorkshops().get(0).getWorkshopName().equals("Taller actualizado")
                )
                .verifyComplete();

        // Verify that deleteImage was called for the image that was removed
        verify(supabaseStorageService).deleteImage("https://storage.example.com/reports/workshops/to-be-deleted.jpg");
    }

    @Test
    void testRestore() {
        // Arrange
        Integer reportId = 1;
        Report inactiveReport = createSampleReport(reportId, 2023, "Q1", "Reporte inactivo", "I");
        Report activatedReport = createSampleReport(reportId, 2023, "Q1", "Reporte inactivo", "A");

        when(reportRepository.findById(reportId)).thenReturn(Mono.just(inactiveReport));
        when(reportRepository.save(any(Report.class))).thenReturn(Mono.just(activatedReport));

        // Act & Assert
        StepVerifier.create(reportService.restore(reportId))
                .verifyComplete();

        // Verify that save was called with the report having status "A"
        verify(reportRepository).save(argThat(report -> report.getStatus().equals("A")));
    }

    @Test
    void testDeleteLogic() {
        // Arrange
        Integer reportId = 1;
        Report activeReport = createSampleReport(reportId, 2023, "Q1", "Reporte activo", "A");
        Report inactivatedReport = createSampleReport(reportId, 2023, "Q1", "Reporte activo", "I");

        when(reportRepository.findById(reportId)).thenReturn(Mono.just(activeReport));
        when(reportRepository.save(any(Report.class))).thenReturn(Mono.just(inactivatedReport));

        // Act & Assert
        StepVerifier.create(reportService.deleteLogic(reportId))
                .verifyComplete();

        // Verify that save was called with the report having status "I"
        verify(reportRepository).save(argThat(report -> report.getStatus().equals("I")));
    }

    @Test
    void testExistsByYearAndTrimester_Exists() {
        // Arrange
        Integer year = 2023;
        String trimester = "Q1";
        Report report = createSampleReport(1, year, trimester, "Reporte existente", "A");

        when(reportRepository.findByYearAndTrimester(year, trimester)).thenReturn(Flux.just(report));

        // Act & Assert
        StepVerifier.create(reportService.existsByYearAndTrimester(year, trimester))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void testExistsByYearAndTrimester_DoesNotExist() {
        // Arrange
        Integer year = 2023;
        String trimester = "Q4";

        when(reportRepository.findByYearAndTrimester(year, trimester)).thenReturn(Flux.empty());

        // Act & Assert
        StepVerifier.create(reportService.existsByYearAndTrimester(year, trimester))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void testFilterWorkshopByDate_InRange() {
        // Arrange
        ReportWorkshop workshop = createSampleWorkshop(1, 1, null, "Taller en rango",
                LocalDate.of(2023, 2, 1), LocalDate.of(2023, 2, 28), "Descripción");

        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 3, 31);

        // Use reflection to access private method for testing
        StepVerifier.create(invokeFilterWorkshopByDate(workshop, startDate, endDate))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void testFilterWorkshopByDate_OutOfRange() {
        // Arrange
        ReportWorkshop workshop = createSampleWorkshop(1, 1, null, "Taller fuera de rango",
                LocalDate.of(2023, 4, 1), LocalDate.of(2023, 4, 30), "Descripción");

        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 3, 31);

        // Use reflection to access private method for testing
        StepVerifier.create(invokeFilterWorkshopByDate(workshop, startDate, endDate))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void generatePdfByIdWithDateFilter_shouldGeneratePdf() {
        // Arrange
        int reportId = 1;
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 3, 31);

        // ✅ Cargar imagen local desde resources
        String imagePath = getClass().getClassLoader().getResource("images/mi-logo.png").toExternalForm();

        Report mockReport = Report.builder()
                .id(reportId)
                .year(2024)
                .trimester("Enero-Marzo")
                .description("Test Report")
                .status("ACTIVE")
                .schedule(imagePath) // ✅ aquí debe ir una URL válida, no solo "horario.jpg"
                .build();

        ReportWorkshop mockWorkshop = ReportWorkshop.builder()
                .id(10)
                .reportId(reportId)
                .workshopName("Taller de prueba")
                .description("Descripción")
                .workshopDateStart(start)
                .workshopDateEnd(end)
                .imageUrl(new String[]{imagePath})
                .build();

        when(reportRepository.findById(reportId)).thenReturn(Mono.just(mockReport));
        when(reportWorkshopRepository.findByReportId(reportId)).thenReturn(Flux.just(mockWorkshop));
        when(supabaseStorageService.uploadPdf(any(), any(), any())).thenReturn(Mono.empty());
        when(supabaseStorageService.fileExists(any(), any())).thenReturn(Mono.just(false));

        // Act
        Mono<ResponseEntity<byte[]>> result = reportService.generatePdfByIdWithDateFilter(reportId, start, end);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(response ->
                        response.getStatusCode() == HttpStatus.OK &&
                                response.getHeaders().getContentType().equals(MediaType.APPLICATION_PDF) &&
                                response.getBody().length > 0
                )
                .verifyComplete();
    }

    @Test
    void testFilterWorkshopByDate_WithWorkshopId() {
        // Arrange
        Integer workshopId = 101;
        ReportWorkshop workshop = createSampleWorkshop(1, 1, workshopId, "Taller con ID",
                null, null, "Descripción"); // Fechas nulas, se tomarán del cache

        WorkshopCache workshopCache = new WorkshopCache();
        workshopCache.setId(workshopId);
        workshopCache.setName("Taller desde cache");
        workshopCache.setDateStart(LocalDate.of(2023, 2, 1));
        workshopCache.setDateEnd(LocalDate.of(2023, 2, 28));
        // No seteamos el status porque no se usará

        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 3, 31);

        // Simula el repositorio
        when(workshopCacheRepository.findById(workshopId)).thenReturn(Mono.just(workshopCache));

        // Usa lógica similar a la interna del método privado
        Mono<ReportWorkshopDto> resultMono = workshopCacheRepository.findById(workshopId)
                .filter(cache -> {
                    try {
                        Method method = ReportService.class.getDeclaredMethod("isInDateRange",
                                LocalDate.class, LocalDate.class, LocalDate.class, LocalDate.class);
                        method.setAccessible(true);
                        return (boolean) method.invoke(reportService, cache.getDateStart(), cache.getDateEnd(), startDate, endDate);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(cache -> {
                    workshop.setWorkshopName(cache.getName());
                    workshop.setWorkshopDateStart(cache.getDateStart());
                    workshop.setWorkshopDateEnd(cache.getDateEnd());

                    try {
                        Method method = ReportService.class.getDeclaredMethod("toWorkshopDto", ReportWorkshop.class);
                        method.setAccessible(true);
                        return (ReportWorkshopDto) method.invoke(reportService, workshop);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        // Assert (sin verificar workshopStatus)
        StepVerifier.create(resultMono)
                .expectNextMatches(dto ->
                        dto.getWorkshopName().equals("Taller desde cache") &&
                                dto.getWorkshopDateStart().equals(LocalDate.of(2023, 2, 1)) &&
                                dto.getWorkshopDateEnd().equals(LocalDate.of(2023, 2, 28))
                )
                .verifyComplete();
    }

    // Helper methods to create test objects

    private Report createSampleReport(Integer id, Integer year, String trimester, String description, String status) {
        Report report = new Report();
        report.setId(id);
        report.setYear(year);
        report.setTrimester(trimester);
        report.setDescription(description);
        report.setStatus(status);
        return report;
    }

    private ReportWorkshop createSampleWorkshop(Integer id, Integer reportId, Integer workshopId,
                                                String workshopName, LocalDate startDate, LocalDate endDate,
                                                String description) {
        ReportWorkshop workshop = new ReportWorkshop();
        workshop.setId(id);
        workshop.setReportId(reportId);
        workshop.setWorkshopId(workshopId);
        workshop.setWorkshopName(workshopName);
        workshop.setWorkshopDateStart(startDate);
        workshop.setWorkshopDateEnd(endDate);
        workshop.setDescription(description);
        workshop.setImageUrl(new String[]{});
        return workshop;
    }

    private Mono<ReportWorkshopDto> invokeFilterWorkshopByDate(ReportWorkshop workshop, LocalDate startDate, LocalDate endDate) {
        try {
            Method isInDateRangeMethod = ReportService.class.getDeclaredMethod("isInDateRange", LocalDate.class, LocalDate.class, LocalDate.class, LocalDate.class);
            isInDateRangeMethod.setAccessible(true);
            boolean inDateRange = (boolean) isInDateRangeMethod.invoke(reportService, workshop.getWorkshopDateStart(), workshop.getWorkshopDateEnd(), startDate, endDate);

            if (inDateRange) {
                Method toWorkshopDtoMethod = ReportService.class.getDeclaredMethod("toWorkshopDto", ReportWorkshop.class);
                toWorkshopDtoMethod.setAccessible(true);
                return Mono.just((ReportWorkshopDto) toWorkshopDtoMethod.invoke(reportService, workshop));
            } else {
                return Mono.empty();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}