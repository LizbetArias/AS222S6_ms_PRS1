package pe.edu.vallegrande.report_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;

@Data
public class WorkshopKafkaEventDto {
    private Integer id;
    private String name;

    @JsonProperty("startDate")
    private LocalDate dateStart;

    @JsonProperty("endDate")
    private LocalDate dateEnd;

    @JsonProperty("state")
    private String status;
}
