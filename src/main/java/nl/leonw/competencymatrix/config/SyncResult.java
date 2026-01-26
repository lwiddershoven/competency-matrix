package nl.leonw.competencymatrix.config;

/**
 * Tracks the outcomes of a competency synchronization operation.
 * Used for comprehensive logging and reporting.
 */
public record SyncResult(
    int categoriesAdded,
    int categoriesUpdated,
    int skillsAdded,
    int skillsUpdated,
    int rolesAdded,
    int rolesUpdated,
    int requirementsAdded,
    int requirementsUpdated,
    int progressionsAdded,
    int progressionsUpdated,
    int categoriesDeleted,    // Only non-zero in REPLACE mode
    int skillsDeleted,        // Only non-zero in REPLACE mode
    int rolesDeleted,         // Only non-zero in REPLACE mode
    int requirementsDeleted,  // Only non-zero in REPLACE mode
    int progressionsDeleted   // Only non-zero in REPLACE mode
) {
    /**
     * Formats a human-readable summary of the synchronization results.
     *
     * @return A formatted summary string suitable for logging
     */
    public String formatSummary() {
        StringBuilder sb = new StringBuilder("Sync complete: ");

        if (categoriesAdded + categoriesUpdated > 0) {
            sb.append(String.format("%d categories (%d added, %d updated), ",
                      categoriesAdded + categoriesUpdated, categoriesAdded, categoriesUpdated));
        }
        if (skillsAdded + skillsUpdated > 0) {
            sb.append(String.format("%d skills (%d added, %d updated), ",
                      skillsAdded + skillsUpdated, skillsAdded, skillsUpdated));
        }
        if (rolesAdded + rolesUpdated > 0) {
            sb.append(String.format("%d roles (%d added, %d updated), ",
                      rolesAdded + rolesUpdated, rolesAdded, rolesUpdated));
        }
        if (requirementsAdded + requirementsUpdated > 0) {
            sb.append(String.format("%d requirements (%d added, %d updated), ",
                      requirementsAdded + requirementsUpdated, requirementsAdded, requirementsUpdated));
        }
        if (progressionsAdded + progressionsUpdated > 0) {
            sb.append(String.format("%d progressions (%d added, %d updated), ",
                      progressionsAdded + progressionsUpdated, progressionsAdded, progressionsUpdated));
        }

        // Remove trailing comma and space
        if (sb.length() > 15) {
            sb.setLength(sb.length() - 2);
        } else {
            sb.append("no changes");
        }

        return sb.toString();
    }
}
