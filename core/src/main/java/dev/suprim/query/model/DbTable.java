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

        if (columnName.contains("#>>")) {
            int index = columnName.indexOf("#>>");
            colName = columnName.substring(0, index);
            String path = columnName.substring(index + 3);
            jsonParts = "#>>'{" + path.replace(".", ",") + "}'";
        } else if (columnName.contains("#>")) {
            int index = columnName.indexOf("#>");
            colName = columnName.substring(0, index);
            String path = columnName.substring(index + 2);
            jsonParts = "#>'{" + path.replace(".", ",") + "}'";
        } else if (columnName.contains("->>") || columnName.contains("->")) {
            jsonParts = parseChainedArrows(columnName);
            int firstOp = columnName.indexOf("->");
            colName = columnName.substring(0, firstOp);
        } else if (columnName.contains("**")) {
            int index = columnName.indexOf("**");
            colName = columnName.substring(0, index);
            String remainder = columnName.substring(index + 2);
            String[] segments = remainder.split("\\*\\*");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < segments.length; i++) {
                if (i == segments.length - 1) {
                    sb.append("->>").append("'").append(segments[i]).append("'");
                } else {
                    sb.append("->").append("'").append(segments[i]).append("'");
                }
            }
            jsonParts = sb.toString();
        } else if (columnName.contains("*")) {
            int index = columnName.indexOf("*");
            colName = columnName.substring(0, index);
            String remainder = columnName.substring(index + 1);
            String[] segments = remainder.split("\\*");
            StringBuilder sb = new StringBuilder();
            for (String segment : segments) {
                sb.append("->").append("'").append(segment).append("'");
            }
            jsonParts = sb.toString();
        }

        String alias = aliasParts.length == 2 ? aliasParts[1] : "";
        return new DbAlias(colName.trim(), alias.trim(), jsonParts);
    }

    private String parseChainedArrows(String expression) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        // Skip column name — find first ->
        int firstOp = expression.indexOf("->");
        if (firstOp == -1) {
            return "";
        }
        i = firstOp;

        while (i < expression.length()) {
            if (i + 2 < expression.length() && expression.startsWith("->>", i)) {
                i += 3;
                String key = extractKey(expression, i);
                i += key.length();
                if (isQuoted(key)) {
                    result.append("->>").append(key);
                } else {
                    result.append("->>").append("'").append(key).append("'");
                }
            } else if (expression.startsWith("->", i)) {
                i += 2;
                String key = extractKey(expression, i);
                i += key.length();
                if (isQuoted(key)) {
                    result.append("->").append(key);
                } else {
                    result.append("->").append("'").append(key).append("'");
                }
            } else {
                break;
            }
        }
        return result.toString();
    }

    private String extractKey(String expression, int start) {
        if (start >= expression.length()) {
            return "";
        }
        // Quoted key: 'something'
        if (expression.charAt(start) == '\'') {
            int end = expression.indexOf('\'', start + 1);
            if (end == -1) {
                return expression.substring(start);
            }
            return expression.substring(start, end + 1);
        }
        // Unquoted key: read until next -> or end
        int nextOp = expression.indexOf("->", start);
        if (nextOp == -1) {
            return expression.substring(start);
        }
        return expression.substring(start, nextOp);
    }

    private boolean isQuoted(String key) {
        return key.length() >= 2 && key.charAt(0) == '\'' && key.charAt(key.length() - 1) == '\'';
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
