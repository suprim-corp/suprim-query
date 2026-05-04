package dev.suprim.query.rsql.handler;

import dev.suprim.query.dialect.Dialect;
import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.DbWhere;

import java.util.Map;

public class IsNullOperatorHandler implements OperatorHandler {
    private static final String OPERATOR = " is null ";

    @Override
    public String handle(
            Dialect dialect,
            DbColumn column,
            DbWhere dbWhere,
            String value,
            Class<?> type,
            Map<String, Object> paramMap
    ) {
        if (dialect.supportAlias()) {
            return dialect.getAliasedName(column, dbWhere.isDelete()) + OPERATOR;
        } else {
            return column.name() + OPERATOR;
        }
    }
}
