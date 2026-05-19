package dev.suprim.query.jdbc.operation;

import dev.suprim.query.dialect.Dialect;
import dev.suprim.query.exception.DbException;
import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.DbJoin;
import dev.suprim.query.model.DbSort;
import dev.suprim.query.model.DbTable;
import dev.suprim.query.model.ExpressionField;
import dev.suprim.query.model.context.CreateContext;
import dev.suprim.query.model.context.DeleteContext;
import dev.suprim.query.model.context.InsertableColumn;
import dev.suprim.query.model.context.ReadContext;
import dev.suprim.query.model.context.UpdateContext;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests SqlCreatorTemplate with real JTE template engine to cover all template rendering paths.
 */
@ExtendWith(MockitoExtension.class)
class SqlCreatorTemplateTest {

    @Mock
    private JdbcManager jdbcManager;

    @Mock
    private Dialect dialect;

    private SqlCreatorTemplate sqlCreatorTemplate;

    private static final DbTable USERS_TABLE = new DbTable(
            "public", "users", "public.users", "u",
            List.of(
                    new DbColumn("users", "id", "", "u", true, "int8", false, false, Long.class, "\"", ""),
                    new DbColumn("users", "name", "", "u", false, "varchar", false, false, String.class, "\"", ""),
                    new DbColumn("users", "email", "", "u", false, "varchar", false, false, String.class, "\"", "")
            ),
            "TABLE", "\""
    );

    @BeforeEach
    void setUp() {
        TemplateEngine templateEngine = TemplateEngine.createPrecompiled(ContentType.Plain);
        sqlCreatorTemplate = new SqlCreatorTemplate(templateEngine, jdbcManager);
    }

    @Nested
    @DisplayName("query() - read template")
    class QueryTests {

        @Test
        void query_simpleSelect_rendersCorrectly() throws DbException {
            when(jdbcManager.getDialect("db1")).thenReturn(dialect);
            when(dialect.getReadSqlTemplate()).thenReturn("read");

            ReadContext ctx = new ReadContext();
            ctx.setDbId("db1");
            ctx.setRoot(USERS_TABLE);
            ctx.setCols(USERS_TABLE.buildColumns());
            ctx.setLimit(-1);
            ctx.setDefaultFetchLimit(100);

            String sql = sqlCreatorTemplate.query(ctx);

            assertThat(sql).contains("SELECT");
            assertThat(sql).contains("FROM");
            assertThat(sql).contains("LIMIT");
        }

        @Test
        void query_withSorts_includesOrderBy() throws DbException {
            when(jdbcManager.getDialect("db1")).thenReturn(dialect);
            when(dialect.getReadSqlTemplate()).thenReturn("read");

            ReadContext ctx = new ReadContext();
            ctx.setDbId("db1");
            ctx.setRoot(USERS_TABLE);
            ctx.setCols(USERS_TABLE.buildColumns());
            ctx.setDbSortList(List.of(new DbSort("users", "u", "name", "ASC")));
            ctx.setLimit(10);

            String sql = sqlCreatorTemplate.query(ctx);

            assertThat(sql).contains("ORDER BY");
            assertThat(sql).contains("LIMIT");
        }

        @Test
        void query_withOffset_includesOffset() throws DbException {
            when(jdbcManager.getDialect("db1")).thenReturn(dialect);
            when(dialect.getReadSqlTemplate()).thenReturn("read");

            ReadContext ctx = new ReadContext();
            ctx.setDbId("db1");
            ctx.setRoot(USERS_TABLE);
            ctx.setCols(USERS_TABLE.buildColumns());
            ctx.setLimit(10);
            ctx.setOffset(5);

            String sql = sqlCreatorTemplate.query(ctx);

            assertThat(sql).contains("LIMIT");
            assertThat(sql).contains("OFFSET");
        }

        @Test
        void query_withWhere_includesWhereClause() throws DbException {
            when(jdbcManager.getDialect("db1")).thenReturn(dialect);
            when(dialect.getReadSqlTemplate()).thenReturn("read");

            ReadContext ctx = new ReadContext();
            ctx.setDbId("db1");
            ctx.setRoot(USERS_TABLE);
            ctx.setCols(USERS_TABLE.buildColumns());
            ctx.setRootWhere("u.\"id\" = :id");
            ctx.setLimit(10);

            String sql = sqlCreatorTemplate.query(ctx);

            assertThat(sql).contains("WHERE");
        }

