package dev.suprim.query.jdbc.executor.deletion;

import cz.jirutka.rsql.parser.ast.Node;
import dev.suprim.query.exception.DbErrorCode;
import dev.suprim.query.exception.DbException;
import dev.suprim.query.exception.DbRuntimeException;
import dev.suprim.query.jdbc.operation.DbOperationService;
import dev.suprim.query.jdbc.operation.JdbcManager;
import dev.suprim.query.jdbc.operation.SqlCreatorTemplate;
import dev.suprim.query.model.DbTable;
import dev.suprim.query.model.DbWhere;
import dev.suprim.query.model.context.DeleteContext;
import dev.suprim.query.rsql.parser.RSQLParserBuilder;
import dev.suprim.query.rsql.visitor.BaseRSQLVisitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;

@Slf4j
@RequiredArgsConstructor
public class JdbcDeleteService implements DeleteService {
    private final JdbcManager jdbcManager;
    private final SqlCreatorTemplate sqlCreatorTemplate;
    private final DbOperationService dbOperationService;

    @Override
    @Transactional
    public int delete(
            String dbId,
            String schemaName,
            String tableName,
            String filter
    ) throws DbException {
        if (isNull(filter) || filter.isBlank()) {
            throw new DbException(DbErrorCode.INVALID_REQUEST,
                    "DELETE without filter is not allowed. Provide an RSQL filter to scope the operation.");
        }

        DbTable dbTable = jdbcManager.getTable(dbId, schemaName, tableName);

        DeleteContext context = DeleteContext.builder()
                .dbId(dbId)
                .tableName(tableName)
                .table(dbTable)
                .build();

        context.createParamMap();

        return executeDelete(dbId, filter, dbTable, context);
    }

    @Override
    public int deleteBulk(
            String dbId,
            String schemaName,
            String tableName,
            List<String> filters
    ) throws DbException {
        if (isNull(filters) || filters.isEmpty()) {
            throw new DbException(DbErrorCode.INVALID_REQUEST,
                    "Bulk delete requires at least one filter.");
        }

        DbTable dbTable = jdbcManager.getTable(dbId, schemaName, tableName);

        Integer totalDeleted = jdbcManager.getTxnTemplate(dbId).execute(status -> {
            try {
                int total = 0;
                for (String filter : filters) {
                    total += executeSingleDelete(dbId, dbTable, tableName, filter);
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

        return isNull(totalDeleted) ? 0 : totalDeleted;
    }

    private int executeSingleDelete(
            String dbId,
            DbTable dbTable,
            String tableName,
            String filter
    ) throws DbException {
        if (isNull(filter) || filter.isBlank()) {
            throw new DbException(DbErrorCode.INVALID_REQUEST,
                    "Each bulk delete filter must be non-blank.");
        }

        DeleteContext context = DeleteContext.builder()
                .dbId(dbId)
                .tableName(tableName)
                .table(dbTable)
                .build();

        context.createParamMap();

        addWhere(filter, dbTable, context);
        String sql = sqlCreatorTemplate.deleteQuery(context);

        log.debug("Bulk delete SQL: {}", sql);
        log.debug("Bulk delete params: {}", context.getParamMap());

        return dbOperationService.delete(
                jdbcManager.getNamedParameterJdbcTemplate(dbId),
                context.getParamMap(),
                sql
        );
    }

    private int executeDelete(
            String dbId,
            String filter,
            DbTable table,
            DeleteContext context
    ) throws DbException {
        addWhere(filter, table, context);
        String sql = sqlCreatorTemplate.deleteQuery(context);

        log.debug("Delete SQL: {}", sql);
        log.debug("Delete Params: {}", context.getParamMap());

        Integer result = jdbcManager.getTxnTemplate(dbId).execute(status -> {
            try {
                return dbOperationService.delete(
                        jdbcManager.getNamedParameterJdbcTemplate(dbId),
                        context.getParamMap(),
                        sql
                );
            } catch (Exception e) {
                log.error("Error in delete operation: ", e);
                status.setRollbackOnly();
                throw new RuntimeException(e.getMessage());
            }
        });

        return isNull(result) ? 0 : result;
    }

    private void addWhere(String filter, DbTable table, DeleteContext context) throws DbException {
        Map<String, Object> paramMap = context.getParamMap();

        DbWhere dbWhere = new DbWhere(
                context.getTableName(),
                table,
                null,
                paramMap,
                "delete",
                null // No joins in delete operations
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
