package dev.suprim.query.model;

/**
 * SQL JOIN types supported by JoinDetail and JoinProcessor.
 */
public enum JoinType {

    INNER,
    LEFT,
    RIGHT,
    FULL;

    public String symbol() {
        return name();
    }
}
