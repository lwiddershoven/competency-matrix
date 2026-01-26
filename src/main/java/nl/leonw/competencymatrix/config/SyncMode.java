package nl.leonw.competencymatrix.config;

/**
 * Enumeration of competency synchronization modes.
 * Controls how the system synchronizes competencies.yaml with the database.
 */
public enum SyncMode {
    /**
     * No synchronization performed.
     * Database remains unchanged regardless of YAML file contents.
     */
    NONE,

    /**
     * Incremental updates (merge mode).
     * Adds new entities, updates existing entities, preserves unchanged data.
     * No deletions occur.
     */
    MERGE,

    /**
     * Full replacement mode.
     * Deletes all existing competency data and seeds fresh from YAML.
     */
    REPLACE
}
