package dev.suprim.query.jdbc.executor.deletion;

import cz.jirutka.rsql.parser.ast.Node;
import dev.suprim.query.exception.DbErrorCode;
import dev.suprim.query.exception.DbException;
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

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

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
        if (nonNull(filter) && !filter.isBlank()) {
            Map<String, Object> paramMap = context.getParamMap();
            if (isNull(paramMap)) {
                paramMap = new HashMap<>();
                context.setParamMap(paramMap);
            }

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
}
