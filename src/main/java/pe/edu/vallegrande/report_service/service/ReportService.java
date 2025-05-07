package pe.edu.vallegrande.report_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.report_service.dto.ReportDto;
import pe.edu.vallegrande.report_service.dto.ReportPDFDto;
import pe.edu.vallegrande.report_service.dto.ReportWithWorkshopsDto;
import pe.edu.vallegrande.report_service.dto.ReportWorkshopDto;
import pe.edu.vallegrande.report_service.model.Report;
import pe.edu.vallegrande.report_service.model.ReportWorkshop;
import pe.edu.vallegrande.report_service.repository.ReportRepository;
import pe.edu.vallegrande.report_service.repository.ReportWorkshopRepository;
import pe.edu.vallegrande.report_service.repository.WorkshopCacheRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepo;
    private final ReportWorkshopRepository workshopRepo;
    private final WorkshopCacheRepository workshopCacheRepo;
    private final SupabaseStorageService storageService;

    /**
     * Método para filtrar talleres según las fechas especificadas
     */
    private Mono<ReportWorkshopDto> filterWorkshopByDate(ReportWorkshop rw, LocalDate workshopDateStart, LocalDate workshopDateEnd) {
        ReportWorkshopDto dto = toWorkshopDto(rw);
        if (rw.getWorkshopId() != null) {
            return workshopCacheRepo.findById(rw.getWorkshopId())
                    .filter(wc -> isInDateRange(wc.getDateStart(), wc.getDateEnd(), workshopDateStart, workshopDateEnd))
                    .map(wc -> {
                        dto.setWorkshopStatus(wc.getStatus());
                        dto.setWorkshopDateStart(wc.getDateStart());
                        dto.setWorkshopDateEnd(wc.getDateEnd());
                        dto.setWorkshopName(wc.getName());
                        return dto;
                    });
        } else {
            boolean inRange = isInDateRange(rw.getWorkshopDateStart(), rw.getWorkshopDateEnd(), workshopDateStart, workshopDateEnd);
            return inRange ? Mono.just(dto) : Mono.empty();
        }
    }

    /**
     * Verifica si una fecha está dentro del rango especificado
     */
    private boolean isInDateRange(LocalDate start, LocalDate end, LocalDate rangeStart, LocalDate rangeEnd) {
        boolean inRange = true;
        if (rangeStart != null) inRange = !start.isBefore(rangeStart);
        if (rangeEnd != null) inRange = inRange && !end.isAfter(rangeEnd);
        return inRange;
    }

    /**
     * Busca reportes con filtros aplicados
     */
    public Flux<ReportWithWorkshopsDto> findFilteredReports(String status, String trimester, Integer year, LocalDate workshopDateStart, LocalDate workshopDateEnd) {
        Flux<Report> baseQuery = (status != null) ? reportRepo.findByStatus(status) : reportRepo.findAll();

        if (trimester != null) {
            baseQuery = baseQuery.filter(r -> trimester.equalsIgnoreCase(r.getTrimester()));
        }
        if (year != null) {
            baseQuery = baseQuery.filter(r -> year.equals(r.getYear()));
        }

        return baseQuery.flatMap(report ->
                workshopRepo.findByReportId(report.getId())
                        .flatMap(rw -> filterWorkshopByDate(rw, workshopDateStart, workshopDateEnd))
                        .collectList()
                        .filter(list -> !list.isEmpty())
                        .map(workshops -> {
                            ReportWithWorkshopsDto dto = new ReportWithWorkshopsDto();
                            dto.setReport(toDto(report));
                            dto.setWorkshops(workshops);
                            return dto;
                        })
        );
    }

    /**
     * Busca un reporte por ID con filtro de fechas para los talleres
     */
    public Mono<ReportWithWorkshopsDto> findByIdWithDateFilter(Integer id, LocalDate workshopDateStart, LocalDate workshopDateEnd) {
        Mono<Report> reportMono = reportRepo.findById(id);

        Flux<ReportWorkshopDto> workshopsFlux = workshopRepo.findByReportId(id)
                .flatMap(rw -> filterWorkshopByDate(rw, workshopDateStart, workshopDateEnd));

        return Mono.zip(reportMono, workshopsFlux.collectList(), (report, workshops) -> {
            ReportWithWorkshopsDto dto = new ReportWithWorkshopsDto();
            dto.setReport(toDto(report));
            dto.setWorkshops(workshops);
            return dto;
        });
    }

    /**
     * Crea un nuevo reporte con sus talleres asociados
     */
    public Mono<ReportWithWorkshopsDto> create(ReportWithWorkshopsDto dto) {
        Report report = fromDto(dto.getReport());
        report.setStatus("A");

        Mono<String> scheduleMono = handleSchedule(dto.getReport().getSchedule());

        return scheduleMono.flatMap(scheduleUrl -> {
            report.setSchedule(scheduleUrl);
            return reportRepo.save(report)
                    .flatMap(saved -> saveWorkshops(dto.getWorkshops(), saved.getId()))
                    .map(savedWorkshops -> {
                        ReportWithWorkshopsDto response = new ReportWithWorkshopsDto();
                        response.setReport(toDto(report));
                        response.setWorkshops(savedWorkshops.stream().map(this::toWorkshopDto).toList());
                        log.info("✅ Reporte creado: {}", response);
                        return response;
                    });
        });
    }

    /**
     * Guarda la lista de talleres asociados a un reporte
     */
    private Mono<List<ReportWorkshop>> saveWorkshops(List<ReportWorkshopDto> workshopsDto, Integer reportId) {
        return Flux.fromIterable(workshopsDto)
                .flatMap(dtoW -> Flux.fromArray(dtoW.getImageUrl())
                        .flatMap(image -> handleImageUpload(image, "reports/workshops"))
                        .collectList()
                        .flatMap(images -> {
                            ReportWorkshop rw = fromWorkshopDto(dtoW);
                            rw.setReportId(reportId);
                            rw.setImageUrl(images.toArray(new String[0]));
                            return Mono.just(rw);
                        }))
                .collectList()
                .flatMap(workshops -> workshopRepo.saveAll(workshops).collectList());
    }

    /**
     * Maneja la subida del horario (schedule) del reporte
     */
    private Mono<String> handleSchedule(String rawSchedule) {
        return (rawSchedule == null)
                ? Mono.just("")
                : isBase64(rawSchedule)
                ? storageService.uploadBase64Image("reports/schedules", rawSchedule)
                : Mono.just(rawSchedule);
    }

    /**
     * Maneja la subida de imágenes
     */
    private Mono<String> handleImageUpload(String image, String folder) {
        return isBase64(image)
                ? storageService.uploadBase64Image(folder, image)
                : Mono.just(image);
    }

    /**
     * Edita un reporte existente junto con sus talleres asociados
     */
    public Mono<ReportWithWorkshopsDto> update(Integer id, ReportWithWorkshopsDto dto) {
        return reportRepo.findById(id)
                .flatMap(existing -> {
                    // Actualiza los campos del reporte existente
                    updateExistingReport(existing, dto);

                    // Maneja los talleres antiguos y nuevos
                    return workshopRepo.findByReportId(id)
                            .collectList()
                            .flatMap(oldWorkshops -> handleOldWorkshops(oldWorkshops, dto.getWorkshops(), id))
                            .then(reportRepo.save(existing)) // Guarda el reporte actualizado
                            .flatMap(savedReport -> {
                                // Guarda los talleres y crea la respuesta con el reporte guardado
                                return saveWorkshops(dto.getWorkshops(), savedReport.getId())
                                        .map(savedWorkshops -> {
                                            ReportWithWorkshopsDto response = new ReportWithWorkshopsDto();
                                            response.setReport(toDto(savedReport)); // Usamos savedReport aquí
                                            response.setWorkshops(savedWorkshops.stream().map(this::toWorkshopDto).toList());
                                            log.info("✏️ Reporte actualizado con limpieza de imágenes: {}", response);
                                            return response;
                                        });
                            });
                });
    }

    /**
     * Actualiza los campos de un reporte existente
     */
    private void updateExistingReport(Report existing, ReportWithWorkshopsDto dto) {
        existing.setYear(dto.getReport().getYear());
        existing.setTrimester(dto.getReport().getTrimester());
        existing.setDescription(dto.getReport().getDescription());
        existing.setSchedule(handleSchedule(dto.getReport().getSchedule()).block());
    }

    /**
     * Maneja los talleres antiguos, elimina imágenes que ya no se usan
     */
    private Mono<Void> handleOldWorkshops(List<ReportWorkshop> oldWorkshops, List<ReportWorkshopDto> newWorkshops, Integer reportId) {
        List<String> oldUrls = oldWorkshops.stream()
                .flatMap(rw -> rw.getImageUrl() == null ? Stream.empty() : Arrays.stream(rw.getImageUrl()))
                .collect(Collectors.toList());

        List<String> newUrls = newWorkshops.stream()
                .flatMap(w -> w.getImageUrl() == null ? Stream.empty() : Arrays.stream(w.getImageUrl()))
                .collect(Collectors.toList());

        List<String> toDelete = oldUrls.stream()
                .filter(url -> !newUrls.contains(url))
                .collect(Collectors.toList());

        return Flux.fromIterable(toDelete)
                .flatMap(storageService::deleteImage)
                .then();
    }

    /**
     * Restaura un reporte (cambia su estado a Activo)
     */
    public Mono<Void> restore(Integer id) {
        return reportRepo.findById(id)
                .flatMap(r -> {
                    r.setStatus("A");
                    return reportRepo.save(r).then();
                });
    }

    /**
     * Eliminación lógica de un reporte (cambia su estado a Inactivo)
     */
    public Mono<Void> deleteLogic(Integer id) {
        return reportRepo.findById(id)
                .flatMap(r -> {
                    r.setStatus("I");
                    return reportRepo.save(r).then();
                });
    }

    /**
     * Genera un PDF con los datos del reporte y sus talleres filtrados por fecha
     */
    public Mono<ResponseEntity<byte[]>> generatePdfByIdWithDateFilter(Integer reportId, LocalDate workshopDateStart, LocalDate workshopDateEnd) {
        String folder = "pdf";
        String fileName = buildFileName(reportId, workshopDateStart, workshopDateEnd);

        return storageService.fileExists(folder, fileName)
                .flatMap(exists -> {
                    if (exists) {
                        return handleExistingPdf(folder, fileName);
                    }
                    return generateNewPdf(reportId, workshopDateStart, workshopDateEnd, fileName);
                });
    }

    /**
     * Construye el nombre del archivo PDF
     */
    private String buildFileName(Integer reportId, LocalDate workshopDateStart, LocalDate workshopDateEnd) {
        StringBuilder fileNameBuilder = new StringBuilder("reporte_" + reportId);
        if (workshopDateStart != null) {
            fileNameBuilder.append("_from_").append(workshopDateStart);
        }
        if (workshopDateEnd != null) {
            fileNameBuilder.append("_to_").append(workshopDateEnd);
        }
        fileNameBuilder.append(".pdf");
        return fileNameBuilder.toString();
    }

    /**
     * Maneja un PDF existente (redirige a la URL)
     */
    private Mono<ResponseEntity<byte[]>> handleExistingPdf(String folder, String fileName) {
        String url = storageService.getPublicUrl(folder, fileName);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(url));
        return Mono.just(ResponseEntity.status(HttpStatus.FOUND)
                .headers(headers)
                .body(new byte[0]));
    }

    /**
     * Genera un nuevo PDF para el reporte
     */
    private Mono<ResponseEntity<byte[]>> generateNewPdf(Integer reportId, LocalDate workshopDateStart, LocalDate workshopDateEnd, String fileName) {
        return reportRepo.findById(reportId)
                .flatMap(report -> workshopRepo.findByReportId(reportId)
                        .filter(rw -> isInDateRange(rw.getWorkshopDateStart(), rw.getWorkshopDateEnd(), workshopDateStart, workshopDateEnd))
                        .collectList()
                        .flatMap(filteredWorkshops -> createPdf(report, filteredWorkshops, fileName))
                ).switchIfEmpty(Mono.error(new NoSuchElementException("Reporte no encontrado con ID: " + reportId)));
    }

    /**
     * Crea el PDF con JasperReports
     */
    private Mono<ResponseEntity<byte[]>> createPdf(Report report, List<ReportWorkshop> filteredWorkshops, String fileName) {
        try {
            InputStream inputStream = new ClassPathResource("reportPDF.jasper").getInputStream();
            JasperReport jasperReport = (JasperReport) JRLoader.loadObject(inputStream);

            List<ReportPDFDto> reportData = filteredWorkshops.stream()
                    .map(workshop -> {
                        ReportPDFDto dto = new ReportPDFDto();
                        dto.setReport_id(report.getId());
                        dto.setReport_year(report.getYear());
                        dto.setTrimester(report.getTrimester());
                        dto.setReport_description(report.getDescription());
                        dto.setSchedule(report.getSchedule());
                        dto.setStatus(report.getStatus());
                        dto.setWorkshop_id(workshop.getId());
                        dto.setWorkshop_name(workshop.getWorkshopName());
                        dto.setWorkshop_description(workshop.getDescription());
                        dto.setImage_url(workshop.getImageUrl());
                        return dto;
                    }).collect(Collectors.toList());

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportData);
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("ReportTitle", "Reporte de Actividades");
            parameters.put("SUBREPORT_DIR", "images/");

            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            JasperExportManager.exportReportToPdfStream(jasperPrint, baos);
            byte[] pdfBytes = baos.toByteArray();

            // 🔄 Subir a Supabase en segundo plano (no bloquear)
            storageService.uploadPdf("pdf", fileName, pdfBytes).subscribe();

            // ✅ Devolver el PDF inmediatamente
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", fileName);
            return Mono.just(new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK));

        } catch (Exception e) {
            log.error("❌ Error al generar PDF:", e);
            return Mono.error(new RuntimeException("Error generando el PDF", e));
        }
    }

    /**
     * Verifica si ya existe un reporte para el año y trimestre especificados
     */
    public Mono<Boolean> existsByYearAndTrimester(Integer year, String trimester) {
        return reportRepo.findByYearAndTrimester(year, trimester)
                .hasElements();
    }

    // ======================= MAPEO DTO =======================

    /**
     * Convierte un Report a ReportDto
     */
    private ReportDto toDto(Report r) {
        ReportDto dto = new ReportDto();
        dto.setId(r.getId());
        dto.setYear(r.getYear());
        dto.setTrimester(r.getTrimester());
        dto.setDescription(r.getDescription());
        dto.setSchedule(r.getSchedule());
        dto.setStatus(r.getStatus());
        return dto;
    }

    /**
     * Convierte un ReportDto a Report
     */
    private Report fromDto(ReportDto dto) {
        return Report.builder()
                .id(dto.getId())
                .year(dto.getYear())
                .trimester(dto.getTrimester())
                .description(dto.getDescription())
                .schedule(dto.getSchedule())
                .status("A")
                .build();
    }

    /**
     * Convierte un ReportWorkshop a ReportWorkshopDto
     */
    private ReportWorkshopDto toWorkshopDto(ReportWorkshop rw) {
        ReportWorkshopDto dto = new ReportWorkshopDto();
        dto.setId(rw.getId());
        dto.setReportId(rw.getReportId());
        dto.setWorkshopId(rw.getWorkshopId());
        dto.setWorkshopName(rw.getWorkshopName());
        dto.setWorkshopDateStart(rw.getWorkshopDateStart());
        dto.setWorkshopDateEnd(rw.getWorkshopDateEnd());
        dto.setDescription(rw.getDescription());
        dto.setImageUrl(rw.getImageUrl());
        return dto;
    }

    /**
     * Convierte un ReportWorkshopDto a ReportWorkshop
     */
    private ReportWorkshop fromWorkshopDto(ReportWorkshopDto dto) {
        return ReportWorkshop.builder()
                .id(dto.getId())
                .reportId(dto.getReportId())
                .workshopId(dto.getWorkshopId())
                .workshopName(dto.getWorkshopName())
                .workshopDateStart(dto.getWorkshopDateStart())
                .workshopDateEnd(dto.getWorkshopDateEnd())
                .description(dto.getDescription())
                .imageUrl(dto.getImageUrl())
                .build();
    }

    /**
     * Verifica si una cadena es una imagen en formato Base64
     */
    private boolean isBase64(String input) {
        return input != null && input.startsWith("data:image/");
    }
}