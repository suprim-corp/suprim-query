package dev.suprim.query.postgresql;

import dev.suprim.query.exception.DbException;
import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.DbTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.jdbc.PgArray;
import org.postgresql.util.PGobject;
import tools.jackson.databind.ObjectMapper;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PostGreSQLDialect — no Docker/Testcontainers required.
 */
class PostGreSQLDialectTest {

    private PostGreSQLDialect dialect;

    @BeforeEach
    void setUp() {
        dialect = new PostGreSQLDialect(new ObjectMapper());
    }

    // ==================== isSupportedDb ====================

    @Test
    void isSupportedDb_postgresql_shouldReturnTrue() {
        assertThat(dialect.isSupportedDb("PostgreSQL", 16)).isTrue();
        assertThat(dialect.isSupportedDb("postgresql", 14)).isTrue();
        assertThat(dialect.isSupportedDb("POSTGRESQL", 12)).isTrue();
    }

    @Test
    void isSupportedDb_otherDb_shouldReturnFalse() {
        assertThat(dialect.isSupportedDb("MySQL", 8)).isFalse();
        assertThat(dialect.isSupportedDb("Oracle", 19)).isFalse();
    }

    // ==================== renderTableName ====================

    @Test
    void renderTableName_shouldReturnQuotedSchemaTableAlias() {
        DbTable table = createTestTable("public", "users", "u");

        String result = dialect.renderTableName(table, false, false);

        assertThat(result).isEqualTo("\"public\".\"users\" u");
    }

    @Test
    void renderTableNameWithoutAlias_shouldReturnQuotedSchemaTable() {
        DbTable table = createTestTable("public", "users", "u");

        String result = dialect.renderTableNameWithoutAlias(table);

        assertThat(result).isEqualTo("\"public\".\"users\"");
    }

    // ==================== processTypes — null value ====================

    @Test
    void processTypes_withNullValue_shouldSkip() throws DbException {
        DbTable table = createTableWithColumn("test_col", "varchar");
        Map<String, Object> data = new HashMap<>();
        data.put("test_col", null);

        dialect.processTypes(table, List.of("test_col"), data);

        assertThat(data.get("test_col")).isNull();
    }

    // ==================== processTypes — JSON/JSONB ====================

    @Test
    void processTypes_jsonType_shouldConvertToPGobject() throws DbException {
        DbTable table = createTableWithColumn("json_col", "json");
        Map<String, Object> data = new HashMap<>();
        data.put("json_col", Map.of("key", "value"));

        dialect.processTypes(table, List.of("json_col"), data);

        assertThat(data.get("json_col")).isInstanceOf(PGobject.class);
        PGobject pg = (PGobject) data.get("json_col");
        assertThat(pg.getType()).isEqualTo("json");
    }

    @Test
    void processTypes_jsonbType_shouldConvertToPGobject() throws DbException {
        DbTable table = createTableWithColumn("jsonb_col", "jsonb");
        Map<String, Object> data = new HashMap<>();
        data.put("jsonb_col", Map.of("nested", Map.of("key", "value")));

        dialect.processTypes(table, List.of("jsonb_col"), data);

        assertThat(data.get("jsonb_col")).isInstanceOf(PGobject.class);
        PGobject pg = (PGobject) data.get("jsonb_col");
        assertThat(pg.getType()).isEqualTo("jsonb");
    }

    @Test
    void processTypes_jsonWithCircularReference_shouldThrowException() {
        DbTable table = createTableWithColumn("json_col", "json");
        Map<String, Object> circular = new HashMap<>();
        circular.put("self", circular);
        Map<String, Object> data = new HashMap<>();
        data.put("json_col", circular);

        assertThatThrownBy(() -> dialect.processTypes(table, List.of("json_col"), data))
                .isInstanceOf(DbException.class)
                .hasMessageContaining("Error converting to JSON");
    }

    // ==================== processTypes — timestamp ====================