        @Test
        void query_withJoins_includesJoinClause() throws DbException {
            when(jdbcManager.getDialect("db1")).thenReturn(dialect);
            when(dialect.getReadSqlTemplate()).thenReturn("read");

            DbJoin join = DbJoin.builder()
                    .tableName("\"public\".\"orders\"")
                    .alias("o")
                    .joinType("LEFT")
                    .build();

            DbColumn leftCol = new DbColumn("users", "id", "", "u", true, "int8", false, false, Long.class, "\"", "");
            DbColumn rightCol = new DbColumn("orders", "user_id", "", "o", false, "int8", false, false, Long.class, "\"", "");
            join.addOn(leftCol, "=", rightCol);

            ReadContext ctx = new ReadContext();
            ctx.setDbId("db1");
            ctx.setRoot(USERS_TABLE);
            ctx.setCols(USERS_TABLE.buildColumns());
            ctx.addJoin(join);
            ctx.setLimit(10);

            String sql = sqlCreatorTemplate.query(ctx);

            assertThat(sql).contains("LEFT JOIN");
        }

        @Test
        void query_noSortsNoLimitNoOffset_rendersMinimal() throws DbException {
            when(jdbcManager.getDialect("db1")).thenReturn(dialect);
            when(dialect.getReadSqlTemplate()).thenReturn("read");

            ReadContext ctx = new ReadContext();
            ctx.setDbId("db1");
            ctx.setRoot(USERS_TABLE);
            ctx.setCols(USERS_TABLE.buildColumns());
            ctx.setLimit(-2); // won't trigger limit (not > -1 and not == -1)
            ctx.setOffset(-1); // won't trigger offset (not > -1)

            String sql = sqlCreatorTemplate.query(ctx);

            assertThat(sql).contains("SELECT");
            assertThat(sql).doesNotContain("ORDER BY");
            assertThat(sql).doesNotContain("LIMIT");
            assertThat(sql).doesNotContain("OFFSET");
        }

        @Test
        void query_withEmptySortList_doesNotIncludeOrderBy() throws DbException {
            when(jdbcManager.getDialect("db1")).thenReturn(dialect);
            when(dialect.getReadSqlTemplate()).thenReturn("read");

            ReadContext ctx = new ReadContext();
            ctx.setDbId("db1");
            ctx.setRoot(USERS_TABLE);
            ctx.setCols(USERS_TABLE.buildColumns());
            ctx.setDbSortList(List.of()); // non-null but empty
            ctx.setLimit(10);

            String sql = sqlCreatorTemplate.query(ctx);

            assertThat(sql).contains("SELECT");
            assertThat(sql).doesNotContain("ORDER BY");
        }

        @Test
        void query_withLimitButNoOffset_rendersLimitWithoutOffset() throws DbException {
            when(jdbcManager.getDialect("db1")).thenReturn(dialect);
            when(dialect.getReadSqlTemplate()).thenReturn("read");

            ReadContext ctx = new ReadContext();
            ctx.setDbId("db1");
            ctx.setRoot(USERS_TABLE);
            ctx.setCols(USERS_TABLE.buildColumns());
            ctx.setLimit(10);
            ctx.setOffset(-1); // offset not > -1, so not included in params

            String sql = sqlCreatorTemplate.query(ctx);

            assertThat(sql).contains("LIMIT");
            assertThat(sql).doesNotContain("OFFSET");
        }

        @Test
        void query_withExpressions_rendersExpressionsInSelect() throws DbException {
            when(jdbcManager.getDialect("db1")).thenReturn(dialect);
            when(dialect.getReadSqlTemplate()).thenReturn("read");

            ReadContext ctx = new ReadContext();
            ctx.setDbId("db1");
            ctx.setRoot(USERS_TABLE);
            ctx.setCols(USERS_TABLE.buildColumns());
            ctx.setExpressions(List.of(ExpressionField.count("*").as("total")));
            ctx.setLimit(10);

            String sql = sqlCreatorTemplate.query(ctx);

            assertThat(sql).contains("COUNT(*)");
            assertThat(sql).contains("AS \"total\"");
        }

