package dev.suprim.query.jdbc.config;

import java.util.List;

/**
 * Lightweight metadata configuration for a datasource detected via annotation.
 * Used when no explicit {@code db.databases} configuration is provided.
 *
 * @param dbId             the database identifier (typically the bean name)
 * @param schemas          the schemas to load metadata for
 * @param includeAllSchemas whether to include all schemas (ignores {@code schemas} list if true)
 */
public record DataSourceMetadataConfig(
        String dbId,
        List<String> schemas,
        boolean includeAllSchemas
) {

    /**
     * Creates a config with specific schemas (includeAllSchemas = false).
     */
    public static DataSourceMetadataConfig of(String dbId, List<String> schemas) {
        return new DataSourceMetadataConfig(dbId, schemas, false);
    }

    /**
     * Creates a config that includes all schemas.
     */
    public static DataSourceMetadataConfig allSchemas(String dbId) {
        return new DataSourceMetadataConfig(dbId, List.of(), true);
    }
}
