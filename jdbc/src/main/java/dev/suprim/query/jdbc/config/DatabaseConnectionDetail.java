package dev.suprim.query.jdbc.config;

import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public record DatabaseConnectionDetail(
        String id,
        String type,
        String url,
        String username,
        String password,
        String database,
        List<String> catalog,
        List<String> schemas,
        List<String> tables,
        Map<String, String> connectionProperties,
        EnvironmentProperties envProperties,
        int maxConnections
) {
    public boolean isMongo() {
        return "MONGO".equalsIgnoreCase(type);
    }

    public boolean isJdbcPresent() {
        return nonNull(url) && !url.isBlank();
    }

    public boolean includeAllSchemas() {
        return isNull(schemas) || schemas.isEmpty();
    }
}