    @Test
    void processTypes_timestampWithLocalDateTime_shouldKeepValue() throws DbException {
        DbTable table = createTableWithColumn("ts_col", "timestamp");
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> data = new HashMap<>();
        data.put("ts_col", now);

        dialect.processTypes(table, List.of("ts_col"), data);

        assertThat(data.get("ts_col")).isEqualTo(now);
    }

    @Test
    void processTypes_timestampWithString_shouldConvert() throws DbException {
        DbTable table = createTableWithColumn("ts_col", "timestamp");
        Map<String, Object> data = new HashMap<>();
        data.put("ts_col", "2024-01-15T10:30:00");

        dialect.processTypes(table, List.of("ts_col"), data);

        assertThat(data.get("ts_col")).isInstanceOf(LocalDateTime.class);
        LocalDateTime result = (LocalDateTime) data.get("ts_col");
        assertThat(result.getYear()).isEqualTo(2024);
        assertThat(result.getMonthValue()).isEqualTo(1);
        assertThat(result.getDayOfMonth()).isEqualTo(15);
    }

    @Test
    void processTypes_timestampWithInvalidString_shouldThrow() {
        DbTable table = createTableWithColumn("ts_col", "timestamp");
        Map<String, Object> data = new HashMap<>();
        data.put("ts_col", "invalid-date");

        assertThatThrownBy(() -> dialect.processTypes(table, List.of("ts_col"), data))
                .isInstanceOf(DbException.class)
                .hasMessageContaining("Error converting to LocalDateTime");
    }

    // ==================== processTypes — timestamptz ====================

    @Test
    void processTypes_timestamptzWithOffsetDateTime_shouldKeepValue() throws DbException {
        DbTable table = createTableWithColumn("tstz_col", "timestamptz");
        OffsetDateTime now = OffsetDateTime.now();
        Map<String, Object> data = new HashMap<>();
        data.put("tstz_col", now);

        dialect.processTypes(table, List.of("tstz_col"), data);

        assertThat(data.get("tstz_col")).isEqualTo(now);
    }

    @Test
    void processTypes_timestamptzWithOffsetString_shouldConvert() throws DbException {
        DbTable table = createTableWithColumn("tstz_col", "timestamptz");
        Map<String, Object> data = new HashMap<>();
        data.put("tstz_col", "2024-01-15T10:30:00+07:00");

        dialect.processTypes(table, List.of("tstz_col"), data);

        assertThat(data.get("tstz_col")).isInstanceOf(OffsetDateTime.class);
    }

    @Test
    void processTypes_timestamptzWithLocalDateTimeString_shouldConvert() throws DbException {
        DbTable table = createTableWithColumn("tstz_col", "timestamptz");
        Map<String, Object> data = new HashMap<>();
        data.put("tstz_col", "2024-01-15T10:30:00");

        dialect.processTypes(table, List.of("tstz_col"), data);

        assertThat(data.get("tstz_col")).isInstanceOf(OffsetDateTime.class);
    }

    @Test
    void processTypes_timestamptzWithInvalidString_shouldThrow() {
        DbTable table = createTableWithColumn("tstz_col", "timestamptz");
        Map<String, Object> data = new HashMap<>();
        data.put("tstz_col", "invalid-date");

        assertThatThrownBy(() -> dialect.processTypes(table, List.of("tstz_col"), data))
                .isInstanceOf(DbException.class)
                .hasMessageContaining("Error converting to OffsetDateTime");
    }

    // ==================== processTypes — timetz ====================

    @Test
    void processTypes_timetzWithOffsetTime_shouldKeepValue() throws DbException {
        DbTable table = createTableWithColumn("tz_col", "timetz");
        OffsetTime now = OffsetTime.now();
        Map<String, Object> data = new HashMap<>();
        data.put("tz_col", now);

        dialect.processTypes(table, List.of("tz_col"), data);

        assertThat(data.get("tz_col")).isEqualTo(now);
    }

