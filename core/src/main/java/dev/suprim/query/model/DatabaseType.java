package dev.suprim.query.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
@Getter
public enum DatabaseType {
    ORACLE("Oracle"),
    MSSQL("Microsoft SQL Server"),
    MYSQL("MySQL"),
    POSTGRESQL("PostgreSQL"),
    MARIADB("MariaDB"),
    SQLITE("SQLite"),
    DB2("DB2/UDB");

    private final String name;

    /**
     * Returns the {@link DatabaseType} whose {@link #name} matches the given string,
     * case-insensitively. Also matches the enum constant name itself (e.g. "POSTGRESQL").
     *
     * @param type the database type string from config (e.g. "postgresql", "PostgreSQL")
     * @return matching type, or empty if unknown
     */
    public static Optional<DatabaseType> fromString(String type) {
        if (type == null) return Optional.empty();
        for (DatabaseType dt : values()) {
            if (dt.name().equalsIgnoreCase(type) || dt.getName().equalsIgnoreCase(type)) {
                return Optional.of(dt);
            }
        }
        return Optional.empty();
    }
}
