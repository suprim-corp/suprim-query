package dev.suprim.query.rsql.handler;

import dev.suprim.query.exception.DbException;
import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.DbWhere;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for string/pattern matching operator handlers: like, ilike, startWith, endWith, notlike
 */
class StringOperatorHandlersTest extends OperatorHandlerTestBase {

    private TestDialect dialect;
    private TestDialect dialectNoAlias;
    private DbColumn column;
    private DbWhere dbWhere;
    private Map<String, Object> paramMap;

    @BeforeEach
    void setUp() {
        dialect = createDialect();
        dialectNoAlias = createDialectWithoutAlias();
        column = createColumn("name", "varchar", String.class);
        dbWhere = createDbWhere();
        paramMap = createParamMap();
    }

    // LikeOperatorHandler tests
    @Test
    void like_withAlias_shouldReturnCorrectSql() throws DbException {
        LikeOperatorHandler handler = new LikeOperatorHandler();

        String result = handler.handle(dialect, column, dbWhere, "john", String.class, paramMap);

        assertThat(result).isEqualTo("t.name like :t_name");
        assertThat(paramMap).containsEntry("t_name", "%john%");
    }

    @Test
    void like_withoutAlias_shouldReturnCorrectSql() throws DbException {
        LikeOperatorHandler handler = new LikeOperatorHandler();

        String result = handler.handle(dialectNoAlias, column, dbWhere, "john", String.class, paramMap);

        assertThat(result).isEqualTo("name like :name");
        assertThat(paramMap).containsEntry("name", "%john%");
    }

    // ILikeOperatorHandler tests
    @Test
    void ilike_withAlias_shouldReturnCorrectSql() throws DbException {
        ILikeOperatorHandler handler = new ILikeOperatorHandler();

        String result = handler.handle(dialect, column, dbWhere, "john", String.class, paramMap);

        assertThat(result).isEqualTo("t.name ilike :t_name");
        assertThat(paramMap).containsEntry("t_name", "%john%");
    }

    @Test
    void ilike_withoutAlias_shouldReturnCorrectSql() throws DbException {
        ILikeOperatorHandler handler = new ILikeOperatorHandler();

        String result = handler.handle(dialectNoAlias, column, dbWhere, "john", String.class, paramMap);

        assertThat(result).isEqualTo("name ilike :name");
        assertThat(paramMap).containsEntry("name", "%john%");
    }

    // StartWithOperatorHandler tests
    @Test
    void startWith_withAlias_shouldReturnCorrectSql() throws DbException {
        StartWithOperatorHandler handler = new StartWithOperatorHandler();

        String result = handler.handle(dialect, column, dbWhere, "john", String.class, paramMap);

        assertThat(result).isEqualTo("t.name like :t_name");
        assertThat(paramMap).containsEntry("t_name", "john%");
    }

    @Test
    void startWith_withoutAlias_shouldReturnCorrectSql() throws DbException {
        StartWithOperatorHandler handler = new StartWithOperatorHandler();

        String result = handler.handle(dialectNoAlias, column, dbWhere, "john", String.class, paramMap);

        assertThat(result).isEqualTo("name like :name");
        assertThat(paramMap).containsEntry("name", "john%");
    }

    // EndWithOperatorHandler tests
    @Test
    void endWith_withAlias_shouldReturnCorrectSql() throws DbException {
        EndWithOperatorHandler handler = new EndWithOperatorHandler();

        String result = handler.handle(dialect, column, dbWhere, "son", String.class, paramMap);

        assertThat(result).isEqualTo("t.name like :t_name");
        assertThat(paramMap).containsEntry("t_name", "%son");
    }

    @Test
    void endWith_withoutAlias_shouldReturnCorrectSql() throws DbException {
        EndWithOperatorHandler handler = new EndWithOperatorHandler();

        String result = handler.handle(dialectNoAlias, column, dbWhere, "son", String.class, paramMap);

        assertThat(result).isEqualTo("name like :name");
        assertThat(paramMap).containsEntry("name", "%son");
    }

    // NotLikeOperatorHandler tests
    @Test
    void notLike_withAlias_shouldReturnCorrectSql() throws DbException {
        NotLikeOperatorHandler handler = new NotLikeOperatorHandler();

        String result = handler.handle(dialect, column, dbWhere, "test", String.class, paramMap);

        assertThat(result).isEqualTo("t.name not like :t_name");
        assertThat(paramMap).containsEntry("t_name", "%test%");
    }

    @Test
    void notLike_withoutAlias_shouldReturnCorrectSql() throws DbException {
        NotLikeOperatorHandler handler = new NotLikeOperatorHandler();

        String result = handler.handle(dialectNoAlias, column, dbWhere, "test", String.class, paramMap);

        assertThat(result).isEqualTo("name not like :name");
        assertThat(paramMap).containsEntry("name", "%test%");
    }
}
