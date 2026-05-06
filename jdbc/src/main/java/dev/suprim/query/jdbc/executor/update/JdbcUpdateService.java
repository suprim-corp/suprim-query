package dev.suprim.query.jdbc.executor.update;

import cz.jirutka.rsql.parser.ast.Node;
import dev.suprim.query.exception.DbErrorCode;
import dev.suprim.query.exception.DbException;
import dev.suprim.query.exception.DbRuntimeException;
import dev.suprim.query.jdbc.operation.DbOperationService;
import dev.suprim.query.jdbc.operation.JdbcManager;
import dev.suprim.query.jdbc.operation.SqlCreatorTemplate;
import dev.suprim.query.model.DbTable;
import dev.suprim.query.model.DbWhere;
import dev.suprim.query.model.context.UpdateContext;
import dev.suprim.query.model.dto.BulkUpdate;
import dev.suprim.query.rsql.parser.RSQLParserBuilder;
import dev.suprim.query.rsql.visitor.BaseRSQLVisitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;

@Slf4j
@RequiredArgsConstructor
public class JdbcUpdateService implements UpdateService {
    private final JdbcManager jdbcManager;
    private final SqlCreatorTemplate sqlCreatorTemplate;
    private final DbOperationService dbOperationService;

    @Override
    public int patch(
            String dbId,
            String schemaName,
            String tableName,
            Map<String, Object> data,
            String filter
    ) throws DbException {
        if (isNull(filter) || filter.isBlank()) {
            throw new DbException(DbErrorCode.INVALID_REQUEST,
                    "UPDATE without filter is not allowed. Provide an RSQL filter to scope the operation.");
        }

        DbTable dbTable = jdbcManager.getTable(dbId, schemaName, tableName);

        List<String> updatableColumns = data.keySet().stream().toList();

        jdbcManager.getDialect(dbId).processTypes(dbTable, updatableColumns, data);

        UpdateContext context = UpdateContext.builder()
                .dbId(dbId)
                .tableName(tableName)
                .table(dbTable)
                .updatableColumns(updatableColumns)
                .build();

        context.createParamMap(data);

        return executeUpdate(dbId, filter, dbTable, context);
    }

    @Override
    public int patchBulk(
            String dbId,
            String schemaName,
            String tableName,
            List<BulkUpdate> updates
    ) throws DbException {
        if (isNull(updates) || updates.isEmpty()) {
            throw new DbException(DbErrorCode.INVALID_REQUEST,
                    "Bulk update requires at least one operation.");
        }

        DbTable dbTable = jdbcManager.getTable(dbId, schemaName, tableName);

        Integer totalAffected = jdbcManager.getTxnTemplate(dbId).execute(status -> {
            try {
                int total = 0;
                for (BulkUpdate update : updates) {
                    total += executeSingleUpdate(dbId, dbTable, tableName, update);
                }
                return total;
            } catch (DbException e) {
                status.setRollbackOnly();
                throw new DbRuntimeException(e);
            } catch (Exception e) {
                status.setRollbackOnly();
                throw new RuntimeException(e.getMessage(), e);
            }
        });

        return isNull(totalAffected) ? 0 : totalAffected;
    }

    private int executeSingleUpdate(
            String dbId,
            DbTable dbTable,
            String tableName,
            BulkUpdate update
    ) throws DbException {
        String filter = update.filter();
        Map<String, Object> data = update.data();

        if (isNull(filter) || filter.isBlank()) {
            throw new DbException(DbErrorCode.INVALID_REQUEST,
                    "Each bulk update entry must have a non-blank filter.");
        }

        if (isNull(data) || data.isEmpty()) {
            throw new DbException(DbErrorCode.INVALID_REQUEST,
                    "Each bulk update entry must have non-empty data.");
        }

        List<String> updatableColumns = data.keySet().stream().toList();

        jdbcManager.getDialect(dbId).processTypes(dbTable, updatableColumns, data);

        UpdateContext context = UpdateContext.builder()
                .dbId(dbId)
                .tableName(tableName)
                .table(dbTable)
                .updatableColumns(updatableColumns)
                .build();

        context.createParamMap(data);

        addWhere(filter, dbTable, context);
        String sql = sqlCreatorTemplate.updateQuery(context);

        log.debug("Bulk update SQL: {}", sql);
        log.debug("Bulk update params: {}", context.getParamMap());

        return dbOperationService.update(
                jdbcManager.getNamedParameterJdbcTemplate(dbId),
                context.getParamMap(),
                sql
        );
    }

    private int executeUpdate(
            String dbId,
            String filter,
            DbTable table,
            UpdateContext context
    ) throws DbException {
        addWhere(filter, table, context);
        String sql = sqlCreatorTemplate.updateQuery(context);

        log.debug("{}", sql);
        log.debug("{}", context.getParamMap());

        Integer i = jdbcManager.getTxnTemplate(dbId).execute(status -> {
            try {
                return dbOperationService.update(
                        jdbcManager.getNamedParameterJdbcTemplate(dbId),
                        context.getParamMap(),
                        sql
                );
            } catch (Exception e) {
                log.error("Error in update operation: ", e);
                status.setRollbackOnly();
                throw new RuntimeException(e.getMessage());
            }
        });

        return isNull(i) ? 0 : i;
    }

    private void addWhere(
            String filter,
            DbTable table,
            UpdateContext context
    ) throws DbException {
        DbWhere dbWhere = new DbWhere(
                context.getTableName(),
                table,
                null,
                context.getParamMap(),
                "update",
                null // No joins in update operations
        );

        Node rootNode = RSQLParserBuilder.newRSQLParser().parse(filter);

        String where = rootNode.accept(
                new BaseRSQLVisitor(
                        dbWhere,
                        jdbcManager.getDialect(context.getDbId())
                )
        );
        context.setWhere(where);
    }
}