        @Test
        void query_expressionsOnly_rendersWithoutColumns() throws DbException {
            when(jdbcManager.getDialect("db1")).thenReturn(dialect);
            when(dialect.getReadSqlTemplate()).thenReturn("read");

            ReadContext ctx = new ReadContext();
            ctx.setDbId("db1");
            ctx.setRoot(USERS_TABLE);
            ctx.setCols(List.of());
            ctx.setExpressions(List.of(
                    ExpressionField.count("*").as("total"),
                    ExpressionField.max("id").as("max_id")
            ));
            ctx.setLimit(10);

            String sql = sqlCreatorTemplate.query(ctx);

            assertThat(sql).contains("COUNT(*)");
            assertThat(sql).contains("MAX(\"id\")");
            assertThat(sql).doesNotContain("u.\"id\"");
        }

        @Test
        void query_nullExpressions_rendersColumnsOnly() throws DbException {
            when(jdbcManager.getDialect("db1")).thenReturn(dialect);
            when(dialect.getReadSqlTemplate()).thenReturn("read");

            ReadContext ctx = new ReadContext();
            ctx.setDbId("db1");
            ctx.setRoot(USERS_TABLE);
            ctx.setCols(USERS_TABLE.buildColumns());
            ctx.setExpressions(null);
            ctx.setLimit(10);

            String sql = sqlCreatorTemplate.query(ctx);

            assertThat(sql).contains("SELECT");
            assertThat(sql).contains("u.\"id\"");
        }

        @Test
        void query_nullColumns_withExpressions_rendersExpressionsOnly() throws DbException {
            when(jdbcManager.getDialect("db1")).thenReturn(dialect);
            when(dialect.getReadSqlTemplate()).thenReturn("read");

            ReadContext ctx = new ReadContext();
            ctx.setDbId("db1");
            ctx.setRoot(USERS_TABLE);
            ctx.setCols(null);
            ctx.setExpressions(List.of(ExpressionField.sum("id").as("total_id")));
            ctx.setLimit(10);

            String sql = sqlCreatorTemplate.query(ctx);

            assertThat(sql).contains("SUM(\"id\") AS \"total_id\"");
            assertThat(sql).doesNotContain("u.\"id\"");
        }
    }

    @Nested
    @DisplayName("findOne() - find-one template")
    class FindOneTests {

        @Test
        void findOne_withWhere_rendersCorrectly() throws DbException {
            when(jdbcManager.getDialect("db1")).thenReturn(dialect);
            when(dialect.getFindOneSqlTemplate()).thenReturn("find-one");

            ReadContext ctx = new ReadContext();
            ctx.setDbId("db1");
            ctx.setRoot(USERS_TABLE);
            ctx.setCols(USERS_TABLE.buildColumns());
            ctx.setRootWhere("u.\"id\" = :id");

            String sql = sqlCreatorTemplate.findOne(ctx);

            assertThat(sql).contains("SELECT");
            assertThat(sql).contains("FROM");
            assertThat(sql).contains("WHERE");
        }

        @Test
        void findOne_withoutWhere_rendersWithoutWhereClause() throws DbException {
            when(jdbcManager.getDialect("db1")).thenReturn(dialect);
            when(dialect.getFindOneSqlTemplate()).thenReturn("find-one");

            ReadContext ctx = new ReadContext();
            ctx.setDbId("db1");
            ctx.setRoot(USERS_TABLE);
            ctx.setCols(USERS_TABLE.buildColumns());

            String sql = sqlCreatorTemplate.findOne(ctx);

            assertThat(sql).contains("SELECT");
            assertThat(sql).doesNotContain("WHERE");
        }
    }

    @Nested
    @DisplayName("count() - count template")
    class CountTests {

        @Test
        void count_withWhere_rendersCorrectly() throws DbException {
            when(jdbcManager.getDialect("db1")).thenReturn(dialect);
            when(dialect.getCountSqlTemplate()).thenReturn("count");

            ReadContext ctx = new ReadContext();
            ctx.setDbId("db1");
            ctx.setRoot(USERS_TABLE);
            ctx.setRootWhere("u.\"id\" > :min_id");

            String sql = sqlCreatorTemplate.count(ctx);

            assertThat(sql).contains("COUNT(*)");
            assertThat(sql).contains("WHERE");
        }

