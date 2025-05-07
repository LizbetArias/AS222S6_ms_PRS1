package pe.edu.vallegrande.report_service.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("report")
public class Report {
    @Id
    private Integer id;
    private Integer year;
    private String trimester;
    private String description;
    private String schedule;
    private String status;
}
