package dev.suprim.query.model;

import dev.suprim.query.exception.DbErrorCode;
import dev.suprim.query.exception.DbException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static java.util.Objects.isNull;

@Slf4j
public record DbTable(
        String schema,
        String name,
        String fullName,
        String alias,
        List<DbColumn> dbColumns,
        String type,
        String coverChar
) {
    public String render() {
        return fullName + " " + alias;
    }

    public DbTable copyWithAlias(String tableAlias) {
        List<DbColumn> columns = dbColumns.stream()
                .map(col -> col.copyWithTableAlias(tableAlias))
                .toList();
        return new DbTable(schema, name, fullName, tableAlias, columns, type, coverChar);
    }

    public DbColumn buildColumn(String columnName) throws DbException {
        if (isNull(columnName) || columnName.isBlank()) {
            throw new DbException(DbErrorCode.INVALID_REQUEST,
                    "Column name must not be null or blank");
        }
        DbAlias dbAlias = getAlias(columnName);
        return getDbColumn(dbAlias);
    }

    private DbColumn getDbColumn(DbAlias dbAlias) throws DbException {
        return this.dbColumns.stream()
                .filter(col -> dbAlias.name().equalsIgnoreCase(col.name()))
                .map(col -> col.copyWithAlias(dbAlias))
                .findFirst()
                .orElseThrow(() -> new DbException(
                        DbErrorCode.INVALID_REQUEST,
                        "Column not found: %s.%s".formatted(name, dbAlias.name())
                ));
    }

    private DbAlias getAlias(String name) {
        String[] aliasParts = name.split(":");
        String columnName = aliasParts[0];
        String colName = columnName;
        String jsonParts = "";

        String[][] patterns = {
                {"->>", "->>"},
                {"->", "->"},
                {"#>>", "#>>", "."},
                {"#>", "#>", "."},
                {"**", "->>", "'"},
                {"*", "->", "'"}
        };

        for (String[] patternInfo : patterns) {
            String pattern = patternInfo[0];
            if (columnName.contains(pattern)) {
                int index = columnName.indexOf(pattern);
                colName = columnName.substring(0, index);
                String rawPart = columnName.substring(index + pattern.length());
                String prefix = patternInfo[1];
                String wrapper = patternInfo.length == 3 ? patternInfo[2] : "";
                String finalValue = rawPart;

                if (".".equals(wrapper)) {
                    finalValue = rawPart.replace(".", ",");
                    jsonParts = prefix + finalValue;
                } else if (!wrapper.isEmpty()) {
                    jsonParts = prefix + wrapper + finalValue + wrapper;
                } else {
                    jsonParts = prefix + finalValue;
                }
                break;
            }
        }

        String alias = aliasParts.length == 2 ? aliasParts[1] : "";
        return new DbAlias(colName.trim(), alias.trim(), jsonParts);
    }

    public List<DbColumn> buildColumns() {
        return dbColumns;
    }

    public List<DbColumn> buildPkColumns() {
        return dbColumns.stream()
                .filter(DbColumn::pk)
                .toList();
    }

    public String[] getKeyColumnNames() {
        return buildPkColumns().stream()
                .map(DbColumn::name)
                .toList()
                .toArray(String[]::new);
    }

    public String getColumnDataTypeName(String columnName) throws DbException {
        return lookupColumn(columnName).columnDataTypeName();
    }

    private DbColumn lookupColumn(String columnName) throws DbException {
        return dbColumns.stream()
                .filter(col -> columnName.equalsIgnoreCase(col.name()))
                .findFirst()
                .orElseThrow(() -> new DbException(
                        DbErrorCode.INVALID_REQUEST,
                        "Column not found: %s.%s".formatted(name, columnName)
                ));
    }
}
