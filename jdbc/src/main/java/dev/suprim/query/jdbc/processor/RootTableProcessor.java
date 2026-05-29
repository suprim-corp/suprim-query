package dev.suprim.query.jdbc.processor;

import dev.suprim.query.exception.DbErrorCode;
import dev.suprim.query.exception.DbException;
import dev.suprim.query.jdbc.config.DatabaseProperties;
import dev.suprim.query.jdbc.operation.JdbcManager;
import dev.suprim.query.model.DbTable;
import dev.suprim.query.model.context.ReadContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;

import static java.util.Objects.isNull;

@Slf4j
@Order(1)
@RequiredArgsConstructor
public class RootTableProcessor implements ReadProcessor {
    private final JdbcManager jdbcManager;
    private final DatabaseProperties databaseProperties;

    @Override
    public void process(ReadContext readContext) throws DbException {
        log.debug("Processing root table");

        // limit = -1 is a sentinel meaning "use defaultFetchLimit" (handled by SqlCreatorTemplate).
        // Reject values < -1 which have no defined meaning and would cause SQL errors.
        if (readContext.getLimit() < -1) {
            throw new DbException(DbErrorCode.INVALID_REQUEST,
                    "limit must be >= -1 (use -1 to apply defaultFetchLimit), got: " + readContext.getLimit());
        }
        if (readContext.getOffset() < 0) {
            throw new DbException(DbErrorCode.INVALID_REQUEST,
                    "offset must be >= 0, got: " + readContext.getOffset());
        }

        String dbId = readContext.getDbId();
        if (isNull(dbId) || dbId.isBlank()) {
            dbId = databaseProperties.getDefaultDatabaseId();
            readContext.setDbId(dbId);
            log.debug("dbId was null, resolved to defaultDatabaseId: {}", dbId);
        }

        DbTable table = jdbcManager.getTable(
                dbId,
                readContext.getSchemaName(),
                readContext.getTableName()
        );

        readContext.setRoot(table);
    }
}
