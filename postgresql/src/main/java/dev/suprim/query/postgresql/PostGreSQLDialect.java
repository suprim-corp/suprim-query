package dev.suprim.query.postgresql;

import dev.suprim.query.dialect.Dialect;
import dev.suprim.query.exception.DbErrorCode;
import dev.suprim.query.exception.DbException;
import dev.suprim.query.model.ArrayTypeValueHolder;
import dev.suprim.query.model.DbTable;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.jdbc.PgArray;
import org.postgresql.util.PGobject;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.util.Locale.ROOT;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
public class PostGreSQLDialect extends Dialect {

    public PostGreSQLDialect(ObjectMapper objectMapper) {
        super(objectMapper, "\"");
    }

    @Override
    public void processTypes(
            DbTable table,
            List<String> insertableColumns,
            Map<String, Object> data
    ) throws DbException {
        for (String columnName : insertableColumns) {
            Object value = data.get(columnName);
            String columnDataTypeName = table.getColumnDataTypeName(columnName);

            log.debug("columnName : {} || columnDataTypeName - {}", columnName, columnDataTypeName);

            if (isNull(value)) {
                continue;
            }

            String typeLower = columnDataTypeName.toLowerCase(ROOT);
            if (Set.of("json", "jsonb").contains(typeLower)) {
                log.info("[Type Convert] Converting {} to JSONB (type: {}, valueClass: {})",
                        columnName, columnDataTypeName, value.getClass().getSimpleName());
                Object v = convertToJson(value, columnDataTypeName);
                data.put(columnName, v);
            } else if ("timestamp".equals(typeLower)) {
                if (value instanceof LocalDateTime) {
                    data.put(columnName, value);
                } else {
                    LocalDateTime v = convertToLocalDateTime((String) value);
                    data.put(columnName, v);
                }
            } else if ("timestamptz".equals(typeLower)) {
                if (value instanceof OffsetDateTime) {
                    data.put(columnName, value);
                } else {
                    OffsetDateTime v = convertToOffsetDateTime((String) value);
                    data.put(columnName, v);
                }
            } else if ("timetz".equals(typeLower)) {
                if (value instanceof OffsetTime) {
                    data.put(columnName, value);
                } else {
                    OffsetTime v = convertToOffsetTime((String) value);
                    data.put(columnName, v);
                }
            } else if (Set.of("int4", "int2", "int8", "int").contains(typeLower)) {
                data.put(columnName, Long.valueOf(value.toString().trim()));
            } else if ("numeric".equals(typeLower)) {
                data.put(columnName, Double.valueOf(value.toString().trim()));
            } else if ("year".equals(typeLower)) {
                data.put(columnName, Integer.valueOf(value.toString().trim()));
            } else if ("_varchar".equals(typeLower)) {
                log.debug("Array type found");
                data.put(columnName, new ArrayTypeValueHolder(
                        "java.sql.Array",
                        "varchar",
                        ((ArrayList<?>) value).toArray()
                ));
            } else if (typeLower.startsWith("vector")) {
                log.info("[Type Convert] Converting {} to VECTOR (type: {}, valueClass: {})",
                        columnName, columnDataTypeName, value.getClass().getSimpleName());
                Object vectorValue = convertToVector(value);
                data.put(columnName, vectorValue);
            } else if ("uuid".equals(typeLower)) {
                if (value instanceof UUID) {
                    data.put(columnName, value);
                } else if (value instanceof String) {
                    log.debug("[Type Convert] Converting {} to UUID (type: {}, valueClass: {})",
                            columnName, columnDataTypeName, value.getClass().getSimpleName());
                    UUID uuid = UUID.fromString((String) value);
                    data.put(columnName, uuid);
                }
            } else if (value instanceof List || value instanceof Map) {
                log.warn("[Type Convert] Column {} has complex value type {} but DB type is '{}' - may need conversion",
                        columnName, value.getClass().getSimpleName(), columnDataTypeName);
            }
        }
    }

    @Override
    public Object convertJsonToVO(Object object) throws DbException {
        if (nonNull(object)) {
            PGobject pGobject = (PGobject) object;
            String val = pGobject.getValue();

            try {
                return getObjectMapper().readValue(val, Object.class);
            } catch (JacksonException e) {
                throw new DbException(
                        DbErrorCode.INVALID_REQUEST,
                        "Error converting to JSON type - " + e.getLocalizedMessage()
                );
            }
        }
        return null;
    }

