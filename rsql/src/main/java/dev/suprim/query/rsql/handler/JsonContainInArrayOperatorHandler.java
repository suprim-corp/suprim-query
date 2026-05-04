package dev.suprim.query.rsql.handler;

import dev.suprim.query.dialect.Dialect;
import dev.suprim.query.exception.DbErrorCode;
import dev.suprim.query.exception.DbException;
import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.DbWhere;

import java.util.Map;

public class JsonContainInArrayOperatorHandler implements OperatorHandler {
    private static final String OPERATOR = " ?? ";

    @Override
    public String handle(
            Dialect dialect,
            DbColumn column,
            DbWhere dbWhere,
            String value,
            Class<?> type,
            Map<String, Object> paramMap
    ) throws DbException {
        if (!value.matches("^[a-zA-Z0-9_\\-\\.]+$")) {
            throw new DbException(DbErrorCode.INVALID_REQUEST,
                    "JSONB ?? value contains invalid characters: '" + value + "'");
        }

        if (dialect.supportAlias()) {
            // getAliasedName() already includes jsonParts — do not append again
            return dialect.getAliasedName(column, dbWhere.isDelete()) + OPERATOR + "'" + value + "'";
        } else {
            String jsonPart = column.jsonParts() != null ? column.jsonParts() : "";
            return column.name() + jsonPart + OPERATOR + "'" + value + "'";
        }
    }
}
