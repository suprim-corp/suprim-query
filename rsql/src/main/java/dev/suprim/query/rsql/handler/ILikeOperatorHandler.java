package dev.suprim.query.rsql.handler;

import dev.suprim.query.dialect.Dialect;
import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.DbWhere;

import java.util.Map;

public class ILikeOperatorHandler implements OperatorHandler {
    private static final String OPERATOR = " ilike ";

    @Override
    public String handle(
            Dialect dialect,
            DbColumn column,
            DbWhere dbWhere,
            String value,
            Class<?> type,
            Map<String, Object> paramMap
    ) {
        // value is always string for ilike operator
        String vo = "%" + value + "%";

        if (dialect.supportAlias()) {
            String key = reviewAndSetParam(
                    dialect.getAliasedNameParam(column, dbWhere.isDelete()), vo, paramMap);
            return dialect.getAliasedName(column, dbWhere.isDelete()) +
                   OPERATOR + PREFIX + key;
        } else {
            String key = reviewAndSetParam(column.name(), vo, paramMap);
            return column.name() + OPERATOR + PREFIX + key;
        }
    }
}
