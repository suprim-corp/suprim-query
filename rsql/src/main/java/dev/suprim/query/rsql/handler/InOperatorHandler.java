package dev.suprim.query.rsql.handler;

import dev.suprim.query.dialect.Dialect;
import dev.suprim.query.exception.DbException;
import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.DbWhere;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public class InOperatorHandler implements OperatorHandler {
    private static final String OPERATOR = " in ";

    @Override
    public String handle(
            Dialect dialect,
            DbColumn column,
            DbWhere dbWhere,
            String value,
            Class<?> type,
            Map<String, Object> paramMap
    ) throws DbException {
        return handle(
                dialect,
                column,
                dbWhere,
                Collections.singletonList(value),
                type,
                paramMap
        );
    }

    @Override
    public String handle(
            Dialect dialect,
            DbColumn column,
            DbWhere dbWhere,
            List<String> values,
            Class<?> type,
            Map<String, Object> paramMap
    ) {
        List<Object> vo = dialect.parseListValues(
                values,
                type,
                column.columnDataTypeName()
        );

        if (dialect.supportAlias()) {
            String key = reviewAndSetParam(
                    dialect.getAliasedNameParam(column, dbWhere.isDelete()),
                    vo,
                    paramMap
            );
            return dialect.getAliasedName(column, dbWhere.isDelete()) +
                   OPERATOR + " ( " + PREFIX + key + " ) ";
        } else {
            String key = reviewAndSetParam(column.name(), vo, paramMap);
            return column.name() + OPERATOR + " ( " + PREFIX + key + " ) ";
        }
    }
}