    @Test
    void processTypes_timetzWithString_shouldConvert() throws DbException {
        DbTable table = createTableWithColumn("tz_col", "timetz");
        Map<String, Object> data = new HashMap<>();
        data.put("tz_col", "10:30:00+07:00");

        dialect.processTypes(table, List.of("tz_col"), data);

        assertThat(data.get("tz_col")).isInstanceOf(OffsetTime.class);
    }

    @Test
    void processTypes_timetzWithInvalidString_shouldThrow() {
        DbTable table = createTableWithColumn("tz_col", "timetz");
        Map<String, Object> data = new HashMap<>();
        data.put("tz_col", "invalid-time");

        assertThatThrownBy(() -> dialect.processTypes(table, List.of("tz_col"), data))
                .isInstanceOf(DbException.class)
                .hasMessageContaining("Error converting to OffsetTime");
    }

    // ==================== processTypes — int types ====================

    @Test
    void processTypes_intTypes_shouldConvertToLong() throws DbException {
        for (String intType : List.of("int4", "int2", "int8", "int")) {
            DbTable table = createTableWithColumn("int_col", intType);
            Map<String, Object> data = new HashMap<>();
            data.put("int_col", "42");

            dialect.processTypes(table, List.of("int_col"), data);

            assertThat(data.get("int_col")).isEqualTo(42L);
        }
    }

    // ==================== processTypes — numeric ====================

    @Test
    void processTypes_numericType_shouldConvertToDouble() throws DbException {
        DbTable table = createTableWithColumn("num_col", "numeric");
        Map<String, Object> data = new HashMap<>();
        data.put("num_col", "3.14159");

        dialect.processTypes(table, List.of("num_col"), data);

        assertThat(data.get("num_col")).isEqualTo(3.14159);
    }

    // ==================== processTypes — year ====================

    @Test
    void processTypes_yearType_shouldConvertToInteger() throws DbException {
        DbTable table = createTableWithColumn("year_col", "year");
        Map<String, Object> data = new HashMap<>();
        data.put("year_col", "2024");

        dialect.processTypes(table, List.of("year_col"), data);

        assertThat(data.get("year_col")).isEqualTo(2024);
    }

    // ==================== processTypes — _varchar (array) ====================

    @Test
    void processTypes_varcharArray_shouldConvertToArrayTypeValueHolder() throws DbException {
        DbTable table = createTableWithColumn("arr_col", "_varchar");
        Map<String, Object> data = new HashMap<>();
        data.put("arr_col", new ArrayList<>(List.of("a", "b", "c")));

        dialect.processTypes(table, List.of("arr_col"), data);

        assertThat(data.get("arr_col")).isNotNull();
    }

    // ==================== processTypes — vector ====================

    @Test
    void processTypes_vectorWithString_shouldConvertToPGobject() throws DbException {
        DbTable table = createTableWithColumn("vec_col", "vector(3)");
        Map<String, Object> data = new HashMap<>();
        data.put("vec_col", "[1.0,2.0,3.0]");

        dialect.processTypes(table, List.of("vec_col"), data);

        assertThat(data.get("vec_col")).isInstanceOf(PGobject.class);
        PGobject pg = (PGobject) data.get("vec_col");
        assertThat(pg.getType()).isEqualTo("vector");
        assertThat(pg.getValue()).isEqualTo("[1.0,2.0,3.0]");
    }

    @Test
    void processTypes_vectorWithFloatArray_shouldConvertToPGobject() throws DbException {
        DbTable table = createTableWithColumn("vec_col", "vector(3)");
        Map<String, Object> data = new HashMap<>();
        data.put("vec_col", new float[]{1.0f, 2.0f, 3.0f});

        dialect.processTypes(table, List.of("vec_col"), data);

        assertThat(data.get("vec_col")).isInstanceOf(PGobject.class);
        PGobject pg = (PGobject) data.get("vec_col");
        assertThat(pg.getValue()).isEqualTo("[1.0,2.0,3.0]");
    }

