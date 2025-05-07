package pe.edu.vallegrande.report_service.dto;

import lombok.Data;

@Data
public class ReportDto {
    private Integer id;
    private Integer year;
    private String trimester;
    private String description;
    private String schedule;
    private String status;
}