        @Test
        void count_withoutWhere_rendersWithoutWhereClause() throws DbException {
            when(jdbcManager.getDialect("db1")).thenReturn(dialect);
            when(dialect.getCountSqlTemplate()).thenReturn("count");

            ReadContext ctx = new ReadContext();
            ctx.setDbId("db1");
            ctx.setRoot(USERS_TABLE);

            String sql = sqlCreatorTemplate.count(ctx);

            assertThat(sql).contains("COUNT(*)");
            assertThat(sql).doesNotContain("WHERE");
        }
    }

    @Nested
    @DisplayName("exists() - exists template")
    class ExistsTests {

        @Test
        void exists_withWhere_rendersCorrectly() throws DbException {
            when(jdbcManager.getDialect("db1")).thenReturn(dialect);
            when(dialect.getExistSqlTemplate()).thenReturn("exists");

            ReadContext ctx = new ReadContext();
            ctx.setDbId("db1");
            ctx.setRoot(USERS_TABLE);
            ctx.setRootWhere("u.\"id\" = :id");

            String sql = sqlCreatorTemplate.exists(ctx);

            assertThat(sql).contains("SELECT");
            assertThat(sql).contains("1");
            assertThat(sql).contains("WHERE");
            assertThat(sql).contains("LIMIT 1");
        }

        @Test
        void exists_withJoins_rendersJoinClause() throws DbException {
            when(jdbcManager.getDialect("db1")).thenReturn(dialect);
            when(dialect.getExistSqlTemplate()).thenReturn("exists");

            DbJoin join = DbJoin.builder()
                    .tableName("\"public\".\"orders\"")
                    .alias("o")
                    .joinType("INNER")
                    .build();
            DbColumn leftCol = new DbColumn("users", "id", "", "u", true, "int8", false, false, Long.class, "\"", "");
            DbColumn rightCol = new DbColumn("orders", "user_id", "", "o", false, "int8", false, false, Long.class, "\"", "");
            join.addOn(leftCol, "=", rightCol);

            ReadContext ctx = new ReadContext();
            ctx.setDbId("db1");
            ctx.setRoot(USERS_TABLE);
            ctx.setRootWhere("u.\"id\" = :id");
            ctx.addJoin(join);

            String sql = sqlCreatorTemplate.exists(ctx);

            assertThat(sql).contains("JOIN");
            assertThat(sql).contains("LIMIT 1");
        }

        @Test
        void exists_withoutWhere_rendersWithoutWhereClause() throws DbException {
            when(jdbcManager.getDialect("db1")).thenReturn(dialect);
            when(dialect.getExistSqlTemplate()).thenReturn("exists");

            ReadContext ctx = new ReadContext();
            ctx.setDbId("db1");
            ctx.setRoot(USERS_TABLE);

            String sql = sqlCreatorTemplate.exists(ctx);

            assertThat(sql).contains("SELECT");
            assertThat(sql).doesNotContain("WHERE");
            assertThat(sql).contains("LIMIT 1");
        }
    }

    @Nested
    @DisplayName("create() - insert template")
    class CreateTests {

        @Test
        void create_rendersInsertStatement() throws DbException {
            when(jdbcManager.getDialect("db1")).thenReturn(dialect);
            when(dialect.getInsertSqlTemplate()).thenReturn("insert");

            CreateContext ctx = new CreateContext(
                    "db1",
                    USERS_TABLE,
                    List.of("name", "email"),
                    List.of(new InsertableColumn("name", null), new InsertableColumn("email", null))
            );

            String sql = sqlCreatorTemplate.create(ctx);

            assertThat(sql).contains("INSERT INTO");
            assertThat(sql).contains("public.users");
            assertThat(sql).contains("VALUES");
        }
    }

    @Nested
    @DisplayName("updateQuery() - update template")
    class UpdateQueryTests {

        @Test
        void updateQuery_withAlias_rendersCorrectly() throws DbException {
            when(jdbcManager.getDialect("db1")).thenReturn(dialect);
            when(dialect.getUpdateSqlTemplate()).thenReturn("update");
            when(dialect.supportAlias()).thenReturn(true);

            UpdateContext ctx = UpdateContext.builder()
                    .dbId("db1")
                    .tableName("users")
                    .table(USERS_TABLE)
                    .updatableColumns(List.of("name"))
                    .build();
            ctx.setWhere("u.\"id\" = :id");

            String sql = sqlCreatorTemplate.updateQuery(ctx);

            assertThat(sql).contains("UPDATE");
            assertThat(sql).contains("SET");
            assertThat(sql).contains("WHERE");
        }

