package dev.suprim.query.rsql.handler;

import dev.suprim.query.exception.DbException;
import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.DbWhere;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for comparison operator handlers: =, !=, >, >=, <, <=
 */
class ComparisonOperatorHandlersTest extends OperatorHandlerTestBase {

    private TestDialect dialect;
    private TestDialect dialectNoAlias;
    private DbColumn column;
    private DbWhere dbWhere;
    private Map<String, Object> paramMap;

    @BeforeEach
    void setUp() {
        dialect = createDialect();
        dialectNoAlias = createDialectWithoutAlias();
        column = createColumn("age", "integer", Integer.class);
        dbWhere = createDbWhere();
        paramMap = createParamMap();
    }

    // EqualToOperatorHandler tests
    @Test
    void equalTo_withAlias_shouldReturnCorrectSql() throws DbException {
        EqualToOperatorHandler handler = new EqualToOperatorHandler();

        String result = handler.handle(dialect, column, dbWhere, "25", Integer.class, paramMap);

        assertThat(result).isEqualTo("t.age = :t_age");
        assertThat(paramMap).containsEntry("t_age", 25);
    }

    @Test
    void equalTo_withoutAlias_shouldReturnCorrectSql() throws DbException {
        EqualToOperatorHandler handler = new EqualToOperatorHandler();

        String result = handler.handle(dialectNoAlias, column, dbWhere, "25", Integer.class, paramMap);

        assertThat(result).isEqualTo("age = :age");
        assertThat(paramMap).containsEntry("age", 25);
    }

    @Test
    void equalTo_withDeleteOp_shouldWork() throws DbException {
        EqualToOperatorHandler handler = new EqualToOperatorHandler();
        DbWhere deleteWhere = createDbWhere("delete");

        String result = handler.handle(dialect, column, deleteWhere, "25", Integer.class, paramMap);

        assertThat(result).contains("= :t_age");
    }

    // NotEqualToOperatorHandler tests
    @Test
    void notEqualTo_withAlias_shouldReturnCorrectSql() throws DbException {
        NotEqualToOperatorHandler handler = new NotEqualToOperatorHandler();

        String result = handler.handle(dialect, column, dbWhere, "25", Integer.class, paramMap);

        assertThat(result).isEqualTo("t.age != :t_age");
        assertThat(paramMap).containsEntry("t_age", 25);
    }

    @Test
    void notEqualTo_withoutAlias_shouldReturnCorrectSql() throws DbException {
        NotEqualToOperatorHandler handler = new NotEqualToOperatorHandler();

        String result = handler.handle(dialectNoAlias, column, dbWhere, "25", Integer.class, paramMap);

        assertThat(result).isEqualTo("age != :age");
    }

    // GreaterThanOperatorHandler tests
    @Test
    void greaterThan_withAlias_shouldReturnCorrectSql() throws DbException {
        GreaterThanOperatorHandler handler = new GreaterThanOperatorHandler();

        String result = handler.handle(dialect, column, dbWhere, "18", Integer.class, paramMap);

        assertThat(result).isEqualTo("t.age > :t_age");
        assertThat(paramMap).containsEntry("t_age", 18);
    }

    @Test
    void greaterThan_withoutAlias_shouldReturnCorrectSql() throws DbException {
        GreaterThanOperatorHandler handler = new GreaterThanOperatorHandler();

        String result = handler.handle(dialectNoAlias, column, dbWhere, "18", Integer.class, paramMap);

        assertThat(result).isEqualTo("age > :age");
    }

    // GreaterThanEqualToOperatorHandler tests
    @Test
    void greaterThanEqualTo_withAlias_shouldReturnCorrectSql() throws DbException {
        GreaterThanEqualToOperatorHandler handler = new GreaterThanEqualToOperatorHandler();

        String result = handler.handle(dialect, column, dbWhere, "18", Integer.class, paramMap);

        assertThat(result).isEqualTo("t.age >= :t_age");
        assertThat(paramMap).containsEntry("t_age", 18);
    }

    @Test
    void greaterThanEqualTo_withoutAlias_shouldReturnCorrectSql() throws DbException {
        GreaterThanEqualToOperatorHandler handler = new GreaterThanEqualToOperatorHandler();

        String result = handler.handle(dialectNoAlias, column, dbWhere, "18", Integer.class, paramMap);

        assertThat(result).isEqualTo("age >= :age");
    }

    // LessThanOperatorHandler tests
    @Test
    void lessThan_withAlias_shouldReturnCorrectSql() throws DbException {
        LessThanOperatorHandler handler = new LessThanOperatorHandler();

        String result = handler.handle(dialect, column, dbWhere, "65", Integer.class, paramMap);

        assertThat(result).isEqualTo("t.age < :t_age");
        assertThat(paramMap).containsEntry("t_age", 65);
    }

    @Test
    void lessThan_withoutAlias_shouldReturnCorrectSql() throws DbException {
        LessThanOperatorHandler handler = new LessThanOperatorHandler();

        String result = handler.handle(dialectNoAlias, column, dbWhere, "65", Integer.class, paramMap);

        assertThat(result).isEqualTo("age < :age");
    }

    // LessThanEqualToOperatorHandler tests
    @Test
    void lessThanEqualTo_withAlias_shouldReturnCorrectSql() throws DbException {
        LessThanEqualToOperatorHandler handler = new LessThanEqualToOperatorHandler();

        String result = handler.handle(dialect, column, dbWhere, "65", Integer.class, paramMap);

        assertThat(result).isEqualTo("t.age <= :t_age");
        assertThat(paramMap).containsEntry("t_age", 65);
    }

    @Test
    void lessThanEqualTo_withoutAlias_shouldReturnCorrectSql() throws DbException {
        LessThanEqualToOperatorHandler handler = new LessThanEqualToOperatorHandler();

        String result = handler.handle(dialectNoAlias, column, dbWhere, "65", Integer.class, paramMap);

        assertThat(result).isEqualTo("age <= :age");
    }
}
