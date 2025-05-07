package pe.edu.vallegrande.report_service.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("workshop_cache")
public class WorkshopCache {
    @Id
    private Integer id;
    private String name;
    @Column("date_start")
    private LocalDate dateStart;
    @Column("date_end")
    private LocalDate dateEnd;
    private String status;
}