        @Test
        void updateQuery_withoutAlias_usesTableName() throws DbException {
            when(jdbcManager.getDialect("db1")).thenReturn(dialect);
            when(dialect.getUpdateSqlTemplate()).thenReturn("update");
            when(dialect.supportAlias()).thenReturn(false);

            UpdateContext ctx = UpdateContext.builder()
                    .dbId("db1")
                    .tableName("users")
                    .table(USERS_TABLE)
                    .updatableColumns(List.of("name"))
                    .build();
            ctx.setWhere("\"id\" = :id");

            String sql = sqlCreatorTemplate.updateQuery(ctx);

            assertThat(sql).contains("UPDATE");
            assertThat(sql).contains("users");
            assertThat(sql).contains("SET");
        }

        @Test
        void updateQuery_withoutWhere_rendersWithoutWhereClause() throws DbException {
            when(jdbcManager.getDialect("db1")).thenReturn(dialect);
            when(dialect.getUpdateSqlTemplate()).thenReturn("update");
            when(dialect.supportAlias()).thenReturn(true);

            UpdateContext ctx = UpdateContext.builder()
                    .dbId("db1")
                    .tableName("users")
                    .table(USERS_TABLE)
                    .updatableColumns(List.of("name"))
                    .build();

            String sql = sqlCreatorTemplate.updateQuery(ctx);

            assertThat(sql).contains("UPDATE");
            assertThat(sql).contains("SET");
            assertThat(sql).doesNotContain("WHERE");
        }
    }

    @Nested
    @DisplayName("deleteQuery() - delete template")
    class DeleteQueryTests {

        @Test
        void deleteQuery_withWhere_rendersCorrectly() throws DbException {
            when(jdbcManager.getDialect("db1")).thenReturn(dialect);
            when(dialect.getDeleteSqlTemplate()).thenReturn("delete");
            when(dialect.renderTableName(USERS_TABLE, true, true))
                    .thenReturn("\"public\".\"users\" u");

            DeleteContext ctx = DeleteContext.builder()
                    .dbId("db1")
                    .tableName("users")
                    .table(USERS_TABLE)
                    .build();
            ctx.setWhere("u.\"id\" = :id");

            String sql = sqlCreatorTemplate.deleteQuery(ctx);

            assertThat(sql).contains("DELETE FROM");
            assertThat(sql).contains("WHERE");
        }

        @Test
        void deleteQuery_withoutWhere_rendersWithoutWhereClause() throws DbException {
            when(jdbcManager.getDialect("db1")).thenReturn(dialect);
            when(dialect.getDeleteSqlTemplate()).thenReturn("delete");
            when(dialect.renderTableName(USERS_TABLE, false, true))
                    .thenReturn("\"public\".\"users\" u");

            DeleteContext ctx = DeleteContext.builder()
                    .dbId("db1")
                    .tableName("users")
                    .table(USERS_TABLE)
                    .build();

            String sql = sqlCreatorTemplate.deleteQuery(ctx);

            assertThat(sql).contains("DELETE FROM");
            assertThat(sql).doesNotContain("WHERE");
        }

        @Test
        void deleteQuery_withBlankWhere_passesHasWhereAsFalseToDialect() throws DbException {
            when(jdbcManager.getDialect("db1")).thenReturn(dialect);
            when(dialect.getDeleteSqlTemplate()).thenReturn("delete");
            // The key assertion: blank where passes `false` as hasWhere to renderTableName
            when(dialect.renderTableName(USERS_TABLE, false, true))
                    .thenReturn("\"public\".\"users\" u");

            DeleteContext ctx = DeleteContext.builder()
                    .dbId("db1")
                    .tableName("users")
                    .table(USERS_TABLE)
                    .build();
            ctx.setWhere("   "); // blank where — nonNull && !isBlank() == false

            String sql = sqlCreatorTemplate.deleteQuery(ctx);

            assertThat(sql).contains("DELETE FROM");
            // Verify renderTableName was called with hasWhere=false
            verify(dialect).renderTableName(USERS_TABLE, false, true);
        }
    }

    @Nested
    @DisplayName("softDeleteQuery() - soft delete template")
    class SoftDeleteQueryTests {

