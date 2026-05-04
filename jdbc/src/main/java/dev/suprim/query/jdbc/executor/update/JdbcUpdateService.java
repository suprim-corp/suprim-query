package dev.suprim.query.jdbc.executor.update;

import cz.jirutka.rsql.parser.ast.Node;
import dev.suprim.query.exception.DbErrorCode;
import dev.suprim.query.exception.DbException;
import dev.suprim.query.jdbc.operation.DbOperationService;
import dev.suprim.query.jdbc.operation.JdbcManager;
import dev.suprim.query.jdbc.operation.SqlCreatorTemplate;
import dev.suprim.query.model.DbTable;
import dev.suprim.query.model.DbWhere;
import dev.suprim.query.model.context.UpdateContext;
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
