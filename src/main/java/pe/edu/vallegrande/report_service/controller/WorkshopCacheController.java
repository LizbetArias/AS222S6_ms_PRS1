package pe.edu.vallegrande.report_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.report_service.model.WorkshopCache;
import pe.edu.vallegrande.report_service.service.WorkshopCacheService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/workshop-cache")
@RequiredArgsConstructor
public class WorkshopCacheController {

    private final WorkshopCacheService service;

    /**
     * 🔹 Listar todos los talleres del cache.
     */
    @GetMapping
    public Flux<WorkshopCache> getAll(@RequestParam(required = false) String status) {
        return service.findAll(status);
    }

    /**
     * 🔹 Obtener un taller del cache por ID.
     */
    @GetMapping("/{id}")
    public Mono<WorkshopCache> getById(@PathVariable Integer id) {
        return service.findById(id);
    }
}