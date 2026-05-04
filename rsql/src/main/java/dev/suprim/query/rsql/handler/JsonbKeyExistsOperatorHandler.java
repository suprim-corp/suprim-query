package dev.suprim.query.rsql.handler;

import dev.suprim.query.dialect.Dialect;
import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.DbWhere;

import java.util.Map;

public class JsonbKeyExistsOperatorHandler implements OperatorHandler {
    private static final String OPERATOR = " is not null ";

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
            // getAliasedName() already includes jsonParts — do not append again
            return dialect.getAliasedName(column, dbWhere.isDelete()) + OPERATOR;
        } else {
            String jsonPart = column.jsonParts() != null ? column.jsonParts() : "";
            return column.name() + jsonPart + OPERATOR;
        }
    }
}
