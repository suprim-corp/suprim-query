package dev.suprim.query.jdbc.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.isNull;

@Data
@Slf4j
public class DatabaseProperties {
    private String defaultDatabaseId;
    private List<DatabaseConnectionDetail> databases;

    public Optional<DatabaseConnectionDetail> getDatabase(String dbId) {
        if (isNull(databases) || databases.isEmpty()) {
            log.warn("No database configuration found for id: {}", dbId);
            return Optional.empty();
        }

        return databases.stream()
                .filter(dbConnectionDetail -> dbId.equalsIgnoreCase(dbConnectionDetail.id()))
                .findFirst();
    }

    public boolean isRdbmsConfigured() {
        if (isNull(databases)) {
            log.info("No database configuration found");
            return false;
        }

        log.info("Database configuration found.");

        boolean jdbcUrlFound = databases.stream()
                .anyMatch(DatabaseConnectionDetail::isJdbcPresent);

        log.info("JDBC URL found: {}", jdbcUrlFound);

        return jdbcUrlFound;
    }
}
