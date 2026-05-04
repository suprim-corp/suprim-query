package dev.suprim.query.model;

import java.util.List;

public record DbMeta(
        String productName,
        int majorVersion,
        String driverName,
        String driverVersion,
        List<DbTable> dbTables
) {}