    @Test
    void processTypes_vectorWithDoubleArray_shouldConvertToPGobject() throws DbException {
        DbTable table = createTableWithColumn("vec_col", "vector(3)");
        Map<String, Object> data = new HashMap<>();
        data.put("vec_col", new double[]{1.0, 2.0, 3.0});

        dialect.processTypes(table, List.of("vec_col"), data);

        assertThat(data.get("vec_col")).isInstanceOf(PGobject.class);
        PGobject pg = (PGobject) data.get("vec_col");
        assertThat(pg.getValue()).isEqualTo("[1.0,2.0,3.0]");
    }

    @Test
    void processTypes_vectorWithUnknownType_shouldUseToString() throws DbException {
        DbTable table = createTableWithColumn("vec_col", "vector(3)");
        Map<String, Object> data = new HashMap<>();
        List<Double> vectorList = List.of(1.0, 2.0, 3.0);
        data.put("vec_col", vectorList);

        dialect.processTypes(table, List.of("vec_col"), data);

        assertThat(data.get("vec_col")).isInstanceOf(PGobject.class);
        PGobject pg = (PGobject) data.get("vec_col");
        assertThat(pg.getValue()).isEqualTo(vectorList.toString());
    }

    @Test
    void processTypes_vectorWithBrokenToString_shouldThrowDbException() {
        DbTable table = createTableWithColumn("vec_col", "vector(3)");
        Map<String, Object> data = new HashMap<>();
        Object brokenValue = new Object() {
            @Override
            public String toString() {
                throw new RuntimeException("toString() broken");
            }
        };
        data.put("vec_col", brokenValue);

        assertThatThrownBy(() -> dialect.processTypes(table, List.of("vec_col"), data))
                .isInstanceOf(DbException.class)
                .hasMessageContaining("Error converting to VECTOR");
    }

    // ==================== processTypes — uuid ====================

    @Test
    void processTypes_uuidWithUUID_shouldKeepValue() throws DbException {
        DbTable table = createTableWithColumn("uuid_col", "uuid");
        UUID uuid = UUID.randomUUID();
        Map<String, Object> data = new HashMap<>();
        data.put("uuid_col", uuid);

        dialect.processTypes(table, List.of("uuid_col"), data);

        assertThat(data.get("uuid_col")).isEqualTo(uuid);
    }

    @Test
    void processTypes_uuidWithString_shouldConvert() throws DbException {
        DbTable table = createTableWithColumn("uuid_col", "uuid");
        String uuidStr = "550e8400-e29b-41d4-a716-446655440000";
        Map<String, Object> data = new HashMap<>();
        data.put("uuid_col", uuidStr);

        dialect.processTypes(table, List.of("uuid_col"), data);

        assertThat(data.get("uuid_col")).isInstanceOf(UUID.class);
        assertThat(data.get("uuid_col").toString()).isEqualTo(uuidStr);
    }

    @Test
    void processTypes_uuidWithNonUuidNonString_shouldNotConvert() throws DbException {
        DbTable table = createTableWithColumn("uuid_col", "uuid");
        Integer intValue = 12345;
        Map<String, Object> data = new HashMap<>();
        data.put("uuid_col", intValue);

        dialect.processTypes(table, List.of("uuid_col"), data);

        assertThat(data.get("uuid_col")).isEqualTo(intValue);
    }

    // ==================== processTypes — complex value with unknown type ====================

    @Test
    void processTypes_complexMapValueWithUnknownType_shouldLogWarning() throws DbException {
        DbTable table = createTableWithColumn("other_col", "text");
        Map<String, Object> data = new HashMap<>();
        data.put("other_col", Map.of("key", "value"));

        dialect.processTypes(table, List.of("other_col"), data);

        assertThat(data.get("other_col")).isInstanceOf(Map.class);
    }

    @Test
    void processTypes_complexListValueWithUnknownType_shouldLogWarning() throws DbException {
        DbTable table = createTableWithColumn("list_col", "text");
        Map<String, Object> data = new HashMap<>();
        data.put("list_col", List.of("item1", "item2"));

        dialect.processTypes(table, List.of("list_col"), data);

        assertThat(data.get("list_col")).isInstanceOf(List.class);
    }

