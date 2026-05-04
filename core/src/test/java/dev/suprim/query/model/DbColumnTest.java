package dev.suprim.query.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DbColumnTest {

    @Test
    void render_shouldReturnTableAliasAndQuotedName() {
        DbColumn col = createColumn("name", "varchar", "");

        String result = col.render();

        assertThat(result).isEqualTo("t.\"name\"");
    }

    @Test
    void render_withJsonParts_shouldIncludeJsonParts() {
        DbColumn col = createColumn("data", "jsonb", "->>'field'");

        String result = col.render();

        assertThat(result).isEqualTo("t.\"data\"->>'field'");
    }

    @Test
    void renderWithAlias_withAlias_shouldIncludeAsClause() {
        DbColumn col = new DbColumn("users", "name", "user_name", "t", false, "varchar", false, false, String.class, "\"", "");

        String result = col.renderWithAlias();

        assertThat(result).isEqualTo("t.\"name\" as \"user_name\"");
    }

    @Test
    void renderWithAlias_withoutAlias_shouldNotIncludeAsClause() {
        DbColumn col = createColumn("name", "varchar", "");

        String result = col.renderWithAlias();

        assertThat(result).isEqualTo("t.\"name\"");
    }

    @Test
    void renderWithAlias_withBlankAlias_shouldNotIncludeAsClause() {
        DbColumn col = new DbColumn("users", "name", "  ", "t", false, "varchar", false, false, String.class, "\"", "");

        String result = col.renderWithAlias();

        assertThat(result).isEqualTo("t.\"name\"");
    }

    @Test
    void getAliasedName_shouldReturnTableAliasAndName() {
        DbColumn col = createColumn("name", "varchar", "");

        assertThat(col.getAliasedName()).isEqualTo("t.name");
    }

    @Test
    void getAliasedNameParam_shouldReturnTableAliasUnderscoreName() {
        DbColumn col = createColumn("name", "varchar", "");

        assertThat(col.getAliasedNameParam()).isEqualTo("t_name");
    }

    @Test
    void isDateTimeFamily_withTimestamp_shouldReturnTrue() {
        DbColumn col = createColumn("created_at", "timestamp", "");

        assertThat(col.isDateTimeFamily()).isTrue();
    }

    @Test
    void isDateTimeFamily_withTimestampUppercase_shouldReturnTrue() {
        DbColumn col = createColumn("created_at", "TIMESTAMP", "");

        assertThat(col.isDateTimeFamily()).isTrue();
    }

    @Test
    void isDateTimeFamily_withVarchar_shouldReturnFalse() {
        DbColumn col = createColumn("name", "varchar", "");

        assertThat(col.isDateTimeFamily()).isFalse();
    }

    @Test
    void isIntFamily_withBigint_shouldReturnTrue() {
        DbColumn col = createColumn("id", "bigint", "");
        assertThat(col.isIntFamily()).isTrue();
    }

    @Test
    void isIntFamily_withInteger_shouldReturnTrue() {
        DbColumn col = createColumn("id", "integer", "");
        assertThat(col.isIntFamily()).isTrue();
    }

    @Test
    void isIntFamily_withSmallint_shouldReturnTrue() {
        DbColumn col = createColumn("id", "smallint", "");
        assertThat(col.isIntFamily()).isTrue();
    }

    @Test
    void isIntFamily_withInt8_shouldReturnTrue() {
        DbColumn col = createColumn("id", "int8", "");
        assertThat(col.isIntFamily()).isTrue();
    }

    @Test
    void isIntFamily_withInt4_shouldReturnTrue() {
        DbColumn col = createColumn("id", "int4", "");
        assertThat(col.isIntFamily()).isTrue();
    }

    @Test
    void isIntFamily_withBigintUnsigned_shouldReturnTrue() {
        DbColumn col = createColumn("id", "bigint unsigned", "");
        assertThat(col.isIntFamily()).isTrue();
    }

    @Test
    void isIntFamily_withNumber_shouldReturnTrue() {
        DbColumn col = createColumn("id", "number", "");
        assertThat(col.isIntFamily()).isTrue();
    }

    @Test
    void isIntFamily_withVarchar_shouldReturnFalse() {
        DbColumn col = createColumn("name", "varchar", "");
        assertThat(col.isIntFamily()).isFalse();
    }

    @Test
    void isStringFamily_withVarchar_shouldReturnTrue() {
        DbColumn col = createColumn("name", "varchar", "");
        assertThat(col.isStringFamily()).isTrue();
    }

    @Test
    void isStringFamily_withText_shouldReturnTrue() {
        DbColumn col = createColumn("description", "text", "");
        assertThat(col.isStringFamily()).isTrue();
    }

    @Test
    void isStringFamily_withVarchar2_shouldReturnTrue() {
        DbColumn col = createColumn("name", "varchar2", "");
        assertThat(col.isStringFamily()).isTrue();
    }

    @Test
    void isStringFamily_withInteger_shouldReturnFalse() {
        DbColumn col = createColumn("id", "integer", "");
        assertThat(col.isStringFamily()).isFalse();
    }

    @Test
    void copyWithAlias_shouldCreateNewColumnWithAlias() {
        DbColumn original = createColumn("name", "varchar", "");
        DbAlias dbAlias = new DbAlias("name", "user_name", "->>'key'");

        DbColumn copy = original.copyWithAlias(dbAlias);

        assertThat(copy.name()).isEqualTo("name");
        assertThat(copy.alias()).isEqualTo("user_name");
        assertThat(copy.jsonParts()).isEqualTo("->>'key'");
        assertThat(copy.tableAlias()).isEqualTo("t");
    }

    @Test
    void copyWithTableAlias_shouldCreateNewColumnWithTableAlias() {
        DbColumn original = new DbColumn("users", "name", "alias", "t", false, "varchar", false, false, String.class, "\"", "->>'key'");

        DbColumn copy = original.copyWithTableAlias("u");

        assertThat(copy.tableAlias()).isEqualTo("u");
        assertThat(copy.alias()).isEqualTo("alias");
        assertThat(copy.jsonParts()).isEmpty();
    }

    @Test
    void recordAccessors_shouldReturnCorrectValues() {
        DbColumn col = new DbColumn("users", "id", "user_id", "t", true, "bigint", false, true, Long.class, "\"", "");

        assertThat(col.tableName()).isEqualTo("users");
        assertThat(col.name()).isEqualTo("id");
        assertThat(col.alias()).isEqualTo("user_id");
        assertThat(col.tableAlias()).isEqualTo("t");
        assertThat(col.pk()).isTrue();
        assertThat(col.columnDataTypeName()).isEqualTo("bigint");
        assertThat(col.generated()).isFalse();
        assertThat(col.autoIncremented()).isTrue();
        assertThat(col.typeMappedClass()).isEqualTo(Long.class);
        assertThat(col.coverChar()).isEqualTo("\"");
    }

    @Test
    void render_withNullJsonParts_shouldNotIncludeJsonParts() {
        DbColumn col = new DbColumn("users", "data", "", "t", false, "varchar", false, false, String.class, "\"", null);

        String result = col.render();

        assertThat(result).isEqualTo("t.\"data\"");
    }

    @Test
    void render_withBlankJsonParts_shouldNotIncludeJsonParts() {
        DbColumn col = new DbColumn("users", "data", "", "t", false, "varchar", false, false, String.class, "\"", "   ");

        String result = col.render();

        assertThat(result).isEqualTo("t.\"data\"");
    }

    @Test
    void renderWithAlias_withNullAlias_shouldNotIncludeAsClause() {
        DbColumn col = new DbColumn("users", "name", null, "t", false, "varchar", false, false, String.class, "\"", "");

        String result = col.renderWithAlias();

        assertThat(result).isEqualTo("t.\"name\"");
    }

    private DbColumn createColumn(String name, String dataType, String jsonParts) {
        return new DbColumn("users", name, "", "t", false, dataType, false, false, String.class, "\"", jsonParts);
    }
}
