package dev.suprim.query.rsql.resolver;

import dev.suprim.query.exception.DbErrorCode;
import dev.suprim.query.exception.DbException;
import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.DbTable;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Resolves column references across multiple tables in join operations.
 * Handles column selectors like "tops.color" and "bottoms.color" by finding
 * the appropriate table based on the table prefix.
 */
@Slf4j
public class CrossTableColumnResolver {

    /**
     * Resolves a column reference that may include a table prefix.
     *
     * @param columnSelector The column selector (e.g., "tops.color", "color", "bottoms.size")
     * @param allTables      List of all tables involved in the query (root + joined tables)
     * @param fallbackTable  The fallback table to use if no table prefix is found
     * @return The resolved DbColumn
     * @throws DbException if the column cannot be resolved
     */
    public static DbColumn resolveColumn(
            String columnSelector,
            List<DbTable> allTables,
            DbTable fallbackTable
    ) throws DbException {
        log.debug(
                "Resolving column selector: {} across {} tables",
                columnSelector,
                nonNull(allTables) ? allTables.size() : 0
        );

        if (isNull(columnSelector) || columnSelector.isBlank()) {
            throw new DbException(
                    DbErrorCode.INVALID_REQUEST,
                    "Column selector cannot be blank"
            );
        }

        // Check if the column selector contains a table prefix (e.g., "tops.color")
        if (columnSelector.contains(".")) {
            String[] parts = columnSelector.split("\\.", 2);
            String tablePrefix = parts[0];
            String columnName = parts[1];

            log.debug(
                    "Column selector has table prefix: {} for column: {}",
                    tablePrefix,
                    columnName
            );

            // Find the table that matches the prefix
            DbTable targetTable = findTableByPrefix(tablePrefix, allTables);
            if (nonNull(targetTable)) {
                log.debug(
                        "Found target table: {} for prefix: {}",
                        targetTable.name(),
                        tablePrefix
                );
                return targetTable.buildColumn(columnName);
            } else {
                log.warn(
                        "No table found for prefix: {}, falling back to default table",
                        tablePrefix
                );
            }
        }

        // If no table prefix found or table not found, use the fallback table
        log.debug(
                "Using fallback table: {} for column: {}",
                nonNull(fallbackTable) ? fallbackTable.name() : "null",
                columnSelector
        );

        if (isNull(fallbackTable)) {
            throw new DbException(
                    DbErrorCode.INVALID_REQUEST,
                    "No fallback table available for column: " + columnSelector
            );
        }

        return fallbackTable.buildColumn(columnSelector);
    }

    /**
     * Finds a table that matches the given prefix.
     * The prefix can match either the table name or the table alias.
     *
     * @param prefix    The table prefix to search for
     * @param allTables List of all available tables
     * @return The matching DbTable or null if not found
     */
    private static DbTable findTableByPrefix(
            String prefix,
            List<DbTable> allTables
    ) {
        if (isNull(allTables) || allTables.isEmpty()) {
            log.debug("No tables available for prefix matching");
            return null;
        }

        for (DbTable table : allTables) {
            // Check if prefix matches table name (case-insensitive)
            if (prefix.equalsIgnoreCase(table.name())) {
                log.debug(
                        "Found table by name match: {} for prefix: {}",
                        table.name(),
                        prefix
                );
                return table;
            }

            // Check if the prefix matches table alias (case-insensitive)
            String alias = table.alias();
            if (nonNull(alias) && !alias.isBlank() &&
                prefix.equalsIgnoreCase(alias)
            ) {
                log.debug(
                        "Found table by alias match: {} (alias: {}) for prefix: {}",
                        table.name(),
                        table.alias(),
                        prefix
                );

                return table;
            }
        }

        log.debug("No table found for prefix: {}", prefix);
        return null;
    }
}
