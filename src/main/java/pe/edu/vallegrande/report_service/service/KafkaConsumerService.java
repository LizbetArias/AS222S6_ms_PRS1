package pe.edu.vallegrande.report_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.report_service.dto.WorkshopKafkaEventDto;
import pe.edu.vallegrande.report_service.model.WorkshopCache;
import pe.edu.vallegrande.report_service.repository.WorkshopCacheRepository;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final WorkshopCacheRepository cacheRepository;
    private final ObjectMapper objectMapper;
    private final R2dbcEntityTemplate template;

    /**
     * 🔹 Escucha eventos del topic "workshop-events" y sincroniza la información en el cache de reportes.
     * Convierte el JSON recibido a DTO, lo transforma en entidad, y lo guarda o actualiza.
     */
    @KafkaListener(topics = "workshop-events")
    public void consumeWorkshopEvent(ConsumerRecord<String, String> record) {
        try {
            String json = record.value();
            WorkshopKafkaEventDto dto = objectMapper.readValue(json, WorkshopKafkaEventDto.class);

            // ⚠️ Validación básica
            if (dto.getId() == null || dto.getName() == null) {
                log.warn("⚠️ Evento ignorado por datos incompletos: {}", dto);
                return;
            }

            log.info("📥 Recibido evento Kafka: {}", dto);

            // 🔄 Construye la entidad WorkshopCache desde el DTO
            WorkshopCache cache = WorkshopCache.builder()
                    .id(dto.getId())
                    .name(dto.getName())
                    .dateStart(dto.getDateStart())
                    .dateEnd(dto.getDateEnd())
                    .status(dto.getStatus())
                    .build();

            // 💾 Si ya existe, actualiza; si no, inserta nuevo registro
            cacheRepository.findById(dto.getId())
                    .flatMap(existing -> {
                        existing.setName(cache.getName());
                        existing.setDateStart(cache.getDateStart());
                        existing.setDateEnd(cache.getDateEnd());
                        existing.setStatus(cache.getStatus());
                        return cacheRepository.save(existing); // ✅ UPDATE
                    })
                    .switchIfEmpty(Mono.defer(() ->
                            template.insert(WorkshopCache.class).using(cache) // ✅ INSERT
                    ))
                    .subscribe(saved ->
                            log.info("✅ WorkshopCache insertado/actualizado: {}", saved)
                    );

        } catch (Exception e) {
            log.error("❌ Error procesando evento Kafka: {}", e.getMessage(), e);
        }
    }
}
