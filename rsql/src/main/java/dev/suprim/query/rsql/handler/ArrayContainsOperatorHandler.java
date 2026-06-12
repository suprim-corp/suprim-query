package dev.suprim.query.rsql.handler;

import dev.suprim.query.dialect.Dialect;
import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.DbWhere;

import java.util.Map;

/**
 * Handles =arrayContains= / =ac=. Generates :param = ANY(column) for PostgreSQL TEXT[] columns.
 */
public class ArrayContainsOperatorHandler implements OperatorHandler {

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
			String key = reviewAndSetParam(
					dialect.getAliasedNameParam(column, dbWhere.isDelete()),
					value,
					paramMap
			);
			String columnRef = dialect.getAliasedName(column, dbWhere.isDelete());
			return PREFIX + key + " = ANY(" + columnRef + ")";
		} else {
			String key = reviewAndSetParam(column.name(), value, paramMap);
			return PREFIX + key + " = ANY(" + column.name() + ")";
		}
	}
}