        @Test
        void softDeleteQuery_withAlias_rendersQualifiedColumn() throws DbException {
            when(jdbcManager.getDialect("db1")).thenReturn(dialect);
            when(dialect.supportAlias()).thenReturn(true);
            when(dialect.currentTimestamp()).thenReturn("NOW()");
            when(dialect.getUpdateSqlTemplate()).thenReturn("update");

            DeleteContext ctx = DeleteContext.builder()
                    .dbId("db1")
                    .tableName("users")
                    .table(USERS_TABLE)
                    .build();
            ctx.setWhere("u.\"id\" = :id");

            String sql = sqlCreatorTemplate.softDeleteQuery(ctx, "deleted_at");

            assertThat(sql).contains("UPDATE");
            assertThat(sql).contains("SET");
            assertThat(sql).contains("u.deleted_at = NOW()");
        }

        @Test
        void softDeleteQuery_withoutAlias_rendersUnqualifiedColumn() throws DbException {
            DbTable noAliasTable = new DbTable(
                    "public", "users", "public.users", "",
                    USERS_TABLE.dbColumns(), "TABLE", "\""
            );

            when(jdbcManager.getDialect("db1")).thenReturn(dialect);
            when(dialect.supportAlias()).thenReturn(false);
            when(dialect.currentTimestamp()).thenReturn("NOW()");
            when(dialect.getUpdateSqlTemplate()).thenReturn("update");

            DeleteContext ctx = DeleteContext.builder()
                    .dbId("db1")
                    .tableName("users")
                    .table(noAliasTable)
                    .build();

            String sql = sqlCreatorTemplate.softDeleteQuery(ctx, "deleted_at");

            assertThat(sql).contains("UPDATE");
            assertThat(sql).contains("deleted_at = NOW()");
            assertThat(sql).doesNotContain(".deleted_at");
        }

        @Test
        void softDeleteQuery_nullAlias_rendersUnqualifiedColumn() throws DbException {
            DbTable nullAliasTable = new DbTable(
                    "public", "users", "public.users", null,
                    USERS_TABLE.dbColumns(), "TABLE", "\""
            );

            when(jdbcManager.getDialect("db1")).thenReturn(dialect);
            when(dialect.supportAlias()).thenReturn(true);
            when(dialect.currentTimestamp()).thenReturn("CURRENT_TIMESTAMP");
            when(dialect.getUpdateSqlTemplate()).thenReturn("update");

            DeleteContext ctx = DeleteContext.builder()
                    .dbId("db1")
                    .tableName("users")
                    .table(nullAliasTable)
                    .build();

            String sql = sqlCreatorTemplate.softDeleteQuery(ctx, "deleted_at");

            assertThat(sql).contains("deleted_at = CURRENT_TIMESTAMP");
        }
    }

    @Nested
    @DisplayName("upsert() - upsert template")
    class UpsertTests {

        @Test
        void upsert_rendersInsertWithOnConflict() throws DbException {
            when(jdbcManager.getDialect("db1")).thenReturn(dialect);
            when(dialect.getUpsertSqlTemplate()).thenReturn("upsert");

            CreateContext ctx = new CreateContext(
                    "db1",
                    USERS_TABLE,
                    List.of("name", "email"),
                    List.of(new InsertableColumn("name", null), new InsertableColumn("email", null))
            );

            String sql = sqlCreatorTemplate.upsert(ctx, "ON CONFLICT (\"id\") DO UPDATE SET \"name\" = EXCLUDED.\"name\"");

            assertThat(sql).contains("INSERT INTO");
            assertThat(sql).contains("ON CONFLICT");
            assertThat(sql).contains("DO UPDATE SET");
        }
    }

    @Nested
    @DisplayName("Template not found")
    class TemplateNotFoundTests {

        @Test
        void renderSqlTemplate_invalidTemplate_throwsDbException() throws DbException {
            when(jdbcManager.getDialect("db1")).thenReturn(dialect);
            when(dialect.getReadSqlTemplate()).thenReturn("nonexistent-template");

            ReadContext ctx = new ReadContext();
            ctx.setDbId("db1");
            ctx.setRoot(USERS_TABLE);
            ctx.setCols(USERS_TABLE.buildColumns());
            ctx.setLimit(10);

            assertThatThrownBy(() -> sqlCreatorTemplate.query(ctx))
                    .isInstanceOf(DbException.class);
        }
    }
}
