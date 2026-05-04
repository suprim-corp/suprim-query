package dev.suprim.query.rsql.handler;

import dev.suprim.query.exception.DbException;
import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.DbWhere;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for null check and in/not-in operator handlers.
 */
class NullAndInOperatorHandlersTest extends OperatorHandlerTestBase {

    private TestDialect dialect;
    private TestDialect dialectNoAlias;
    private DbColumn column;
    private DbWhere dbWhere;
    private Map<String, Object> paramMap;

    @BeforeEach
    void setUp() {
        dialect = createDialect();
        dialectNoAlias = createDialectWithoutAlias();
        column = createColumn("status", "varchar", String.class);
        dbWhere = createDbWhere();
        paramMap = createParamMap();
    }

    // IsNullOperatorHandler tests
    @Test
    void isNull_withAlias_shouldReturnCorrectSql() throws DbException {
        IsNullOperatorHandler handler = new IsNullOperatorHandler();

        String result = handler.handle(dialect, column, dbWhere, "true", String.class, paramMap);

        assertThat(result).isEqualTo("t.status is null ");
        assertThat(paramMap).isEmpty();
    }

    @Test
    void isNull_withoutAlias_shouldReturnCorrectSql() throws DbException {
        IsNullOperatorHandler handler = new IsNullOperatorHandler();

        String result = handler.handle(dialectNoAlias, column, dbWhere, "true", String.class, paramMap);

        assertThat(result).isEqualTo("status is null ");
    }

    // IsNotNullOperatorHandler tests
    @Test
    void isNotNull_withAlias_shouldReturnCorrectSql() throws DbException {
        IsNotNullOperatorHandler handler = new IsNotNullOperatorHandler();

        String result = handler.handle(dialect, column, dbWhere, "true", String.class, paramMap);

        assertThat(result).isEqualTo("t.status is not null ");
        assertThat(paramMap).isEmpty();
    }

    @Test
    void isNotNull_withoutAlias_shouldReturnCorrectSql() throws DbException {
        IsNotNullOperatorHandler handler = new IsNotNullOperatorHandler();

        String result = handler.handle(dialectNoAlias, column, dbWhere, "true", String.class, paramMap);

        assertThat(result).isEqualTo("status is not null ");
    }

    // InOperatorHandler tests
    @Test
    void in_withSingleValue_withAlias_shouldReturnCorrectSql() throws DbException {
        InOperatorHandler handler = new InOperatorHandler();

        String result = handler.handle(dialect, column, dbWhere, "active", String.class, paramMap);

        assertThat(result).isEqualTo("t.status in  ( :t_status ) ");
        assertThat(paramMap).containsKey("t_status");
    }

    @Test
    void in_withMultipleValues_withAlias_shouldReturnCorrectSql() throws DbException {
        InOperatorHandler handler = new InOperatorHandler();

        String result = handler.handle(dialect, column, dbWhere, List.of("active", "pending", "closed"), String.class, paramMap);

        assertThat(result).isEqualTo("t.status in  ( :t_status ) ");
        assertThat(paramMap.get("t_status")).isEqualTo(List.of("active", "pending", "closed"));
    }

    @Test
    void in_withoutAlias_shouldReturnCorrectSql() throws DbException {
        InOperatorHandler handler = new InOperatorHandler();

        String result = handler.handle(dialectNoAlias, column, dbWhere, List.of("active", "pending"), String.class, paramMap);

        assertThat(result).isEqualTo("status in  ( :status ) ");
        assertThat(paramMap.get("status")).isEqualTo(List.of("active", "pending"));
    }

    // NotInOperatorHandler tests
    @Test
    void notIn_withSingleValue_withAlias_shouldReturnCorrectSql() throws DbException {
        NotInOperatorHandler handler = new NotInOperatorHandler();

        String result = handler.handle(dialect, column, dbWhere, "deleted", String.class, paramMap);

        assertThat(result).isEqualTo("t.status not in  ( :t_status ) ");
        assertThat(paramMap).containsKey("t_status");
    }

    @Test
    void notIn_withMultipleValues_withAlias_shouldReturnCorrectSql() throws DbException {
        NotInOperatorHandler handler = new NotInOperatorHandler();

        String result = handler.handle(dialect, column, dbWhere, List.of("deleted", "archived"), String.class, paramMap);

        assertThat(result).isEqualTo("t.status not in  ( :t_status ) ");
        assertThat(paramMap.get("t_status")).isEqualTo(List.of("deleted", "archived"));
    }

    @Test
    void notIn_withoutAlias_shouldReturnCorrectSql() throws DbException {
        NotInOperatorHandler handler = new NotInOperatorHandler();

        String result = handler.handle(dialectNoAlias, column, dbWhere, List.of("deleted"), String.class, paramMap);

        assertThat(result).isEqualTo("status not in  ( :status ) ");
    }

    // Test with integer type
    @Test
    void in_withIntegerType_shouldParseCorrectly() throws DbException {
        InOperatorHandler handler = new InOperatorHandler();
        DbColumn intColumn = createColumn("priority", "integer", Integer.class);

        String result = handler.handle(dialect, intColumn, dbWhere, List.of("1", "2", "3"), Integer.class, paramMap);

        assertThat(result).contains("in");
        assertThat(paramMap.get("t_priority")).isEqualTo(List.of(1, 2, 3));
    }
}
