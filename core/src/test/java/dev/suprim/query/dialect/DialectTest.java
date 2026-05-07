package dev.suprim.query.dialect;

import dev.suprim.query.exception.DbException;
import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.DbTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DialectTest {
    private TestDialect dialect;

    @BeforeEach
    void setUp() {
        dialect = new TestDialect(new ObjectMapper(), "\"");
    }

    @Test
    void supportBatchReturnKeys_shouldReturnTrue() {
        assertThat(dialect.supportBatchReturnKeys()).isTrue();
    }

    @Test
    void supportAlias_shouldReturnTrue() {
        assertThat(dialect.supportAlias()).isTrue();
    }

    @Test
    void getMajorVersion_shouldReturnNegativeOne() {
        assertThat(dialect.getMajorVersion()).isEqualTo(-1);
    }

    @Test
    void getAliasedName_withoutJsonParts_shouldReturnBaseName() {
        DbColumn col = createColumn("name", "");

        String result = dialect.getAliasedName(col, false);

        assertThat(result).isEqualTo("t.name");
    }

    @Test
    void getAliasedName_withJsonParts_shouldIncludeJsonParts() {
        DbColumn col = createColumn("data", "->>'key'");

        String result = dialect.getAliasedName(col, false);

        assertThat(result).isEqualTo("t.data->>'key'");
    }

    @Test
    void getAliasedNameParam_withoutJsonParts_shouldReturnBaseParam() {
        DbColumn col = createColumn("name", "");

        String result = dialect.getAliasedNameParam(col, false);

        assertThat(result).isEqualTo("t_name");
    }

    @Test
    void getAliasedNameParam_withJsonParts_shouldCleanAndAppendJsonParts() {
        DbColumn col = createColumn("data", "->>'key'");

        String result = dialect.getAliasedNameParam(col, false);

        assertThat(result).isEqualTo("t_data_key");
    }

    @Test
    void processValue_withString_shouldReturnString() throws DbException {
        Object result = dialect.processValue("hello", String.class, null, "varchar");

        assertThat(result).isEqualTo("hello");
    }

    @Test
    void processValue_withBooleanTrue_shouldReturn1() throws DbException {
        Object result = dialect.processValue("true", Boolean.class, null, "boolean");

        assertThat(result).isEqualTo("1");
    }

    @Test
    void processValue_withBooleanFalse_shouldReturn0() throws DbException {
        Object result = dialect.processValue("false", Boolean.class, null, "boolean");

        assertThat(result).isEqualTo("0");
    }

    @Test
    void processValue_withPrimitiveBoolean_shouldWork() throws DbException {
        Object result = dialect.processValue("true", boolean.class, null, "boolean");

        assertThat(result).isEqualTo("1");
    }

    @Test
    void processValue_withInteger_shouldReturnInteger() throws DbException {
        Object result = dialect.processValue("42", Integer.class, null, "integer");

        assertThat(result).isEqualTo(42);
    }

    @Test
    void processValue_withPrimitiveInt_shouldReturnInteger() throws DbException {
        Object result = dialect.processValue("42", int.class, null, "integer");

        assertThat(result).isEqualTo(42);
    }

    @Test
    void processValue_withLong_shouldReturnLong() throws DbException {
        Object result = dialect.processValue("123456789", Long.class, null, "bigint");

        assertThat(result).isEqualTo(123456789L);
    }

    @Test
    void processValue_withPrimitiveLong_shouldReturnLong() throws DbException {
        Object result = dialect.processValue("123456789", long.class, null, "bigint");

        assertThat(result).isEqualTo(123456789L);
    }

    @Test
    void processValue_withShort_shouldReturnShort() throws DbException {
        Object result = dialect.processValue("123", Short.class, null, "smallint");

        assertThat(result).isEqualTo((short) 123);
    }

    @Test
    void processValue_withPrimitiveShort_shouldReturnShort() throws DbException {
        Object result = dialect.processValue("123", short.class, null, "smallint");

        assertThat(result).isEqualTo((short) 123);
    }

    @Test
    void processValue_withSqlDate_shouldReturnLocalDate() throws DbException {
        Object result = dialect.processValue("2024-01-15", Date.class, null, "date");

        assertThat(result).isEqualTo(LocalDate.of(2024, 1, 15));
    }

    @Test
    void processValue_withTimestamp_shouldReturnOffsetDateTime() throws DbException {
        Object result = dialect.processValue("2024-01-15T10:30:00+07:00", Timestamp.class, null, "timestamp");

        assertThat(result).isInstanceOf(OffsetDateTime.class);
    }

    @Test
    void processValue_withUUID_shouldReturnUUID() throws DbException {
        String uuidStr = "550e8400-e29b-41d4-a716-446655440000";
        Object result = dialect.processValue(uuidStr, Object.class, null, "uuid");

        assertThat(result).isEqualTo(UUID.fromString(uuidStr));
    }

    @Test
    void processValue_withUnknownType_shouldReturnString() throws DbException {
        Object result = dialect.processValue("unknown", Object.class, null, "custom");

        assertThat(result).isEqualTo("unknown");
    }

    @Test
    void processValue_withObjectTypeAndNonUuidColumnType_shouldReturnString() throws DbException {
        // Tests the branch where Object.class == type but columnTypeName != "uuid"
        Object result = dialect.processValue("some-value", Object.class, null, "jsonb");

        assertThat(result).isEqualTo("some-value");
    }

    @Test
    void parseListValues_shouldProcessAllValues() {
        List<String> values = List.of("1", "2", "3");

        List<Object> result = dialect.parseListValues(values, Integer.class, "integer");

        assertThat(result).containsExactly(1, 2, 3);
    }

    @Test
    void parseListValues_withInvalidValue_shouldThrowRuntimeException() {
        List<String> values = List.of("not-a-number");

        assertThatThrownBy(() -> dialect.parseListValues(values, Integer.class, "integer"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void convertToStringArray_shouldReturnEmptyList() throws DbException {
        List<String> result = dialect.convertToStringArray(new Object());

        assertThat(result).isEmpty();
    }

    @Test
    void convertJsonToVO_shouldReturnNull() throws DbException {
        Object result = dialect.convertJsonToVO(new Object());

        assertThat(result).isNull();
    }

    @Test
    void getSqlTemplates_shouldReturnCorrectNames() {
        assertThat(dialect.getCountSqlTemplate()).isEqualTo("count");
        assertThat(dialect.getDeleteSqlTemplate()).isEqualTo("delete");
        assertThat(dialect.getExistSqlTemplate()).isEqualTo("exists");
        assertThat(dialect.getFindOneSqlTemplate()).isEqualTo("find-one");
        assertThat(dialect.getInsertSqlTemplate()).isEqualTo("insert");
        assertThat(dialect.getReadSqlTemplate()).isEqualTo("read");
        assertThat(dialect.getUpdateSqlTemplate()).isEqualTo("update");
    }

    @Test
    void convertTimestamp_shouldParseISODateTime() throws DbException {
        Object result = dialect.convertTimestamp("2024-01-15T10:30:00+00:00");

        assertThat(result).isInstanceOf(OffsetDateTime.class);
        OffsetDateTime dt = (OffsetDateTime) result;
        assertThat(dt.getYear()).isEqualTo(2024);
        assertThat(dt.getMonthValue()).isEqualTo(1);
        assertThat(dt.getDayOfMonth()).isEqualTo(15);
    }

    @Test
    void getAliasedName_withNullJsonParts_shouldReturnBaseName() {
        DbColumn col = new DbColumn("users", "name", "", "t", false, "varchar", false, false, String.class, "\"", null);

        String result = dialect.getAliasedName(col, false);

        assertThat(result).isEqualTo("t.name");
    }

    @Test
    void getAliasedNameParam_withNullJsonParts_shouldReturnBaseParam() {
        DbColumn col = new DbColumn("users", "name", "", "t", false, "varchar", false, false, String.class, "\"", null);

        String result = dialect.getAliasedNameParam(col, false);

        assertThat(result).isEqualTo("t_name");
    }

    @Test
    void getAliasedNameParam_withEmptyJsonParts_shouldReturnBaseParam() {
        DbColumn col = createColumn("name", "");

        String result = dialect.getAliasedNameParam(col, false);

        assertThat(result).isEqualTo("t_name");
    }

    @Test
    void testDialect_isSupportedDb_shouldReturnTrue() {
        assertThat(dialect.isSupportedDb("TestDB", 1)).isTrue();
    }

    @Test
    void testDialect_isSupportedDb_shouldReturnFalse() {
        assertThat(dialect.isSupportedDb("OtherDB", 1)).isFalse();
    }

    @Test
    void getObjectMapper_shouldReturnObjectMapper() {
        assertThat(dialect.getTestObjectMapper()).isNotNull();
    }

    @Test
    void getCoverChar_shouldReturnCoverChar() {
        assertThat(dialect.getTestCoverChar()).isEqualTo("\"");
    }

    @Test
    void processValue_withDoubleType_shouldFallThroughToDefault() throws DbException {
        // Tests the branch where Object.class == type is FALSE at line 104
        // Double.class doesn't match any specific type handler, so falls through
        Object result = dialect.processValue("3.14", Double.class, null, "double");

        assertThat(result).isEqualTo("3.14");
    }

    @Test
    void parseListValues_whenProcessValueThrowsDbException_shouldWrapInRuntimeException() {
        // Use a dialect that throws DbException from processValue
        ThrowingDialect throwingDialect = new ThrowingDialect();
        List<String> values = List.of("trigger-exception");

        assertThatThrownBy(() -> throwingDialect.parseListValues(values, String.class, "varchar"))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(DbException.class);
    }

    private DbColumn createColumn(String name, String jsonParts) {
        return new DbColumn("users", name, "", "t", false, "varchar", false, false, String.class, "\"", jsonParts);
    }

    // Concrete implementation for testing abstract Dialect
    private static class TestDialect extends Dialect {
        protected TestDialect(ObjectMapper objectMapper, String coverChar) {
            super(objectMapper, coverChar);
        }

        @Override
        public boolean isSupportedDb(String productName, int majorVersion) {
            return "TestDB".equals(productName);
        }

        @Override
        public void processTypes(DbTable table, List<String> insertableColumns, Map<String, Object> data) {
            // No-op for testing
        }

        @Override
        public String renderTableName(DbTable table, boolean containsWhere, boolean deleteOp) {
            return table.fullName() + " " + table.alias();
        }

        @Override
        public String renderTableNameWithoutAlias(DbTable table) {
            return table.fullName();
        }

        // Expose protected methods for testing
        public ObjectMapper getTestObjectMapper() {
            return getObjectMapper();
        }

        public String getTestCoverChar() {
            return getCoverChar();
        }
    }

    // Dialect that throws DbException from processValue for testing exception handling
    private static class ThrowingDialect extends Dialect {
        protected ThrowingDialect() {
            super(new ObjectMapper(), "\"");
        }

        @Override
        public boolean isSupportedDb(String productName, int majorVersion) {
            return false;
        }

        @Override
        public void processTypes(DbTable table, List<String> insertableColumns, Map<String, Object> data) {
        }

        @Override
        public String renderTableName(DbTable table, boolean containsWhere, boolean deleteOp) {
            return "";
        }

        @Override
        public String renderTableNameWithoutAlias(DbTable table) {
            return "";
        }

        @Override
        public Object processValue(String value, Class<?> type, String format, String columnTypeName) throws DbException {
            throw new DbException(dev.suprim.query.exception.DbErrorCode.SERVER_ERROR, "Forced exception for testing");
        }
    }

    // --- renderOnConflictClause tests ---

    @Test
    void renderOnConflictClause_withUpdateColumns_returnsDoUpdateSet() {
        String result = dialect.renderOnConflictClause(
                List.of("email"),
                List.of("name", "age")
        );

        assertThat(result).isEqualTo("ON CONFLICT (email) DO UPDATE SET name = EXCLUDED.name, age = EXCLUDED.age");
    }

    @Test
    void renderOnConflictClause_multipleConflictColumns_returnsComposite() {
        String result = dialect.renderOnConflictClause(
                List.of("tenant_id", "email"),
                List.of("name")
        );

        assertThat(result).isEqualTo("ON CONFLICT (tenant_id, email) DO UPDATE SET name = EXCLUDED.name");
    }

    @Test
    void renderOnConflictClause_nullUpdateColumns_returnsDoNothing() {
        String result = dialect.renderOnConflictClause(
                List.of("email"),
                null
        );

        assertThat(result).isEqualTo("ON CONFLICT (email) DO NOTHING");
    }

    @Test
    void renderOnConflictClause_emptyUpdateColumns_returnsDoNothing() {
        String result = dialect.renderOnConflictClause(
                List.of("email"),
                List.of()
        );

        assertThat(result).isEqualTo("ON CONFLICT (email) DO NOTHING");
    }

    @Test
    void getUpsertSqlTemplate_returnsUpsert() {
        assertThat(dialect.getUpsertSqlTemplate()).isEqualTo("upsert");
    }
}
