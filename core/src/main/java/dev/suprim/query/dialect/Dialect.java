package dev.suprim.query.dialect;

import dev.suprim.query.exception.DbException;
import dev.suprim.query.exception.DbRuntimeException;
import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.DbTable;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.nonNull;

/**
 * Base dialect for database operations.
 * Subclasses implement database-specific behavior.
 */
public abstract class Dialect {
    private final ObjectMapper objectMapper;
    private final String coverChar;

    protected Dialect(ObjectMapper objectMapper, String coverChar) {
        this.objectMapper = objectMapper;
        this.coverChar = coverChar;
    }

    /**
     * Returns {@code true} if this dialect handles the given database product.
     *
     * @param productName  from {@code DatabaseMetaData.getDatabaseProductName()}
     * @param majorVersion from {@code DatabaseMetaData.getDatabaseMajorVersion()}
     */
    public abstract boolean isSupportedDb(String productName, int majorVersion);

    /**
     * Converts data map values to database-native types in-place before INSERT/UPDATE.
     * <p>
     * For example, converts JSON objects to {@code PGobject} for PostgreSQL,
     * or string timestamps to {@code LocalDateTime}.
     *
     * @param table             target table metadata (for column type lookup)
     * @param insertableColumns column names whose values must be converted
     * @param data              mutable map of column name → value (modified in-place)
     */
    public abstract void processTypes(DbTable table, List<String> insertableColumns, Map<String, Object> data) throws DbException;

    /**
     * Renders a fully-qualified, aliased table name for use in SELECT/DELETE/UPDATE.
     * Example PostgreSQL output: {@code "public"."users" t}
     */
    public abstract String renderTableName(DbTable table, boolean containsWhere, boolean deleteOp);

    /**
     * Renders a fully-qualified table name without alias, for use in INSERT.
     * Example PostgreSQL output: {@code "public"."users"}
     */
    public abstract String renderTableNameWithoutAlias(DbTable table);

    protected ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    protected String getCoverChar() {
        return coverChar;
    }

    public boolean supportBatchReturnKeys() {
        return true;
    }

    public boolean supportAlias() {
        return true;
    }

    public int getMajorVersion() {
        return -1;
    }

    public String getAliasedName(DbColumn dbColumn, boolean deleteOp) {
        String base = dbColumn.tableAlias() + "." + dbColumn.name();
        if (nonNull(dbColumn.jsonParts()) && !dbColumn.jsonParts().isEmpty()) {
            return base + dbColumn.jsonParts();
        }
        return base;
    }

    public String getAliasedNameParam(DbColumn dbColumn, boolean deleteOp) {
        String base = dbColumn.tableAlias() + "_" + dbColumn.name();
        if (nonNull(dbColumn.jsonParts()) && !dbColumn.jsonParts().isEmpty()) {
            String cleanedJsonPart = dbColumn.jsonParts()
                    .replaceAll("[->'\"]", "")
                    .replaceAll("[^a-zA-Z0-9_]", "_");
            return base + "_" + cleanedJsonPart;
        }
        return base;
    }

    public List<Object> parseListValues(List<String> values, Class<?> type, String columnDatatypeName) {
        return values.stream()
                .map(v -> {
                    try {
                        return processValue(v, type, null, columnDatatypeName);
                    } catch (DbException e) {
                        throw new DbRuntimeException(e);
                    }
                })
                .toList();
    }

    public Object processValue(String value, Class<?> type, String format, String columnTypeName) throws DbException {
        if (String.class == type) {
            return value;
        } else if (Boolean.class == type || boolean.class == type) {
            return convertBoolean(value);
        } else if (Integer.class == type || int.class == type) {
            return Integer.valueOf(value);
        } else if (Long.class == type || long.class == type) {
            return Long.valueOf(value);
        } else if (Short.class == type || short.class == type) {
            return Short.valueOf(value);
        } else if (java.sql.Date.class == type) {
            return LocalDate.parse(value, DateTimeFormatter.ISO_DATE);
        } else if (java.sql.Timestamp.class == type) {
            return convertTimestamp(value);
        } else if (Object.class == type && "uuid".equals(columnTypeName)) {
            return UUID.fromString(value);
        } else {
            return value;
        }
    }

    public List<String> convertToStringArray(Object object) throws DbException {
        return List.of();
    }

    /**
     * Converts a string representation of a boolean to the appropriate database value.
     * <p>
     * The default implementation returns {@code "1"} or {@code "0"} for compatibility
     * with databases that store booleans as integers (MySQL, Oracle, MSSQL).
     * <p>
     * Override in dialect subclasses that support native boolean types
     * (e.g. {@code PostGreSQLDialect} returns an actual {@link Boolean}).
     *
     * @param value the string value ("true" / "false")
     * @return the converted value to bind as a SQL parameter
     */
    protected Object convertBoolean(String value) {
        return Boolean.parseBoolean(value) ? "1" : "0";
    }

    public Object convertJsonToVO(Object object) throws DbException {
        return null;
    }

    public String getCountSqlTemplate() {
        return "count";
    }

    public String getDeleteSqlTemplate() {
        return "delete";
    }

    public String getExistSqlTemplate() {
        return "exists";
    }

    public String getFindOneSqlTemplate() {
        return "find-one";
    }

    public String getInsertSqlTemplate() {
        return "insert";
    }

    public String getReadSqlTemplate() {
        return "read";
    }

    public String getUpdateSqlTemplate() {
        return "update";
    }

    public Object convertTimestamp(String value) throws DbException {
        return OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
