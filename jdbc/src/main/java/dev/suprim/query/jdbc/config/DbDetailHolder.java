package dev.suprim.query.jdbc.config;

import dev.suprim.query.dialect.Dialect;
import dev.suprim.query.model.DbMeta;
import dev.suprim.query.model.DbTable;

import java.util.Map;

public record DbDetailHolder(
        String dbId,
        DbMeta dbMeta,
        Map<String, DbTable> dbTableMap,
        Dialect dialect
) {
}
