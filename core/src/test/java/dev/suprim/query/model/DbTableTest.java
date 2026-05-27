package dev.suprim.query.model;

import dev.suprim.query.exception.DbException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DbTableTest {

    private static final DbColumn DATA_COLUMN = new DbColumn(
            "users", "data", "", "t", false, "jsonb", false, false, String.class, "\"", ""
    );
    private static final DbTable JSONB_TABLE = new DbTable(
            "public", "users", "public.users", "t", List.of(DATA_COLUMN), "TABLE", "\""
    );

    @Test
    void render_shouldReturnFullNameAndAlias() {
        DbTable table = createTable();

        String result = table.render();

        assertThat(result).isEqualTo("public.users t");
    }

    @Test
    void copyWithAlias_shouldCreateNewTableWithNewAlias() {
        DbColumn col = createColumn("name", false);
        DbTable table = new DbTable("public", "users", "public.users", "t", List.of(col), "TABLE", "\"");

        DbTable copy = table.copyWithAlias("u");

        assertThat(copy.alias()).isEqualTo("u");
        assertThat(copy.name()).isEqualTo("users");
        assertThat(copy.dbColumns()).hasSize(1);
        assertThat(copy.dbColumns().get(0).tableAlias()).isEqualTo("u");
    }

    @Test
    void buildColumn_withExistingColumn_shouldReturnDbColumn() throws DbException {
        DbColumn col = createColumn("name", false);
        DbTable table = new DbTable("public", "users", "public.users", "t", List.of(col), "TABLE", "\"");

        DbColumn result = table.buildColumn("name");

        assertThat(result.name()).isEqualTo("name");
    }

    @Test
    void buildColumn_withAlias_shouldSetAlias() throws DbException {
        DbColumn col = createColumn("name", false);
        DbTable table = new DbTable("public", "users", "public.users", "t", List.of(col), "TABLE", "\"");

        DbColumn result = table.buildColumn("name:user_name");

        assertThat(result.name()).isEqualTo("name");
        assertThat(result.alias()).isEqualTo("user_name");
    }

    @Test
    void buildColumn_withJsonArrowOperator_shouldParseCorrectly() throws DbException {
        DbColumn result = JSONB_TABLE.buildColumn("data->>name");

        assertThat(result.name()).isEqualTo("data");
        assertThat(result.jsonParts()).isEqualTo("->>'name'");
    }

    @Test
    void buildColumn_withJsonSingleArrow_shouldParseCorrectly() throws DbException {
        DbColumn result = JSONB_TABLE.buildColumn("data->nested");

        assertThat(result.name()).isEqualTo("data");
        assertThat(result.jsonParts()).isEqualTo("->'nested'");
    }

    @Test
    void buildColumn_withJsonHashDoubleArrow_shouldParseCorrectly() throws DbException {
        DbColumn result = JSONB_TABLE.buildColumn("data#>>nested.field");

        assertThat(result.name()).isEqualTo("data");
        assertThat(result.jsonParts()).isEqualTo("#>>'{nested,field}'");
    }

    @Test
    void buildColumn_withJsonHashSingleArrow_shouldParseCorrectly() throws DbException {
        DbColumn result = JSONB_TABLE.buildColumn("data#>nested.field");

        assertThat(result.name()).isEqualTo("data");
        assertThat(result.jsonParts()).isEqualTo("#>'{nested,field}'");
    }

    @Test
    void buildColumn_withDoubleAsterisk_shouldParseCorrectly() throws DbException {
        DbColumn result = JSONB_TABLE.buildColumn("data**name");

        assertThat(result.name()).isEqualTo("data");
        assertThat(result.jsonParts()).isEqualTo("->>'name'");
    }

    @Test
    void buildColumn_withSingleAsterisk_shouldParseCorrectly() throws DbException {
        DbColumn result = JSONB_TABLE.buildColumn("data*name");

        assertThat(result.name()).isEqualTo("data");
        assertThat(result.jsonParts()).isEqualTo("->'name'");
    }

    @Test
    void buildColumn_withNullColumnName_shouldThrowDbException() {
        DbTable table = createTable();

        assertThatThrownBy(() -> table.buildColumn(null))
                .isInstanceOf(DbException.class)
                .hasMessageContaining("Column name must not be null or blank");
    }

    @Test
    void buildColumn_withBlankColumnName_shouldThrowDbException() {
        DbTable table = createTable();

        assertThatThrownBy(() -> table.buildColumn("   "))
                .isInstanceOf(DbException.class)
                .hasMessageContaining("Column name must not be null or blank");
    }

    @Test
    void buildColumn_withNonExistentColumn_shouldThrowDbException() {
        DbTable table = createTable();

        assertThatThrownBy(() -> table.buildColumn("nonexistent"))
                .isInstanceOf(DbException.class)
                .hasMessageContaining("Column not found: users.nonexistent");
    }

    // --- Multi-level JSONB path tests ---

    @Test
    void buildColumn_withMultiLevelArrow_shouldParseChainedOperators() throws DbException {
        DbColumn result = JSONB_TABLE.buildColumn("data->'feedback'->>'type'");

        assertThat(result.name()).isEqualTo("data");
        assertThat(result.jsonParts()).isEqualTo("->'feedback'->>'type'");
    }

    @Test
    void buildColumn_withMultiLevelUnquotedArrow_shouldAutoQuoteKeys() throws DbException {
        DbColumn result = JSONB_TABLE.buildColumn("data->feedback->>type");

        assertThat(result.name()).isEqualTo("data");
        assertThat(result.jsonParts()).isEqualTo("->'feedback'->>'type'");
    }

    @Test
    void buildColumn_withThreeLevelArrow_shouldParseAllLevels() throws DbException {
        DbColumn col = createColumn("meta", false);
        DbTable table = new DbTable("public", "events", "public.events", "e", List.of(col), "TABLE", "\"");

        DbColumn result = table.buildColumn("meta->config->settings->>theme");

        assertThat(result.name()).isEqualTo("meta");
        assertThat(result.jsonParts()).isEqualTo("->'config'->'settings'->>'theme'");
    }

    @Test
    void buildColumn_withMultiLevelDoubleAsterisk_shouldChainTextExtraction() throws DbException {
        DbColumn result = JSONB_TABLE.buildColumn("data**feedback**type");

        assertThat(result.name()).isEqualTo("data");
        assertThat(result.jsonParts()).isEqualTo("->'feedback'->>'type'");
    }

    @Test
    void buildColumn_withMultiLevelSingleAsterisk_shouldChainObjectExtraction() throws DbException {
        DbColumn result = JSONB_TABLE.buildColumn("data*feedback*type");

        assertThat(result.name()).isEqualTo("data");
        assertThat(result.jsonParts()).isEqualTo("->'feedback'->'type'");
    }

    @Test
    void buildColumn_withMultiLevelHashPath_shouldProducePostgresPathArray() throws DbException {
        DbColumn result = JSONB_TABLE.buildColumn("data#>>feedback.type.value");

        assertThat(result.name()).isEqualTo("data");
        assertThat(result.jsonParts()).isEqualTo("#>>'{feedback,type,value}'");
    }

    @Test
    void buildColumn_withMultiLevelArrowAndAlias_shouldSetAlias() throws DbException {
        DbColumn result = JSONB_TABLE.buildColumn("data->feedback->>type:feedback_type");

        assertThat(result.name()).isEqualTo("data");
        assertThat(result.alias()).isEqualTo("feedback_type");
        assertThat(result.jsonParts()).isEqualTo("->'feedback'->>'type'");
    }

    @Test
    void buildColumn_withTrailingDoubleAsterisk_shouldThrowDbException() {
        assertThatThrownBy(() -> JSONB_TABLE.buildColumn("data**"))
                .isInstanceOf(DbException.class)
                .hasMessageContaining("trailing delimiter");
    }

    @Test
    void buildColumn_withTrailingSingleAsterisk_shouldThrowDbException() {
        assertThatThrownBy(() -> JSONB_TABLE.buildColumn("data*"))
                .isInstanceOf(DbException.class)
                .hasMessageContaining("trailing delimiter");
    }

    @Test
    void buildColumn_withUnclosedQuote_shouldThrowDbException() {
        assertThatThrownBy(() -> JSONB_TABLE.buildColumn("data->'feedback"))
                .isInstanceOf(DbException.class)
                .hasMessageContaining("Unclosed quote");
    }

    @Test
    void buildColumn_withMultipleColons_shouldUseFirstColonOnly() throws DbException {
        DbColumn result = JSONB_TABLE.buildColumn("data->>name:alias:extra");

        assertThat(result.name()).isEqualTo("data");
        assertThat(result.alias()).isEqualTo("alias:extra");
    }

    @Test
    void buildColumn_withArrowAndEmptyKey_shouldProduceEmptyQuotedKey() throws DbException {
        DbColumn result = JSONB_TABLE.buildColumn("data->>");

        assertThat(result.name()).isEqualTo("data");
        assertThat(result.jsonParts()).isEqualTo("->>''");
    }

    @Test
    void buildColumn_withSingleCharKey_shouldNotBeConsideredQuoted() throws DbException {
        DbColumn result = JSONB_TABLE.buildColumn("data->>x");

        assertThat(result.name()).isEqualTo("data");
        assertThat(result.jsonParts()).isEqualTo("->>'x'");
    }

    @Test
    void buildColumn_withNoJsonOperator_shouldReturnPlainColumn() throws DbException {
        DbColumn result = JSONB_TABLE.buildColumn("data");

        assertThat(result.name()).isEqualTo("data");
        assertThat(result.jsonParts()).isEmpty();
    }

    @Test
    void buildColumns_shouldReturnAllColumns() {
        DbColumn col1 = createColumn("id", true);
        DbColumn col2 = createColumn("name", false);
        DbTable table = new DbTable("public", "users", "public.users", "t", List.of(col1, col2), "TABLE", "\"");

        List<DbColumn> result = table.buildColumns();

        assertThat(result).hasSize(2);
    }

    @Test
    void buildPkColumns_shouldReturnOnlyPrimaryKeyColumns() {
        DbColumn pkCol = createColumn("id", true);
        DbColumn regularCol = createColumn("name", false);
        DbTable table = new DbTable("public", "users", "public.users", "t", List.of(pkCol, regularCol), "TABLE", "\"");

        List<DbColumn> result = table.buildPkColumns();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("id");
    }

    @Test
    void getKeyColumnNames_shouldReturnPkColumnNamesAsArray() {
        DbColumn pk1 = createColumn("id", true);
        DbColumn pk2 = new DbColumn("users", "tenant_id", "", "t", true, "bigint", false, false, Long.class, "\"", "");
        DbColumn regular = createColumn("name", false);
        DbTable table = new DbTable("public", "users", "public.users", "t", List.of(pk1, pk2, regular), "TABLE", "\"");

        String[] result = table.getKeyColumnNames();

        assertThat(result).containsExactly("id", "tenant_id");
    }

    @Test
    void getColumnDataTypeName_shouldReturnDataType() throws DbException {
        DbColumn col = createColumn("name", false);
        DbTable table = new DbTable("public", "users", "public.users", "t", List.of(col), "TABLE", "\"");

        String result = table.getColumnDataTypeName("name");

        assertThat(result).isEqualTo("varchar");
    }

    @Test
    void getColumnDataTypeName_withNonExistentColumn_shouldThrowDbException() {
        DbTable table = createTable();

        assertThatThrownBy(() -> table.getColumnDataTypeName("nonexistent"))
                .isInstanceOf(DbException.class)
                .hasMessageContaining("Column not found: users.nonexistent");
    }

    @Test
    void recordAccessors_shouldReturnCorrectValues() {
        DbTable table = new DbTable("public", "users", "public.users", "t", List.of(), "VIEW", "'");

        assertThat(table.schema()).isEqualTo("public");
        assertThat(table.name()).isEqualTo("users");
        assertThat(table.fullName()).isEqualTo("public.users");
        assertThat(table.alias()).isEqualTo("t");
        assertThat(table.type()).isEqualTo("VIEW");
        assertThat(table.coverChar()).isEqualTo("'");
    }

    private DbTable createTable() {
        return new DbTable("public", "users", "public.users", "t", List.of(), "TABLE", "\"");
    }

    private DbColumn createColumn(String name, boolean pk) {
        return new DbColumn("users", name, "", "t", pk, "varchar", false, false, String.class, "\"", "");
    }
}
