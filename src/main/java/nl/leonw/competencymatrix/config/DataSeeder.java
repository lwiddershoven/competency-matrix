package nl.leonw.competencymatrix.config;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Inject
    CompetencySyncService competencySyncService;

    void onStart(@Observes StartupEvent event) {
        try {
            competencySyncService.syncFromConfiguration();
        } catch (Exception e) {
            log.error("Failed to synchronize competencies", e);
            throw new RuntimeException("Competency synchronization failed", e);
        }
    }
}
