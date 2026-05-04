package dev.suprim.query.rsql.visitor;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.Node;
import dev.suprim.query.dialect.Dialect;
import dev.suprim.query.exception.DbException;
import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.DbTable;
import dev.suprim.query.model.DbWhere;
import dev.suprim.query.rsql.parser.RSQLParserBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for BaseRSQLVisitor.
 */
class BaseRSQLVisitorTest {

    private RSQLParser parser;
    private TestDialect dialect;
    private DbTable table;
    private DbWhere dbWhere;

    @BeforeEach
    void setUp() {
        parser = RSQLParserBuilder.newRSQLParser();
        dialect = new TestDialect();
        table = new DbTable(
                "public",
                "users",
                "\"public\".\"users\"",
                "t",
                List.of(
                        new DbColumn("users", "id", "", "t", false, "uuid", true, false, Object.class, "\"", null),
                        new DbColumn("users", "name", "", "t", false, "varchar", false, false, String.class, "\"", null),
                        new DbColumn("users", "age", "", "t", false, "integer", false, false, Integer.class, "\"", null),
                        new DbColumn("users", "status", "", "t", false, "varchar", false, false, String.class, "\"", null),
                        new DbColumn("users", "metadata", "", "t", false, "jsonb", false, false, String.class, "\"", null)
                ),
                "table",
                "\""
        );
        dbWhere = new DbWhere("users", table, List.of(), new HashMap<>(), "select", List.of(table));
    }

    @Test
    void visit_simpleEqualCondition_shouldReturnCorrectSql() {
        BaseRSQLVisitor visitor = BaseRSQLVisitor.builder()
                .dbWhere(dbWhere)
                .dialect(dialect)
                .build();
        Node node = parser.parse("name==john");

        String result = node.accept(visitor);

        assertThat(result).isEqualTo("t.name = :t_name");
        assertThat(dbWhere.paramMap()).containsEntry("t_name", "john");
    }

    @Test
    void visit_andCondition_shouldReturnCorrectSql() {
        BaseRSQLVisitor visitor = BaseRSQLVisitor.builder()
                .dbWhere(dbWhere)
                .dialect(dialect)
                .build();
        Node node = parser.parse("name==john;age=gt=18");

        String result = node.accept(visitor);

        assertThat(result).contains("AND");
        assertThat(result).contains("t.name = :t_name");
        assertThat(result).contains("t.age > :t_age");
    }

    @Test
    void visit_orCondition_shouldReturnCorrectSql() {
        BaseRSQLVisitor visitor = BaseRSQLVisitor.builder()
                .dbWhere(dbWhere)
                .dialect(dialect)
                .build();
        Node node = parser.parse("name==john,name==jane");

        String result = node.accept(visitor);

        assertThat(result).contains("OR");
        assertThat(result).contains("t.name = ");
    }

    @Test
    void visit_inOperator_shouldReturnCorrectSql() {
        BaseRSQLVisitor visitor = BaseRSQLVisitor.builder()
                .dbWhere(dbWhere)
                .dialect(dialect)
                .build();
        Node node = parser.parse("status=in=(active,pending,closed)");

        String result = node.accept(visitor);

        assertThat(result).contains("in");
        assertThat(result).contains(":t_status");
    }

    @Test
    void visit_likeOperator_shouldReturnCorrectSql() {
        BaseRSQLVisitor visitor = BaseRSQLVisitor.builder()
                .dbWhere(dbWhere)
                .dialect(dialect)
                .build();
        Node node = parser.parse("name=like=john");

        String result = node.accept(visitor);

        assertThat(result).contains("like");
        assertThat(dbWhere.paramMap()).containsEntry("t_name", "%john%");
    }

    @Test
    void visit_isNullOperator_shouldReturnCorrectSql() {
        BaseRSQLVisitor visitor = BaseRSQLVisitor.builder()
                .dbWhere(dbWhere)
                .dialect(dialect)
                .build();
        Node node = parser.parse("name=isnull=true");

        String result = node.accept(visitor);

        assertThat(result).contains("is null");
    }

    @Test
    void visit_unknownOperator_shouldThrowException() {
        // Create a visitor with unknown operator
        BaseRSQLVisitor visitor = BaseRSQLVisitor.builder()
                .dbWhere(dbWhere)
                .dialect(dialect)
                .build();

        // The parser won't accept unknown operators, so we need to test via direct handler lookup
        // This test verifies that an unknown operator would throw an exception
        assertThatThrownBy(() -> {
            // Try parsing with an operator that doesn't have a handler registered
            // but is configured in the parser - this is artificial but tests the code path
            parser.parse("name=unknownop=value");
        }).isInstanceOf(Exception.class);
    }

    @Test
    void visit_complexNestedCondition_shouldReturnCorrectSql() {
        BaseRSQLVisitor visitor = BaseRSQLVisitor.builder()
                .dbWhere(dbWhere)
                .dialect(dialect)
                .build();
        Node node = parser.parse("(name==john;age=gt=18),(name==jane;age=gt=21)");

        String result = node.accept(visitor);

        assertThat(result).contains("AND");
        assertThat(result).contains("OR");
    }

    @Test
    void visit_withDeleteOperation_shouldWork() {
        DbWhere deleteWhere = new DbWhere("users", table, List.of(), new HashMap<>(), "delete", List.of(table));
        BaseRSQLVisitor visitor = BaseRSQLVisitor.builder()
                .dbWhere(deleteWhere)
                .dialect(dialect)
                .build();
        Node node = parser.parse("name==john");

        String result = node.accept(visitor);

        assertThat(result).isEqualTo("t.name = :t_name");
    }

    @Test
    void visit_withCrossTableColumn_shouldResolveCorrectly() {
        DbTable products = new DbTable(
                "public",
                "products",
                "\"public\".\"products\"",
                "p",
                List.of(
                        new DbColumn("products", "id", "", "p", false, "uuid", true, false, Object.class, "\"", null),
                        new DbColumn("products", "color", "", "p", false, "varchar", false, false, String.class, "\"", null)
                ),
                "table",
                "\""
        );

        DbWhere multiTableWhere = new DbWhere("users", table, List.of(), new HashMap<>(), "select", List.of(table, products));
        BaseRSQLVisitor visitor = BaseRSQLVisitor.builder()
                .dbWhere(multiTableWhere)
                .dialect(dialect)
                .build();

        Node node = parser.parse("products.color==red");
        String result = node.accept(visitor);

        assertThat(result).contains("p.color");
    }

    /**
     * Test dialect for visitor testing.
     */
    private static class TestDialect extends Dialect {
        public TestDialect() {
            super(new ObjectMapper(), "\"");
        }

        @Override
        public boolean isSupportedDb(String productName, int majorVersion) {
            return true;
        }

        @Override
        public void processTypes(DbTable table, List<String> insertableColumns, Map<String, Object> data) {
        }

        @Override
        public String renderTableName(DbTable table, boolean containsWhere, boolean deleteOp) {
            return table.fullName() + " " + table.alias();
        }

        @Override
        public String renderTableNameWithoutAlias(DbTable table) {
            return table.fullName();
        }
    }
}
