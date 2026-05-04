package dev.suprim.query.rsql.handler;

import dev.suprim.query.exception.DbException;
import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.DbWhere;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for JSON/JSONB operator handlers.
 */
class JsonOperatorHandlersTest extends OperatorHandlerTestBase {

    private TestDialect dialect;
    private TestDialect dialectNoAlias;
    private DbColumn column;
    private DbWhere dbWhere;
    private Map<String, Object> paramMap;

    @BeforeEach
    void setUp() {
        dialect = createDialect();
        dialectNoAlias = createDialectWithoutAlias();
        column = createColumn("metadata", "jsonb", String.class);
        dbWhere = createDbWhere();
        paramMap = createParamMap();
    }

    // JsonbContainOperatorHandler tests
    @Test
    void jsonbContain_withAlias_shouldReturnCorrectSql() throws DbException {
        JsonbContainOperatorHandler handler = new JsonbContainOperatorHandler();

        String result = handler.handle(dialect, column, dbWhere, "{\"key\":\"value\"}", String.class, paramMap);

        assertThat(result).isEqualTo("t.metadata @> :t_metadata::jsonb");
        assertThat(paramMap).containsEntry("t_metadata", "{\"key\":\"value\"}");
    }

    @Test
    void jsonbContain_withoutAlias_shouldReturnCorrectSql() throws DbException {
        JsonbContainOperatorHandler handler = new JsonbContainOperatorHandler();

        String result = handler.handle(dialectNoAlias, column, dbWhere, "{\"key\":\"value\"}", String.class, paramMap);

        assertThat(result).isEqualTo("metadata @> :metadata::jsonb");
    }

    // JsonContainOperatorHandler tests - uses <@ operator (reverse containment)
    @Test
    void jsonContain_withAlias_shouldReturnCorrectSql() throws DbException {
        JsonContainOperatorHandler handler = new JsonContainOperatorHandler();

        String result = handler.handle(dialect, column, dbWhere, "{\"status\":\"active\"}", String.class, paramMap);

        // JsonContainOperatorHandler uses <@ operator with reversed operands
        assertThat(result).isEqualTo(":t_metadata::jsonb <@ t.metadata");
        assertThat(paramMap).containsEntry("t_metadata", "{\"status\":\"active\"}");
    }

    @Test
    void jsonContain_withoutAlias_shouldReturnCorrectSql() throws DbException {
        JsonContainOperatorHandler handler = new JsonContainOperatorHandler();

        String result = handler.handle(dialectNoAlias, column, dbWhere, "{\"status\":\"active\"}", String.class, paramMap);

        assertThat(result).isEqualTo(":metadata::jsonb <@ metadata");
    }

    // JsonbEqualToOperatorHandler tests - includes jsonParts (null in our case)
    @Test
    void jsonbEqualTo_withAlias_shouldReturnCorrectSql() throws DbException {
        JsonbEqualToOperatorHandler handler = new JsonbEqualToOperatorHandler();

        String result = handler.handle(dialect, column, dbWhere, "{\"id\":123}", String.class, paramMap);

        // getAliasedName() handles jsonParts internally — no "null" appended
        assertThat(result).isEqualTo("t.metadata = :t_metadata");
        assertThat(paramMap).containsEntry("t_metadata", "{\"id\":123}");
    }

    @Test
    void jsonbEqualTo_withoutAlias_shouldReturnCorrectSql() throws DbException {
        JsonbEqualToOperatorHandler handler = new JsonbEqualToOperatorHandler();

        String result = handler.handle(dialectNoAlias, column, dbWhere, "{\"id\":123}", String.class, paramMap);

        // jsonParts is null → empty string, no "null" appended
        assertThat(result).isEqualTo("metadata = :metadata");
    }

    @Test
    void jsonbEqualTo_withJsonParts_shouldIncludeJsonPath() throws DbException {
        JsonbEqualToOperatorHandler handler = new JsonbEqualToOperatorHandler();
        DbColumn columnWithJsonParts = new DbColumn(
                "users", "metadata", "", "t", false, "jsonb",
                false, false, String.class, "\"", "->>'key'");

        String result = handler.handle(dialectNoAlias, columnWithJsonParts, dbWhere, "value", String.class, paramMap);

        assertThat(result).isEqualTo("metadata->>'key' = :metadata");
    }

    // JsonbKeyExistsOperatorHandler tests - uses "is not null" operator
    @Test
    void jsonbKeyExists_withAlias_shouldReturnCorrectSql() throws DbException {
        JsonbKeyExistsOperatorHandler handler = new JsonbKeyExistsOperatorHandler();

        String result = handler.handle(dialect, column, dbWhere, "api_key", String.class, paramMap);

        // getAliasedName() handles jsonParts internally — no "null" appended
        assertThat(result).isEqualTo("t.metadata is not null ");
        assertThat(paramMap).isEmpty();
    }

