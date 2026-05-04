package dev.suprim.query.model;

public record MetaDataTable(
        String tableName,
        String catalog,
        String schema,
        String tableType,
        String tableAlias
) {}
