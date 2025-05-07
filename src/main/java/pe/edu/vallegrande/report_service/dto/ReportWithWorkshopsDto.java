package pe.edu.vallegrande.report_service.dto;

import lombok.Data;

import java.util.List;

@Data
public class ReportWithWorkshopsDto {
    private ReportDto report;
    private List<ReportWorkshopDto> workshops;
}