    @Override
    public String renderTableName(DbTable table, boolean containsWhere, boolean deleteOp) {
        return getQuotedName(table.schema()) + "." + getQuotedName(table.name()) + " " + table.alias();
    }

    @Override
    public String renderTableNameWithoutAlias(DbTable table) {
        return getQuotedName(table.schema()) + "." + getQuotedName(table.name());
    }

    @Override
    public boolean isSupportedDb(String productName, int majorVersion) {
        return "PostgreSQL".equalsIgnoreCase(productName);
    }

    /**
     * PostgreSQL supports native boolean values — return actual {@link Boolean}
     * instead of the default "1"/"0" integer strings used for other databases.
     */
    @Override
    protected Object convertBoolean(String value) {
        return Boolean.parseBoolean(value);
    }

    @Override
    public List<String> convertToStringArray(Object object) throws DbException {
        if (nonNull(object)) {
            PgArray pgArray = (PgArray) object;
            try {
                Object o = pgArray.getArray();
                return Arrays.asList((String[]) o);
            } catch (Exception e) {
                throw new DbException(
                        DbErrorCode.INVALID_REQUEST,
                        "Error converting to Array type - " + e.getLocalizedMessage()
                );
            }
        }
        return List.of();
    }

    @Override
    public LocalDateTime convertTimestamp(String value) throws DbException {
        return convertToLocalDateTime(value);
    }

    private String getQuotedName(String name) {
        return getCoverChar() + name + getCoverChar();
    }

    private OffsetTime convertToOffsetTime(String value) throws DbException {
        try {
            return OffsetTime.parse(value, DateTimeFormatter.ISO_OFFSET_TIME);
        } catch (Exception e) {
            throw new DbException(
                    DbErrorCode.INVALID_REQUEST,
                    "Error converting to OffsetTime type - " + e.getLocalizedMessage()
            );
        }
    }

    private LocalDateTime convertToLocalDateTime(String value) throws DbException {
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            throw new DbException(
                    DbErrorCode.INVALID_REQUEST,
                    "Error converting to LocalDateTime type - " + e.getLocalizedMessage()
            );
        }
    }

    private OffsetDateTime convertToOffsetDateTime(String value) throws DbException {
        try {
            return OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (Exception e1) {
            try {
                LocalDateTime localDateTime = LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                return localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
            } catch (Exception e2) {
                throw new DbException(
                        DbErrorCode.INVALID_REQUEST,
                        "Error converting to OffsetDateTime type - " + e2.getLocalizedMessage()
                );
            }
        }
    }

    private Object convertToJson(Object value, String columnDataTypeName) throws DbException {
        try {
            PGobject pGobject = new PGobject();
            pGobject.setType(columnDataTypeName);
            pGobject.setValue(getObjectMapper().writeValueAsString(value));
            return pGobject;
        } catch (Exception e) {
            throw new DbException(
                    DbErrorCode.INVALID_REQUEST,
                    "Error converting to JSON type - " + e.getLocalizedMessage()
            );
        }
    }

    private Object convertToVector(Object value) throws DbException {
        try {
            PGobject pGobject = new PGobject();
            pGobject.setType("vector");

            String vectorString;
            if (value instanceof String) {
                vectorString = (String) value;
            } else if (value instanceof float[]) {
                float[] arr = (float[]) value;
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < arr.length; i++) {
                    if (i > 0) sb.append(",");
                    sb.append(arr[i]);
                }
                sb.append("]");
                vectorString = sb.toString();
            } else if (value instanceof double[]) {
                double[] arr = (double[]) value;
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < arr.length; i++) {
                    if (i > 0) sb.append(",");
                    sb.append(arr[i]);
                }
                sb.append("]");
                vectorString = sb.toString();
            } else {
                vectorString = value.toString();
            }

            pGobject.setValue(vectorString);
            return pGobject;
        } catch (Exception e) {
            throw new DbException(
                    DbErrorCode.INVALID_REQUEST,
                    "Error converting to VECTOR type - " + e.getLocalizedMessage()
            );
        }
    }
}
