package pe.edu.vallegrande.report_service.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ReportWorkshopDto {
    private Integer id;
    private Integer reportId;
    // Si se selecciona desde workshop_cache
    private Integer workshopId;
    // Siempre se llena
    private String workshopName;
    private LocalDate workshopDateStart;
    private LocalDate workshopDateEnd;
    private String description;
    private String[] imageUrl;
    // Estos solo son informativos si se seleccionó un taller real (opcional en UI)
    private String workshopStatus;
}
