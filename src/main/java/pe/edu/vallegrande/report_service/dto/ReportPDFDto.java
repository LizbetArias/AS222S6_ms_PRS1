package pe.edu.vallegrande.report_service.dto;

import lombok.Data;

@Data
public class ReportPDFDto {
    private Integer report_id;
    private Integer report_year;
    private String trimester;
    private String report_description;
    private String schedule;
    private String status;
    private Integer workshop_id;
    private String workshop_name;
    private String workshop_description;
    private String[] image_url;
}
