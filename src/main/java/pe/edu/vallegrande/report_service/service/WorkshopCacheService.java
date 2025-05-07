package pe.edu.vallegrande.report_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.report_service.model.WorkshopCache;
import pe.edu.vallegrande.report_service.repository.WorkshopCacheRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 🔹 Servicio para consultar los talleres almacenados en el cache.
 */
@Service
@RequiredArgsConstructor
public class WorkshopCacheService {

    private final WorkshopCacheRepository repository;

    /**
     * 🔸 Lista talleres del cache, con filtro opcional por status.
     */
    public Flux<WorkshopCache> findAll(String status) {
        return repository.findAll()
                .filter(workshop -> status == null || workshop.getStatus().equalsIgnoreCase(status));
    }

    /**
     * 🔸 Busca un taller del cache por ID.
     */
    public Mono<WorkshopCache> findById(Integer id) {
        return repository.findById(id);
    }
}
