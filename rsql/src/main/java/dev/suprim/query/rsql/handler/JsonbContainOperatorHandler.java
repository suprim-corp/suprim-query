package dev.suprim.query.rsql.handler;

import dev.suprim.query.dialect.Dialect;
import dev.suprim.query.exception.DbException;
import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.DbWhere;

import java.util.Map;

public class JsonbContainOperatorHandler implements OperatorHandler {
    private static final String OPERATOR = " @> ";

    @Override
    public String handle(
            Dialect dialect,
            DbColumn column,
            DbWhere dbWhere,
            String value,
            Class<?> type,
            Map<String, Object> paramMap
    ) throws DbException {
        Object vo = dialect.processValue(
                value,
                type,
                null,
                column.columnDataTypeName()
        );

        if (dialect.supportAlias()) {
            String key = reviewAndSetParam(
                    dialect.getAliasedNameParam(column, dbWhere.isDelete()), vo, paramMap);
            return dialect.getAliasedName(column, dbWhere.isDelete()) +
                   OPERATOR + PREFIX + key + "::jsonb";
        } else {
            String key = reviewAndSetParam(column.name(), vo, paramMap);
            return column.name() + OPERATOR + PREFIX + key + "::jsonb";
        }
    }
}
