package dev.suprim.query.rsql.handler;

import cz.jirutka.rsql.parser.ast.RSQLOperators;
import dev.suprim.query.rsql.operators.CustomRSQLOperators;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RSQLOperatorHandlers {
    // Immutable after static init — prevents accidental mutation and clarifies
    // that this registry is not designed for runtime extension.
    private static final Map<String, OperatorHandler> OPERATOR_HANDLER_MAP;

    static {
        Map<String, OperatorHandler> map = new HashMap<>();
        map.put(RSQLOperators.EQUAL.getSymbol(), new EqualToOperatorHandler());
        map.put(RSQLOperators.NOT_EQUAL.getSymbol(), new NotEqualToOperatorHandler());
        map.put(RSQLOperators.IN.getSymbol(), new InOperatorHandler());
        map.put(RSQLOperators.NOT_IN.getSymbol(), new NotInOperatorHandler());
        map.put(RSQLOperators.GREATER_THAN.getSymbol(), new GreaterThanOperatorHandler());
        map.put(RSQLOperators.GREATER_THAN_OR_EQUAL.getSymbol(), new GreaterThanEqualToOperatorHandler());
        map.put(RSQLOperators.LESS_THAN.getSymbol(), new LessThanOperatorHandler());
        map.put(RSQLOperators.LESS_THAN_OR_EQUAL.getSymbol(), new LessThanEqualToOperatorHandler());
        map.put(CustomRSQLOperators.LIKE.getSymbol(), new LikeOperatorHandler());
        map.put(CustomRSQLOperators.ILIKE.getSymbol(), new ILikeOperatorHandler());
        map.put(CustomRSQLOperators.START_WITH.getSymbol(), new StartWithOperatorHandler());
        map.put(CustomRSQLOperators.END_WITH.getSymbol(), new EndWithOperatorHandler());
        map.put(CustomRSQLOperators.IS_NULL.getSymbol(), new IsNullOperatorHandler());
        map.put(CustomRSQLOperators.JSONB_CONTAIN.getSymbol(), new JsonbContainOperatorHandler());
        map.put(CustomRSQLOperators.JSON_CONTAIN.getSymbol(), new JsonContainOperatorHandler());
        map.put(CustomRSQLOperators.JSONB_EQUAL.getSymbol(), new JsonbEqualToOperatorHandler());
        map.put(CustomRSQLOperators.JSONB_KEY_EXISTS.getSymbol(), new JsonbKeyExistsOperatorHandler());
        map.put(CustomRSQLOperators.JSON_CONTAINS_IN_ARRAY.getSymbol(), new JsonContainInArrayOperatorHandler());
        map.put(CustomRSQLOperators.NOT_LIKE.getSymbol(), new NotLikeOperatorHandler());
        map.put(CustomRSQLOperators.NOT_NULL.getSymbol(), new IsNotNullOperatorHandler());
        map.put(CustomRSQLOperators.JSONB_ARROW.getSymbol(), new JsonbArrowOperatorHandler());
        map.put(CustomRSQLOperators.ARRAY_CONTAINS.getSymbol(), new ArrayContainsOperatorHandler());
        OPERATOR_HANDLER_MAP = Collections.unmodifiableMap(map);
    }

    public static OperatorHandler getOperatorHandler(String symbol) {
        return OPERATOR_HANDLER_MAP.get(symbol);
    }
}
