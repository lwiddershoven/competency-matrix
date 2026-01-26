package nl.leonw.competencymatrix.config;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class SyncConfigurationTest {

    @Inject
    CompetencySyncService syncService;

    @Test
    void resolveSyncMode_returnsMergeForMergeValue() {
        SyncMode result = syncService.resolveSyncMode("merge");

        assertEquals(SyncMode.MERGE, result);
    }

    @Test
    void resolveSyncMode_returnsReplaceForReplaceValue() {
        SyncMode result = syncService.resolveSyncMode("replace");

        assertEquals(SyncMode.REPLACE, result);
    }

    @Test
    void resolveSyncMode_isCaseInsensitive() {
        SyncMode result = syncService.resolveSyncMode("MERGE");

        assertEquals(SyncMode.MERGE, result);
    }

    @Test
    void resolveSyncMode_trimsWhitespace() {
        SyncMode result = syncService.resolveSyncMode("  merge  ");

        assertEquals(SyncMode.MERGE, result);
    }

    @Test
    void resolveSyncMode_defaultsToNoneWhenValueMissing() {
        SyncMode result = syncService.resolveSyncMode(null);

        assertEquals(SyncMode.NONE, result);
    }

    @Test
    void resolveSyncMode_defaultsToNoneWhenValueBlank() {
        SyncMode result = syncService.resolveSyncMode("   ");

        assertEquals(SyncMode.NONE, result);
    }

    @Test
    void resolveSyncMode_throwsForInvalidValue() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> syncService.resolveSyncMode("invalid"));

        assertTrue(exception.getMessage().contains("merge"));
        assertTrue(exception.getMessage().contains("replace"));
    }
}