    @Test
    void jsonbKeyExists_withoutAlias_shouldReturnCorrectSql() throws DbException {
        JsonbKeyExistsOperatorHandler handler = new JsonbKeyExistsOperatorHandler();

        String result = handler.handle(dialectNoAlias, column, dbWhere, "api_key", String.class, paramMap);

        // jsonParts is null → empty string, no "null" appended
        assertThat(result).isEqualTo("metadata is not null ");
    }

    @Test
    void jsonbKeyExists_withJsonParts_shouldIncludeJsonPath() throws DbException {
        JsonbKeyExistsOperatorHandler handler = new JsonbKeyExistsOperatorHandler();
        DbColumn columnWithJsonParts = new DbColumn(
                "users", "metadata", "", "t", false, "jsonb",
                false, false, String.class, "\"", "->>'api_key'");

        String result = handler.handle(dialectNoAlias, columnWithJsonParts, dbWhere, "api_key", String.class, paramMap);

        assertThat(result).isEqualTo("metadata->>'api_key' is not null ");
    }

    // JsonContainInArrayOperatorHandler tests - uses ?? operator
    @Test
    void jsonContainInArray_withAlias_shouldReturnCorrectSql() throws DbException {
        JsonContainInArrayOperatorHandler handler = new JsonContainInArrayOperatorHandler();

        String result = handler.handle(dialect, column, dbWhere, "admin", String.class, paramMap);

        // getAliasedName() handles jsonParts internally — no "null" appended
        assertThat(result).isEqualTo("t.metadata ?? 'admin'");
        assertThat(paramMap).isEmpty();
    }

    @Test
    void jsonContainInArray_withoutAlias_shouldReturnCorrectSql() throws DbException {
        JsonContainInArrayOperatorHandler handler = new JsonContainInArrayOperatorHandler();

        String result = handler.handle(dialectNoAlias, column, dbWhere, "admin", String.class, paramMap);

        // jsonParts is null → empty string, no "null" appended
        assertThat(result).isEqualTo("metadata ?? 'admin'");
    }

    @Test
    void jsonContainInArray_withJsonParts_shouldIncludeJsonPath() throws DbException {
        JsonContainInArrayOperatorHandler handler = new JsonContainInArrayOperatorHandler();
        DbColumn columnWithJsonParts = new DbColumn(
                "users", "roles", "", "t", false, "jsonb",
                false, false, String.class, "\"", "->'list'");

        String result = handler.handle(dialectNoAlias, columnWithJsonParts, dbWhere, "admin", String.class, paramMap);

        assertThat(result).isEqualTo("roles->'list' ?? 'admin'");
    }

    // JsonbArrowOperatorHandler tests
    @Test
    void jsonbArrow_withAlias_shouldReturnCorrectSql() throws DbException {
        JsonbArrowOperatorHandler handler = new JsonbArrowOperatorHandler();

        String result = handler.handle(dialect, column, dbWhere, "type::aws", String.class, paramMap);

        assertThat(result).isEqualTo("t.metadata->>'type' = :t_metadata_type");
        assertThat(paramMap).containsEntry("t_metadata_type", "aws");
    }

    @Test
    void jsonbArrow_withoutAlias_shouldReturnCorrectSql() throws DbException {
        JsonbArrowOperatorHandler handler = new JsonbArrowOperatorHandler();

        String result = handler.handle(dialectNoAlias, column, dbWhere, "type::aws", String.class, paramMap);

        assertThat(result).isEqualTo("metadata->>'type' = :metadata_type");
        assertThat(paramMap).containsEntry("metadata_type", "aws");
    }

    @Test
    void jsonbArrow_withInvalidFormat_shouldThrowException() {
        JsonbArrowOperatorHandler handler = new JsonbArrowOperatorHandler();

        assertThatThrownBy(() -> handler.handle(dialect, column, dbWhere, "invalid", String.class, paramMap))
                .isInstanceOf(DbException.class)
                .hasMessageContaining("JSONB arrow operator requires format: key::value");
    }

    @Test
    void jsonbArrow_withEmptyKey_shouldThrowException() {
        JsonbArrowOperatorHandler handler = new JsonbArrowOperatorHandler();

        assertThatThrownBy(() -> handler.handle(dialect, column, dbWhere, "::value", String.class, paramMap))
                .isInstanceOf(DbException.class)
                .hasMessageContaining("JSONB arrow operator requires format: key::value");
    }
}
