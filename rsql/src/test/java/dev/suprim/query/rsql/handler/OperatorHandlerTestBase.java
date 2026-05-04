package dev.suprim.query.rsql.handler;

import dev.suprim.query.dialect.Dialect;
import dev.suprim.query.exception.DbException;
import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.DbTable;
import dev.suprim.query.model.DbWhere;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for operator handler tests providing common test infrastructure.
 */
public abstract class OperatorHandlerTestBase {

    protected TestDialect createDialect() {
        return new TestDialect(true);
    }

    protected TestDialect createDialectWithoutAlias() {
        return new TestDialect(false);
    }

    protected DbColumn createColumn(String name, String dataType, Class<?> type) {
        return new DbColumn(
                "users",
                name,
                "",
                "t",
                false,
                dataType,
                false,
                false,
                type,
                "\"",
                null
        );
    }

    protected DbWhere createDbWhere() {
        return createDbWhere("select");
    }

    protected DbWhere createDbWhere(String op) {
        DbTable table = new DbTable(
                "public",
                "users",
                "\"public\".\"users\"",
                "t",
                List.of(),
                "table",
                "\""
        );
        return new DbWhere("users", table, List.of(), new HashMap<>(), op, List.of(table));
    }

    protected Map<String, Object> createParamMap() {
        return new HashMap<>();
    }

    /**
     * Test dialect for operator handler testing.
     */
    protected static class TestDialect extends Dialect {
        private final boolean supportAlias;

        public TestDialect(boolean supportAlias) {
            super(new ObjectMapper(), "\"");
            this.supportAlias = supportAlias;
        }

        @Override
        public boolean supportAlias() {
            return supportAlias;
        }

        @Override
        public boolean isSupportedDb(String productName, int majorVersion) {
            return "TestDB".equals(productName);
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
