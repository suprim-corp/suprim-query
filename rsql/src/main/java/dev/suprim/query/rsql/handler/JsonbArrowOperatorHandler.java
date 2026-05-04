package dev.suprim.query.rsql.handler;

import dev.suprim.query.dialect.Dialect;
import dev.suprim.query.exception.DbErrorCode;
import dev.suprim.query.exception.DbException;
import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.DbWhere;

import java.util.Map;

/**
 * Handler for JSONB arrow operator to extract and compare specific keys.
 * Uses PostgreSQL's ->> operator to extract text values from JSONB.
 *
 * Example: meta_data=jba=type::aws
 * Translates to: meta_data->>'type' = 'aws'
 */
public class JsonbArrowOperatorHandler implements OperatorHandler {
    private static final String OPERATOR = " = ";
    private static final String DELIMITER = "::";

    @Override
    public String handle(
            Dialect dialect,
            DbColumn column,
            DbWhere dbWhere,
            String value,
            Class<?> type,
            Map<String, Object> paramMap
    ) throws DbException {
        // Parse the value as "key::value" where :: is the delimiter
        int delimiterIndex = value.indexOf(DELIMITER);
        if (delimiterIndex <= 0) {
            throw new DbException(
                    DbErrorCode.INVALID_REQUEST,
                    "JSONB arrow operator requires format: key::value. Got: '" + value + "'"
            );
        }

        String jsonKey = value.substring(0, delimiterIndex).trim();
        String jsonValue = value.substring(delimiterIndex + DELIMITER.length()).trim();

        if (!jsonKey.matches("^[a-zA-Z0-9_]+$")) {
            throw new DbException(DbErrorCode.INVALID_REQUEST,
                    "JSONB key must be alphanumeric (a-z, A-Z, 0-9, _), got: '" + jsonKey + "'");
        }

        Object vo = dialect.processValue(jsonValue, String.class, null, "text");

        if (dialect.supportAlias()) {
            String key = reviewAndSetParam(
                    dialect.getAliasedNameParam(column, dbWhere.isDelete()) + "_" + jsonKey,
                    vo,
                    paramMap
            );
            return dialect.getAliasedName(column, dbWhere.isDelete()) +
                   "->>" + "'" + jsonKey + "'" + OPERATOR + PREFIX + key;
        } else {
            String key = reviewAndSetParam(
                    column.name() + "_" + jsonKey,
                    vo,
                    paramMap
            );
            return column.name() + "->>" + "'" + jsonKey + "'" + OPERATOR + PREFIX + key;
        }
    }
}
