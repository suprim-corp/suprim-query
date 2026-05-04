package dev.suprim.query.rsql.operators;

import dev.suprim.query.exception.DbErrorCode;
import dev.suprim.query.exception.DbException;

import java.util.Comparator;
import java.util.Map;

import static java.util.Locale.ROOT;

public class OperatorMap {
    private static final Map<String, String> opMap = Map.of(
            "==", "=",
            "=gt=", ">",
            "=gte=", ">=",
            "=lt=", "<",
            "=lte=", "<=",
            "=notnull=", "IS NOT NULL",
            "=isnull=", "IS NULL");

    public static String getSQLOperator(String rSQLOperator) {
        return opMap.get(rSQLOperator);
    }

    public static String getRSQLOperator(String expression) throws DbException {
        String exprLower = expression.toLowerCase(ROOT);
        // Sort by length descending so longer operators (=gte=) match before
        // shorter prefixes (=gt=) — prevents false matches on substrings.
        return opMap.keySet()
                .stream()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .filter(operator -> exprLower.contains(operator.toLowerCase(ROOT)))
                .findFirst()
                .orElseThrow(() -> new DbException(DbErrorCode.INVALID_REQUEST, "Operator not supported"));
    }
}
