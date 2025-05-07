package pe.edu.vallegrande.report_service.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import pe.edu.vallegrande.report_service.model.ReportWorkshop;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ReportWorkshopRepository extends ReactiveCrudRepository<ReportWorkshop, Integer> {
    Flux<ReportWorkshop> findByReportId(Integer reportId);
    Mono<Void> deleteByReportId(Integer reportId);
}
