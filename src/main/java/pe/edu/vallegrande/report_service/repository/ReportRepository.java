package pe.edu.vallegrande.report_service.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import pe.edu.vallegrande.report_service.model.Report;
import reactor.core.publisher.Flux;

@Repository
public interface ReportRepository extends ReactiveCrudRepository<Report, Integer> {
    Flux<Report> findByStatus(String status);
    // ReportRepository.java
    Flux<Report> findByYearAndTrimester(Integer year, String trimester);

}
