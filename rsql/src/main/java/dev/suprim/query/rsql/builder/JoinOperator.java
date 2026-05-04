package dev.suprim.query.rsql.builder;

/**
 * RSQL operators supported in JOIN ON conditions.
 * Symbols must match keys in {@link dev.suprim.query.rsql.operators.OperatorMap}.
 */
public enum JoinOperator {

    EQ("=="),
    GT("=gt="),
    GTE("=gte="),
    LT("=lt="),
    LTE("=lte="),
    IS_NULL("=isnull="),
    IS_NOT_NULL("=notnull=");

    private final String symbol;

    JoinOperator(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }
}
