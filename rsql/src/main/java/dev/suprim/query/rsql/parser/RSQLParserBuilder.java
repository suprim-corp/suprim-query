package dev.suprim.query.rsql.parser;

import cz.jirutka.rsql.parser.RSQLParser;
import dev.suprim.query.rsql.operators.CustomRSQLOperators;

public class RSQLParserBuilder {
    public static RSQLParser newRSQLParser() {
        return new RSQLParser(CustomRSQLOperators.customOperators());
    }
}
