package dev.suprim.query.jdbc.processor;

import dev.suprim.query.dialect.Dialect;
import dev.suprim.query.exception.DbException;
import dev.suprim.query.jdbc.operation.JdbcManager;
import dev.suprim.query.model.*;
import dev.suprim.query.model.context.ReadContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessorTest {

    @Mock
    private JdbcManager jdbcManager;

    private static DbTable usersTable;
    private static DbTable departmentsTable;
    private static DbTable ordersTable;

    @BeforeAll
    static void buildTables() {
        usersTable = new DbTable(
                "public", "users", "\"public\".\"users\"", "t0",
                List.of(
                        new DbColumn("users", "id", "", "t0", true, "int8", false, false, Long.class, "\"", ""),
                        new DbColumn("users", "name", "", "t0", false, "varchar", false, false, String.class, "\"", ""),
                        new DbColumn("users", "email", "", "t0", false, "varchar", false, false, String.class, "\"", ""),
                        new DbColumn("users", "department_id", "", "t0", false, "int8", false, false, Long.class, "\"", "")
                ),
                "TABLE", "\""
        );

        departmentsTable = new DbTable(
                "public", "departments", "\"public\".\"departments\"", "t1",
                List.of(
                        new DbColumn("departments", "id", "", "t1", true, "int8", false, false, Long.class, "\"", ""),
                        new DbColumn("departments", "name", "", "t1", false, "varchar", false, false, String.class, "\"", "")
                ),
                "TABLE", "\""
        );

        ordersTable = new DbTable(
                "public", "orders", "\"public\".\"orders\"", "t2",
                List.of(
                        new DbColumn("orders", "id", "", "t2", true, "int8", false, false, Long.class, "\"", ""),
                        new DbColumn("orders", "user_id", "", "t2", false, "int8", false, false, Long.class, "\"", ""),
                        new DbColumn("orders", "product_name", "", "t2", false, "varchar", false, false, String.class, "\"", ""),
                        new DbColumn("orders", "total", "", "t2", false, "numeric", false, false, Double.class, "\"", "")
                ),
                "TABLE", "\""
        );
    }

    @Nested
    @DisplayName("TSIDProcessor Tests")
    class TSIDProcessorTests {

        private TSIDProcessor tsidProcessor;

        @BeforeEach
        void setup() {
            tsidProcessor = new TSIDProcessor();
        }

        @Test
        void processTsId_intFamilyPk_generatesLongTsid() throws DbException {
            DbColumn pkColumn = mock(DbColumn.class);
            when(pkColumn.generated()).thenReturn(false);
            when(pkColumn.autoIncremented()).thenReturn(false);
            when(pkColumn.isIntFamily()).thenReturn(true);
            when(pkColumn.name()).thenReturn("id");
            when(pkColumn.columnDataTypeName()).thenReturn("int8");

            Map<String, Object> data = new HashMap<>();

            Map<String, Object> keys = tsidProcessor.processTsId(data, List.of(pkColumn));

            assertThat(keys.get("id")).isNotNull().isInstanceOf(Long.class);
            assertThat(data.get("id")).isEqualTo(keys.get("id"));
        }

        @Test
        void processTsId_stringFamilyPk_generatesStringTsid() throws DbException {
            DbColumn pkColumn = mock(DbColumn.class);
            when(pkColumn.generated()).thenReturn(false);
            when(pkColumn.autoIncremented()).thenReturn(false);
            when(pkColumn.isIntFamily()).thenReturn(false);
            when(pkColumn.isStringFamily()).thenReturn(true);
            when(pkColumn.name()).thenReturn("id");
            when(pkColumn.columnDataTypeName()).thenReturn("varchar");

            Map<String, Object> data = new HashMap<>();

            Map<String, Object> keys = tsidProcessor.processTsId(data, List.of(pkColumn));

            assertThat(keys.get("id")).isNotNull().isInstanceOf(String.class);
            assertThat(data.get("id")).isEqualTo(keys.get("id"));
        }

        @Test
        void processTsId_generatedPk_skipsTsidGeneration() throws DbException {
            DbColumn pkColumn = mock(DbColumn.class);
            when(pkColumn.generated()).thenReturn(true);
            when(pkColumn.autoIncremented()).thenReturn(false);

            Map<String, Object> data = new HashMap<>();

            Map<String, Object> keys = tsidProcessor.processTsId(data, List.of(pkColumn));

            assertThat(keys).isEmpty();
        }

        @Test
        void processTsId_autoIncrementedPk_skipsTsidGeneration() throws DbException {
            DbColumn pkColumn = mock(DbColumn.class);
            when(pkColumn.generated()).thenReturn(false);
            when(pkColumn.autoIncremented()).thenReturn(true);

            Map<String, Object> data = new HashMap<>();

            Map<String, Object> keys = tsidProcessor.processTsId(data, List.of(pkColumn));

            assertThat(keys).isEmpty();
        }

        @Test
        void processTsId_unknownTypeFamily_throwsDbException() {
            DbColumn pkColumn = mock(DbColumn.class);
            when(pkColumn.generated()).thenReturn(false);
            when(pkColumn.autoIncremented()).thenReturn(false);
            when(pkColumn.isIntFamily()).thenReturn(false);
            when(pkColumn.isStringFamily()).thenReturn(false);
            when(pkColumn.columnDataTypeName()).thenReturn("unknown");

            Map<String, Object> data = new HashMap<>();

            assertThatThrownBy(() -> tsidProcessor.processTsId(data, List.of(pkColumn)))
                    .isInstanceOf(DbException.class)
                    .hasMessageContaining("Unable to detect data type family");
        }

        @Test
        void processTsId_multiplePks_generatesMultipleTsids() throws DbException {
            DbColumn pk1 = mock(DbColumn.class);
            when(pk1.generated()).thenReturn(false);
            when(pk1.autoIncremented()).thenReturn(false);
            when(pk1.isIntFamily()).thenReturn(true);
            when(pk1.name()).thenReturn("id1");
            when(pk1.columnDataTypeName()).thenReturn("int8");

            DbColumn pk2 = mock(DbColumn.class);
            when(pk2.generated()).thenReturn(false);
            when(pk2.autoIncremented()).thenReturn(false);
            when(pk2.isIntFamily()).thenReturn(false);
            when(pk2.isStringFamily()).thenReturn(true);
            when(pk2.name()).thenReturn("id2");
            when(pk2.columnDataTypeName()).thenReturn("varchar");

            Map<String, Object> data = new HashMap<>();

            Map<String, Object> keys = tsidProcessor.processTsId(data, List.of(pk1, pk2));

            assertThat(keys).hasSize(2);
            assertThat(keys.get("id1")).isInstanceOf(Long.class);
            assertThat(keys.get("id2")).isInstanceOf(String.class);
        }
    }

    @Nested
    @DisplayName("RootTableProcessor Tests")
    class RootTableProcessorTests {

        private RootTableProcessor processor;

        @BeforeEach
        void setup() {
            processor = new RootTableProcessor(jdbcManager);
        }

        @Test
        void process_validTable_setsRootInContext() throws DbException {
            when(jdbcManager.getTable("test", null, "users")).thenReturn(usersTable);

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setTableName("users");

            processor.process(context);

            assertThat(context.getRoot()).isNotNull();
            assertThat(context.getRoot().name()).isEqualTo("users");
        }

        @Test
        void process_withSchema_setsRootInContext() throws DbException {
            when(jdbcManager.getTable("test", "public", "departments")).thenReturn(departmentsTable);

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setSchemaName("public");
            context.setTableName("departments");

            processor.process(context);

            assertThat(context.getRoot()).isNotNull();
            assertThat(context.getRoot().name()).isEqualTo("departments");
        }

        @Test
        void process_invalidTable_throwsDbException() throws DbException {
            when(jdbcManager.getTable("test", null, "nonexistent"))
                    .thenThrow(new DbException(dev.suprim.query.exception.DbErrorCode.INVALID_REQUEST, "Invalid table"));

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setTableName("nonexistent");

            assertThatThrownBy(() -> processor.process(context))
                    .isInstanceOf(DbException.class);
        }

        @Test
        void process_negativeLimitBelowMinusOne_throwsDbException() {
            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setTableName("users");
            context.setLimit(-2);

            assertThatThrownBy(() -> processor.process(context))
                    .isInstanceOf(DbException.class)
                    .hasMessageContaining("limit must be >= -1");
        }

        @Test
        void process_negativeOffset_throwsDbException() {
            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setTableName("users");
            context.setLimit(10);
            context.setOffset(-1);

            assertThatThrownBy(() -> processor.process(context))
                    .isInstanceOf(DbException.class)
                    .hasMessageContaining("offset must be >= 0");
        }
    }

    @Nested
    @DisplayName("RootTableFieldProcessor Tests")
    class RootTableFieldProcessorTests {

        private RootTableFieldProcessor processor;

        @BeforeEach
        void setup() {
            processor = new RootTableFieldProcessor();
        }

        @Test
        void process_nullFields_doesNotSetColumns() {
            ReadContext context = new ReadContext();
            context.setRoot(usersTable);
            context.setFields(null);

            processor.process(context);

            assertThat(context.getCols()).isNull();
        }

        @Test
        void process_allFields_includesAllColumns() {
            ReadContext context = new ReadContext();
            context.setRoot(usersTable);
            context.setFields("*");

            processor.process(context);

            assertThat(context.getCols()).isNotNull().hasSize(4);
        }

        @Test
        void process_specificFields_includesOnlySpecified() {
            ReadContext context = new ReadContext();
            context.setRoot(usersTable);
            context.setFields("id,name");

            processor.process(context);

            assertThat(context.getCols()).isNotNull().hasSize(2);
        }

        @Test
        void process_blankFields_throwsException() {
            ReadContext context = new ReadContext();
            context.setRoot(usersTable);
            context.setFields("   ");

            assertThatThrownBy(() -> processor.process(context))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("OrderByProcessor Tests")
    class OrderByProcessorTests {

        private OrderByProcessor processor;

        @BeforeEach
        void setup() {
            processor = new OrderByProcessor();
        }

        @Test
        void process_nullSorts_doesNothing() throws DbException {
            ReadContext context = new ReadContext();
            context.setTableName("users");
            context.setRoot(usersTable);
            context.setSorts(null);

            processor.process(context);

            assertThat(context.getDbSortList()).isNull();
        }

        @Test
        void process_emptySorts_doesNothing() throws DbException {
            ReadContext context = new ReadContext();
            context.setTableName("users");
            context.setRoot(usersTable);
            context.setSorts(List.of());

            processor.process(context);

            assertThat(context.getDbSortList()).isNull();
        }

        @Test
        void process_withSort_defaultsToAsc() throws DbException {
            ReadContext context = new ReadContext();
            context.setTableName("users");
            context.setRoot(usersTable);
            context.setSorts(List.of("name"));

            processor.process(context);

            assertThat(context.getDbSortList()).hasSize(1);
            assertThat(context.getDbSortList().get(0).sortDirection()).isEqualTo("ASC");
        }

        @Test
        void process_withAscSort_setsAscDirection() throws DbException {
            ReadContext context = new ReadContext();
            context.setTableName("users");
            context.setRoot(usersTable);
            context.setSorts(List.of("name;ASC"));

            processor.process(context);

            assertThat(context.getDbSortList()).hasSize(1);
            assertThat(context.getDbSortList().get(0).sortDirection()).isEqualTo("ASC");
        }

        @Test
        void process_withDescSort_setsDescDirection() throws DbException {
            ReadContext context = new ReadContext();
            context.setTableName("users");
            context.setRoot(usersTable);
            context.setSorts(List.of("name;DESC"));

            processor.process(context);

            assertThat(context.getDbSortList()).hasSize(1);
            assertThat(context.getDbSortList().get(0).sortDirection()).isEqualTo("DESC");
        }

        @Test
        void process_multipleSorts_addsAllSorts() throws DbException {
            ReadContext context = new ReadContext();
            context.setTableName("users");
            context.setRoot(usersTable);
            context.setSorts(List.of("name;ASC", "id;DESC"));

            processor.process(context);

            assertThat(context.getDbSortList()).hasSize(2);
        }

        @Test
        void process_invalidDirection_throwsDbException() {
            ReadContext context = new ReadContext();
            context.setTableName("users");
            context.setRoot(usersTable);
            context.setSorts(List.of("name;INVALID"));

            assertThatThrownBy(() -> processor.process(context))
                    .isInstanceOf(DbException.class)
                    .hasMessageContaining("Invalid sort direction");
        }

        @Test
        void process_blankSort_throwsDbException() {
            ReadContext context = new ReadContext();
            context.setTableName("users");
            context.setRoot(usersTable);
            context.setSorts(List.of("   "));

            assertThatThrownBy(() -> processor.process(context))
                    .isInstanceOf(DbException.class)
                    .hasMessageContaining("must not be blank");
        }

        @Test
        void process_nullSortElement_throwsDbException() {
            ReadContext context = new ReadContext();
            context.setTableName("users");
            context.setRoot(usersTable);
            List<String> sorts = new ArrayList<>();
            sorts.add(null);
            context.setSorts(sorts);

            assertThatThrownBy(() -> processor.process(context))
                    .isInstanceOf(DbException.class)
                    .hasMessageContaining("must not be blank");
        }
    }

    @Nested
    @DisplayName("RootWhereProcessor Tests")
    class RootWhereProcessorTests {

        private RootWhereProcessor processor;

        @BeforeEach
        void setup() {
            processor = new RootWhereProcessor(jdbcManager);
        }

        @Test
        void process_nullFilter_doesNotSetWhere() throws DbException {
            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setTableName("users");
            context.setRoot(usersTable);
            context.setCols(usersTable.buildColumns());
            context.setFilter(null);

            processor.process(context);

            assertThat(context.getRootWhere()).isNull();
        }

        @Test
        void process_blankFilter_doesNotSetWhere() throws DbException {
            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setTableName("users");
            context.setRoot(usersTable);
            context.setCols(usersTable.buildColumns());
            context.setFilter("   ");

            processor.process(context);

            assertThat(context.getRootWhere()).isNull();
        }

        @Test
        void process_withSimpleFilter_setsWhere() throws DbException {
            Dialect dialect = mock(Dialect.class);
            when(dialect.supportAlias()).thenReturn(true);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setTableName("users");
            context.setRoot(usersTable);
            context.setCols(usersTable.buildColumns());
            context.setFilter("id==1");

            processor.process(context);

            assertThat(context.getRootWhere()).isNotNull();
            assertThat(context.getParamMap()).isNotNull();
        }

        @Test
        void process_invalidRsql_throwsDbException() throws DbException {
            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setTableName("users");
            context.setRoot(usersTable);
            context.setCols(usersTable.buildColumns());
            context.setFilter("invalid rsql ;;; [[[");

            assertThatThrownBy(() -> processor.process(context))
                    .isInstanceOf(DbException.class);
        }
    }

    @Nested
    @DisplayName("JoinProcessor Tests")
    class JoinProcessorTests {

        private JoinProcessor processor;

        @BeforeEach
        void setup() throws DbException {
            processor = new JoinProcessor(jdbcManager);
        }

        @Test
        void process_nullJoins_doesNothing() throws DbException {
            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setRoot(usersTable);
            context.setJoins(null);

            processor.process(context);

            assertThat(context.getDbJoins()).isNullOrEmpty();
        }

        @Test
        void process_emptyJoins_doesNothing() throws DbException {
            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setRoot(usersTable);
            context.setJoins(List.of());

            processor.process(context);

            assertThat(context.getDbJoins()).isNullOrEmpty();
        }

        @Test
        void process_withJoin_addsJoinToContext() throws DbException {
            when(jdbcManager.getTable("test", null, "departments")).thenReturn(departmentsTable);

            JoinDetail joinDetail = new JoinDetail(
                    null, "departments", null, null,
                    List.of("department_id==id"), null, JoinType.LEFT
            );

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setRoot(usersTable);
            context.setJoins(List.of(joinDetail));
            context.setCols(new ArrayList<>(usersTable.buildColumns()));

            processor.process(context);

            assertThat(context.getDbJoins()).hasSize(1);
        }

        @Test
        void process_withMultipleJoins_addsAllJoins() throws DbException {
            when(jdbcManager.getTable("test", null, "departments")).thenReturn(departmentsTable);
            when(jdbcManager.getTable("test", null, "orders")).thenReturn(ordersTable);

            JoinDetail join1 = new JoinDetail(
                    null, "departments", null, null,
                    List.of("department_id==id"), null, JoinType.LEFT
            );

            JoinDetail join2 = new JoinDetail(
                    null, "orders", "users", null,
                    List.of("id==user_id"), null, JoinType.LEFT
            );

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setRoot(usersTable);
            context.setJoins(List.of(join1, join2));
            context.setCols(new ArrayList<>(usersTable.buildColumns()));

            processor.process(context);

            assertThat(context.getDbJoins()).hasSize(2);
        }

        @Test
        void process_withJoinFilter_parsesRsqlFilter() throws DbException {
            Dialect dialect = mock(Dialect.class);
            when(dialect.supportAlias()).thenReturn(true);
            when(jdbcManager.getTable("test", null, "orders")).thenReturn(ordersTable);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);

            JoinDetail joinDetail = new JoinDetail(
                    null, "orders", null, null,
                    List.of("id==user_id"), "total=gt=100", JoinType.LEFT
            );

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setRoot(usersTable);
            context.setJoins(List.of(joinDetail));
            context.setCols(new ArrayList<>(usersTable.buildColumns()));

            processor.process(context);

            assertThat(context.getDbJoins()).isNotEmpty();
        }

        @Test
        void process_nullFields_includesAllColumns() throws DbException {
            when(jdbcManager.getTable("test", null, "departments")).thenReturn(departmentsTable);

            JoinDetail joinDetail = new JoinDetail(
                    null, "departments", null, null,
                    List.of("department_id==id"), null, JoinType.LEFT
            );

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setRoot(usersTable);
            context.setJoins(List.of(joinDetail));
            context.setCols(new ArrayList<>(usersTable.buildColumns()));

            int initialColumns = context.getCols().size();

            processor.process(context);

            // Columns from joined table should be added
            assertThat(context.getCols().size()).isGreaterThan(initialColumns);
        }

        @Test
        void process_specificFields_includesOnlySpecified() throws DbException {
            when(jdbcManager.getTable("test", null, "departments")).thenReturn(departmentsTable);

            JoinDetail joinDetail = new JoinDetail(
                    null, "departments", null, List.of("name"),
                    List.of("department_id==id"), null, JoinType.LEFT
            );

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setRoot(usersTable);
            context.setJoins(List.of(joinDetail));
            context.setCols(new ArrayList<>(usersTable.buildColumns()));

            int initialColumns = context.getCols().size();

            processor.process(context);

            // Only 1 column (name) from joined table should be added
            assertThat(context.getCols()).hasSize(initialColumns + 1);
        }

        @Test
        void process_multipleOnConditions_addsAndCondition() throws DbException {
            when(jdbcManager.getTable("test", null, "orders")).thenReturn(ordersTable);

            // Two ON conditions: first uses addOn, second uses addAndCondition
            JoinDetail joinDetail = new JoinDetail(
                    null, "orders", null, List.of("total"),
                    List.of("id==user_id", "department_id==id"), null, JoinType.INNER
            );

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setRoot(usersTable);
            context.setJoins(List.of(joinDetail));
            context.setCols(new ArrayList<>(usersTable.buildColumns()));

            processor.process(context);

            assertThat(context.getDbJoins()).hasSize(1);
        }

        @Test
        void process_noWithTable_multipleJoins_usesLastRootTable() throws DbException {
            when(jdbcManager.getTable("test", null, "departments")).thenReturn(departmentsTable);
            when(jdbcManager.getTable("test", null, "orders")).thenReturn(ordersTable);

            // Second join has no withTable — should use previous join table as root
            JoinDetail join1 = new JoinDetail(
                    null, "departments", null, null,
                    List.of("department_id==id"), null, JoinType.LEFT
            );
            JoinDetail join2 = new JoinDetail(
                    null, "orders", null, null,
                    List.of("id==user_id"), null, JoinType.LEFT
            );

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setRoot(usersTable);
            context.setJoins(List.of(join1, join2));
            context.setCols(new ArrayList<>(usersTable.buildColumns()));

            processor.process(context);

            assertThat(context.getDbJoins()).hasSize(2);
        }

        @Test
        void process_withTableNotInList_fetchesFromJdbcManager() throws DbException {
            when(jdbcManager.getTable("test", null, "departments")).thenReturn(departmentsTable);
            when(jdbcManager.getTable("test", null, "orders")).thenReturn(ordersTable);
            // withTable "unknown_table" not in allJoinTables, so orElseGet fetches from jdbcManager
            when(jdbcManager.getTable("test", null, "unknown_table")).thenReturn(usersTable);

            JoinDetail join1 = new JoinDetail(
                    null, "departments", null, null,
                    List.of("department_id==id"), null, JoinType.LEFT
            );
            JoinDetail join2 = new JoinDetail(
                    null, "orders", "unknown_table", null,
                    List.of("id==user_id"), null, JoinType.LEFT
            );

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setRoot(usersTable);
            context.setJoins(List.of(join1, join2));
            context.setCols(new ArrayList<>(usersTable.buildColumns()));

            processor.process(context);

            assertThat(context.getDbJoins()).hasSize(2);
            verify(jdbcManager).getTable("test", null, "unknown_table");
        }

        @Test
        void process_withTableNotInList_fetchThrowsDbException_throwsDbRuntimeException() throws DbException {
            when(jdbcManager.getTable("test", null, "departments")).thenReturn(departmentsTable);
            // withTable "bad_table" not in allJoinTables, and fetching it throws
            when(jdbcManager.getTable("test", null, "bad_table"))
                    .thenThrow(new DbException(dev.suprim.query.exception.DbErrorCode.NOT_FOUND, "Table not found"));

            JoinDetail join1 = new JoinDetail(
                    null, "departments", null, null,
                    List.of("department_id==id"), null, JoinType.LEFT
            );
            JoinDetail join2 = new JoinDetail(
                    null, "orders", "bad_table", null,
                    List.of("id==user_id"), null, JoinType.LEFT
            );

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setRoot(usersTable);
            context.setJoins(List.of(join1, join2));
            context.setCols(new ArrayList<>(usersTable.buildColumns()));

            assertThatThrownBy(() -> processor.process(context))
                    .isInstanceOf(dev.suprim.query.exception.DbRuntimeException.class);
        }

        @Test
        void process_noOnConditions_joinsWithoutCondition() throws DbException {
            when(jdbcManager.getTable("test", null, "departments")).thenReturn(departmentsTable);

            // No ON conditions
            JoinDetail joinDetail = new JoinDetail(
                    null, "departments", null, List.of("name"),
                    null, null, JoinType.LEFT
            );

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setRoot(usersTable);
            context.setJoins(List.of(joinDetail));
            context.setCols(new ArrayList<>(usersTable.buildColumns()));

            processor.process(context);

            assertThat(context.getDbJoins()).hasSize(1);
        }

        @Test
        void process_invalidColumnInFields_throwsDbRuntimeException() throws DbException {
            when(jdbcManager.getTable("test", null, "departments")).thenReturn(departmentsTable);

            // "nonexistent_column" doesn't exist in departments table
            JoinDetail joinDetail = new JoinDetail(
                    null, "departments", null, List.of("nonexistent_column"),
                    List.of("department_id==id"), null, JoinType.LEFT
            );

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setRoot(usersTable);
            context.setJoins(List.of(joinDetail));
            context.setCols(new ArrayList<>(usersTable.buildColumns()));

            assertThatThrownBy(() -> processor.process(context))
                    .isInstanceOf(dev.suprim.query.exception.DbRuntimeException.class);
        }
    }
}
