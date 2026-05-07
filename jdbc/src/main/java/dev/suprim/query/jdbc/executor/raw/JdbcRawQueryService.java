package dev.suprim.query.jdbc.executor.raw;

import dev.suprim.query.exception.DbErrorCode;
import dev.suprim.query.exception.DbException;
import dev.suprim.query.jdbc.operation.JdbcManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.isNull;

@Slf4j
@RequiredArgsConstructor
public class JdbcRawQueryService implements RawQueryService {

    private final JdbcManager jdbcManager;

    @Override
    public Optional<Map<String, Object>> queryOne(String dbId, String sql, Map<String, Object> params) throws DbException {
        Objects.requireNonNull(params, "params must not be null; use Map.of() for no params");
        NamedParameterJdbcTemplate jdbc = getJdbcTemplate(dbId);

        log.debug("Raw queryOne — dbId: {}, sql: {}", dbId, sql);
        try {
            return Optional.of(jdbc.queryForMap(sql, params));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (DataAccessException e) {
            log.error("Raw queryOne failed — dbId: {}, error: {}", dbId, e.getMessage(), e);
            throw new DbException(DbErrorCode.SERVER_ERROR, "Raw query failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Map<String, Object>> queryList(String dbId, String sql, Map<String, Object> params) throws DbException {
        Objects.requireNonNull(params, "params must not be null; use Map.of() for no params");
        NamedParameterJdbcTemplate jdbc = getJdbcTemplate(dbId);

        log.debug("Raw queryList — dbId: {}, sql: {}", dbId, sql);
        try {
            return jdbc.queryForList(sql, params);
        } catch (DataAccessException e) {
            log.error("Raw queryList failed — dbId: {}, error: {}", dbId, e.getMessage(), e);
            throw new DbException(DbErrorCode.SERVER_ERROR, "Raw query failed: " + e.getMessage(), e);
        }
    }

    @Override
    public int execute(String dbId, String sql, Map<String, Object> params) throws DbException {
        Objects.requireNonNull(params, "params must not be null; use Map.of() for no params");
        TransactionTemplate txn = getTxnTemplate(dbId);
        NamedParameterJdbcTemplate jdbc = getJdbcTemplate(dbId);

        log.debug("Raw execute — dbId: {}, sql: {}", dbId, sql);
        try {
            Integer result = txn.execute(status -> jdbc.update(sql, params));
            return isNull(result) ? 0 : result;
        } catch (DataAccessException e) {
            log.error("Raw execute failed — dbId: {}, error: {}", dbId, e.getMessage(), e);
            throw new DbException(DbErrorCode.SERVER_ERROR, "Raw execute failed: " + e.getMessage(), e);
        }
    }

    private NamedParameterJdbcTemplate getJdbcTemplate(String dbId) throws DbException {
        NamedParameterJdbcTemplate jdbc = jdbcManager.getNamedParameterJdbcTemplate(dbId);
        if (isNull(jdbc)) {
            throw new DbException(DbErrorCode.NOT_FOUND, "DB not found: " + dbId);
        }
        return jdbc;
    }

    private TransactionTemplate getTxnTemplate(String dbId) throws DbException {
        TransactionTemplate txn = jdbcManager.getTxnTemplate(dbId);
        if (isNull(txn)) {
            throw new DbException(DbErrorCode.NOT_FOUND, "Transaction template not found for DB: " + dbId);
        }
        return txn;
    }
}