    @Test
    void processTypes_unknownTypeWithSimpleValue_shouldNotModify() throws DbException {
        DbTable table = createTableWithColumn("other_col", "citext");
        Map<String, Object> data = new HashMap<>();
        data.put("other_col", "hello");

        dialect.processTypes(table, List.of("other_col"), data);

        // Value passes through unmodified — no branch matches
        assertThat(data.get("other_col")).isEqualTo("hello");
    }

    // ==================== convertJsonToVO ====================

    @Test
    void convertJsonToVO_withValidPGobject_shouldReturnObject() throws Exception {
        PGobject pg = new PGobject();
        pg.setType("json");
        pg.setValue("{\"key\":\"value\"}");

        Object result = dialect.convertJsonToVO(pg);

        assertThat(result).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) result).get("key")).isEqualTo("value");
    }

    @Test
    void convertJsonToVO_withNull_shouldReturnNull() throws DbException {
        Object result = dialect.convertJsonToVO(null);

        assertThat(result).isNull();
    }

    @Test
    void convertJsonToVO_withInvalidJson_shouldThrow() throws Exception {
        PGobject pg = new PGobject();
        pg.setType("json");
        pg.setValue("invalid json");

        assertThatThrownBy(() -> dialect.convertJsonToVO(pg))
                .isInstanceOf(DbException.class)
                .hasMessageContaining("Error converting to JSON");
    }

    // ==================== convertToStringArray ====================

    @Test
    void convertToStringArray_withNull_shouldReturnEmptyList() throws DbException {
        List<String> result = dialect.convertToStringArray(null);

        assertThat(result).isEmpty();
    }

    @Test
    void convertToStringArray_withExceptionInGetArray_shouldThrowDbException() throws SQLException {
        PgArray mockArray = mock(PgArray.class);
        when(mockArray.getArray()).thenThrow(new SQLException("Connection closed"));

        assertThatThrownBy(() -> dialect.convertToStringArray(mockArray))
                .isInstanceOf(DbException.class)
                .hasMessageContaining("Error converting to Array type");
    }

    @Test
    void convertToStringArray_withValidArray_shouldReturnList() throws Exception {
        PgArray mockArray = mock(PgArray.class);
        when(mockArray.getArray()).thenReturn(new String[]{"a", "b", "c"});

        List<String> result = dialect.convertToStringArray(mockArray);

        assertThat(result).containsExactly("a", "b", "c");
    }

    // ==================== convertTimestamp ====================

    @Test
    void convertTimestamp_withValidString_shouldReturnLocalDateTime() throws DbException {
        LocalDateTime result = dialect.convertTimestamp("2024-01-15T10:30:00");

        assertThat(result.getYear()).isEqualTo(2024);
        assertThat(result.getMonthValue()).isEqualTo(1);
        assertThat(result.getDayOfMonth()).isEqualTo(15);
    }

    // ==================== processValue (boolean override) ====================

    @Test
    void processValue_withBooleanType_shouldParseBoolean() throws DbException {
        Object resultTrue = dialect.processValue("true", Boolean.class, null, "bool");
        Object resultFalse = dialect.processValue("false", boolean.class, null, "bool");

        assertThat(resultTrue).isEqualTo(true);
        assertThat(resultFalse).isEqualTo(false);
    }

    @Test
    void processValue_withOtherType_shouldDelegateToSuper() throws DbException {
        Object result = dialect.processValue("test", String.class, null, "varchar");

        assertThat(result).isEqualTo("test");
    }

    // ==================== helpers ====================

    private DbTable createTestTable(String schema, String name, String alias) {
        return new DbTable(schema, name, schema + "." + name, alias, List.of(), "TABLE", "\"");
    }

    private DbTable createTableWithColumn(String columnName, String typeName) {
        DbColumn column = new DbColumn(
                "test_table", columnName, "", "t",
                false, typeName, false, false,
                Object.class, "\"", ""
        );
        return new DbTable("public", "test_table", "public.test_table", "t",
                List.of(column), "TABLE", "\"");
    }
}
