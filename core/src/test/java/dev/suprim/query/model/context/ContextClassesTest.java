package dev.suprim.query.model.context;

import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.DbJoin;
import dev.suprim.query.model.DbSort;
import dev.suprim.query.model.DbTable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ContextClassesTest {

    @Test
    void deleteContext_createParamMap_shouldInitializeIfNull() {
        DeleteContext ctx = DeleteContext.builder().build();
        assertThat(ctx.getParamMap()).isNull();

        ctx.createParamMap();

        assertThat(ctx.getParamMap()).isNotNull().isEmpty();
    }

    @Test
    void deleteContext_createParamMap_shouldNotReplaceExisting() {
        Map<String, Object> existing = new HashMap<>();
        existing.put("key", "value");
        DeleteContext ctx = DeleteContext.builder().paramMap(existing).build();

        ctx.createParamMap();

        assertThat(ctx.getParamMap()).containsEntry("key", "value");
    }

    @Test
    void deleteContext_builder_shouldSetAllFields() {
        DbTable table = createTestTable();
        Map<String, Object> data = Map.of("id", 1);
        Map<String, Object> params = new HashMap<>();

        DeleteContext ctx = DeleteContext.builder()
                .dbId("db1")
                .tableName("users")
                .table(table)
                .where("id = :id")
                .paramMap(params)
                .data(data)
                .build();

        assertThat(ctx.getDbId()).isEqualTo("db1");
        assertThat(ctx.getTableName()).isEqualTo("users");
        assertThat(ctx.getTable()).isEqualTo(table);
        assertThat(ctx.getWhere()).isEqualTo("id = :id");
        assertThat(ctx.getData()).containsEntry("id", 1);
    }

    @Test
    void readContext_createParamMap_shouldInitializeIfNull() {
        ReadContext ctx = ReadContext.builder().build();
        assertThat(ctx.getParamMap()).isNull();

        ctx.createParamMap();

        assertThat(ctx.getParamMap()).isNotNull().isEmpty();
    }

    @Test
    void readContext_addColumns_shouldAppendToExisting() {
        DbColumn col1 = createTestColumn("col1");
        DbColumn col2 = createTestColumn("col2");
        ReadContext ctx = ReadContext.builder().cols(new ArrayList<>(List.of(col1))).build();

        ctx.addColumns(List.of(col2));

        assertThat(ctx.getCols()).hasSize(2);
    }

    @Test
    void readContext_addJoin_shouldInitializeAndAdd() {
        ReadContext ctx = ReadContext.builder().build();
        DbJoin join = new DbJoin();

        ctx.addJoin(join);

        assertThat(ctx.getDbJoins()).hasSize(1).contains(join);
    }

    @Test
    void readContext_addJoin_shouldAppendToExisting() {
        DbJoin join1 = new DbJoin();
        DbJoin join2 = new DbJoin();
        ReadContext ctx = ReadContext.builder().dbJoins(new ArrayList<>(List.of(join1))).build();

        ctx.addJoin(join2);

        assertThat(ctx.getDbJoins()).hasSize(2);
    }

    @Test
    void readContext_addTable_shouldInitializeAndAdd() {
        ReadContext ctx = ReadContext.builder().build();
        DbTable table = createTestTable();

        ctx.addTable(table);

        assertThat(ctx.getAllTables()).hasSize(1).contains(table);
    }

    @Test
    void readContext_addTable_shouldAppendToExisting() {
        DbTable table1 = createTestTable();
        DbTable table2 = createTestTable();
        ReadContext ctx = ReadContext.builder().allTables(new ArrayList<>(List.of(table1))).build();

        ctx.addTable(table2);

        assertThat(ctx.getAllTables()).hasSize(2);
    }

    @Test
    void readContext_builder_shouldSetAllFields() {
        ReadContext ctx = ReadContext.builder()
                .dbId("db1")
                .schemaName("public")
                .tableName("users")
                .fields("id,name")
                .filter("active=true")
                .sorts(List.of("name:asc"))
                .limit(10)
                .offset(5L)
                .defaultFetchLimit(100)
                .build();

        assertThat(ctx.getDbId()).isEqualTo("db1");
        assertThat(ctx.getSchemaName()).isEqualTo("public");
        assertThat(ctx.getTableName()).isEqualTo("users");
        assertThat(ctx.getFields()).isEqualTo("id,name");
        assertThat(ctx.getFilter()).isEqualTo("active=true");
        assertThat(ctx.getSorts()).containsExactly("name:asc");
        assertThat(ctx.getLimit()).isEqualTo(10);
        assertThat(ctx.getOffset()).isEqualTo(5L);
        assertThat(ctx.getDefaultFetchLimit()).isEqualTo(100);
    }

    @Test
    void createContext_renderColumns_shouldJoinColumnNames() {
        List<InsertableColumn> cols = List.of(
                InsertableColumn.builder().columnName("id").build(),
                InsertableColumn.builder().columnName("name").build()
        );
        CreateContext ctx = new CreateContext("db1", null, null, cols);

        String result = ctx.renderColumns();

        assertThat(result).isEqualTo("id,name");
    }

    @Test
    void createContext_renderParams_shouldJoinParamNames() {
        List<InsertableColumn> cols = List.of(
                InsertableColumn.builder().columnName("id").build(),
                InsertableColumn.builder().columnName("name").build()
        );
        CreateContext ctx = new CreateContext("db1", null, null, cols);

        String result = ctx.renderParams();

        assertThat(result).isEqualTo(":id,:name");
    }

    @Test
    void createContext_renderParams_withSequence_shouldUseSequence() {
        List<InsertableColumn> cols = List.of(
                InsertableColumn.builder().columnName("id").sequence("nextval('id_seq')").build(),
                InsertableColumn.builder().columnName("name").build()
        );
        CreateContext ctx = new CreateContext("db1", null, null, cols);

        String result = ctx.renderParams();

        assertThat(result).isEqualTo("nextval('id_seq'),:name");
    }

    @Test
    void createContext_renderParams_withEmptySequence_shouldUseColumnName() {
        List<InsertableColumn> cols = List.of(
                InsertableColumn.builder().columnName("id").sequence("").build(),
                InsertableColumn.builder().columnName("name").sequence("  ").build()
        );
        CreateContext ctx = new CreateContext("db1", null, null, cols);

        String result = ctx.renderParams();

        assertThat(result).isEqualTo(":id,:name");
    }

    @Test
    void updateContext_renderSetColumns_shouldFormatCorrectly() {
        UpdateContext ctx = UpdateContext.builder()
                .updatableColumns(List.of("name", "email"))
                .build();

        String result = ctx.renderSetColumns();

        assertThat(result).isEqualTo("name = :set_name,email = :set_email");
    }

    @Test
    void updateContext_createParamMap_shouldInitializeAndPrefixKeys() {
        UpdateContext ctx = UpdateContext.builder().build();
        Map<String, Object> data = new HashMap<>();
        data.put("name", "John");
        data.put("email", "john@test.com");

        ctx.createParamMap(data);

        assertThat(ctx.getParamMap())
                .containsEntry("set_name", "John")
                .containsEntry("set_email", "john@test.com");
    }

    @Test
    void updateContext_createParamMap_shouldAppendToExisting() {
        Map<String, Object> existing = new HashMap<>();
        existing.put("existing", "value");
        UpdateContext ctx = UpdateContext.builder().paramMap(existing).build();

        ctx.createParamMap(Map.of("name", "John"));

        assertThat(ctx.getParamMap())
                .containsEntry("existing", "value")
                .containsEntry("set_name", "John");
    }

    @Test
    void insertableColumn_shouldHoldColumnNameAndSequence() {
        InsertableColumn col = InsertableColumn.builder()
                .columnName("id")
                .sequence("nextval('seq')")
                .build();

        assertThat(col.getColumnName()).isEqualTo("id");
        assertThat(col.getSequence()).isEqualTo("nextval('seq')");
    }

    @Test
    void insertableColumn_noArgsConstructor_shouldWork() {
        InsertableColumn col = new InsertableColumn();
        col.setColumnName("name");
        col.setSequence(null);

        assertThat(col.getColumnName()).isEqualTo("name");
        assertThat(col.getSequence()).isNull();
    }

    private DbTable createTestTable() {
        return new DbTable("public", "users", "public.users", "u", List.of(), "TABLE", "\"");
    }

    @Test
    void readContext_createParamMap_shouldNotReplaceExisting() {
        Map<String, Object> existing = new HashMap<>();
        existing.put("key", "value");
        ReadContext ctx = ReadContext.builder().paramMap(existing).build();

        ctx.createParamMap();

        assertThat(ctx.getParamMap()).containsEntry("key", "value");
    }

    @Test
    void updateContext_createParamMap_shouldInitializeIfNull() {
        UpdateContext ctx = UpdateContext.builder().build();
        assertThat(ctx.getParamMap()).isNull();

        ctx.createParamMap(Map.of());

        assertThat(ctx.getParamMap()).isNotNull();
    }

    private DbColumn createTestColumn(String name) {
        return new DbColumn("users", name, "", "u", false, "varchar", false, false, String.class, "\"", "");
    }
}
