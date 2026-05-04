package dev.suprim.query.jdbc.operation;

import dev.suprim.query.dialect.Dialect;
import dev.suprim.query.exception.DbErrorCode;
import dev.suprim.query.exception.DbException;
import dev.suprim.query.model.ArrayTypeValueHolder;
import dev.suprim.query.model.DbTable;
import dev.suprim.query.model.dto.CountResponse;
import dev.suprim.query.model.dto.CreateBulkResponse;
import dev.suprim.query.model.dto.CreationResponse;
import dev.suprim.query.model.dto.ExistsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import static java.util.Objects.isNull;

@RequiredArgsConstructor
@Slf4j
public class JdbcOperationService implements DbOperationService {

    @Override
    public int update(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            Map<String, Object> paramMap,
            String sql
    ) {
        return namedParameterJdbcTemplate.update(sql, paramMap);
    }

    @Override
    public List<Map<String, Object>> read(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            Map<String, Object> paramMap,
            String sql,
            Dialect dialect
    ) {
        return namedParameterJdbcTemplate.query(
                sql,
                new MapSqlParameterSource(paramMap),
                new SimpleRowMapper(dialect)
        );
    }

    @Override
    public Map<String, Object> findOne(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            String sql,
            Map<String, Object> paramMap
    ) {
        return namedParameterJdbcTemplate.queryForMap(sql, paramMap);
    }

    @Override
    public ExistsResponse exists(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            Map<String, Object> paramMap,
            String sql
    ) {
        List<String> queryResult = namedParameterJdbcTemplate.query(
                sql,
                paramMap,
                (rs, rowNum) -> rs.getString(1)
        );

        return queryResult.isEmpty()
                ? new ExistsResponse(false)
                : new ExistsResponse(true);
    }

    @Override
    public CountResponse count(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            Map<String, Object> paramMap,
            String sql
    ) {
        Long itemCount = namedParameterJdbcTemplate.queryForObject(sql, paramMap, Long.class);

        return new CountResponse(isNull(itemCount) ? 0 : itemCount);
    }

    @Override
    public Object queryCustom(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            boolean single,
            String sql,
            Map<String, Object> params
    ) {
        return single ?
                namedParameterJdbcTemplate.queryForMap(sql, params) :
                namedParameterJdbcTemplate.queryForList(sql, params);
    }

    @Override
    public int delete(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            Map<String, Object> params,
            String sql
    ) {
        return namedParameterJdbcTemplate.update(sql, params);
    }

    @Override
    public CreationResponse create(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            Map<String, Object> data,
            String sql,
            DbTable dbTable
    ) throws DbException {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        MapSqlParameterSource parameterSource = new MapSqlParameterSource();

        for (String key : data.keySet()) {
            Object value = data.get(key);

            if (value instanceof ArrayTypeValueHolder val) {
                value = processArrayValue(namedParameterJdbcTemplate, val);
            }

            parameterSource.addValue(key, value);
        }

        int row = namedParameterJdbcTemplate.update(
                sql,
                parameterSource,
                keyHolder,
                dbTable.getKeyColumnNames()
        );

        log.info("*** update fired returning ***");

        return new CreationResponse(row, keyHolder.getKeys());
    }

    private Array processArrayValue(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            ArrayTypeValueHolder val
    ) throws DbException {
        DataSource ds = namedParameterJdbcTemplate.getJdbcTemplate().getDataSource();
        Connection connection = DataSourceUtils.getConnection(ds);
        try {
            return connection.createArrayOf(val.sqlType(), val.values());
        } catch (SQLException e) {
            log.error("Unable to convert Array field", e);
            throw new DbException(DbErrorCode.INVALID_REQUEST, "Unable to convert Array field");
        } finally {
            DataSourceUtils.releaseConnection(connection, ds);
        }
    }

    @Override
    public CreateBulkResponse batchUpdate(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            List<Map<String, Object>> dataList,
            String sql,
            DbTable dbTable
    ) {
        SqlParameterSource[] batch = SqlParameterSourceUtils.createBatch(dataList.toArray());

        int[] updateCounts;
        KeyHolder keyHolder = new GeneratedKeyHolder();

        updateCounts = namedParameterJdbcTemplate.batchUpdate(
                sql,
                batch,
                keyHolder,
                dbTable.getKeyColumnNames()
        );

        return new CreateBulkResponse(updateCounts, keyHolder.getKeyList());
    }

    @Override
    public CreateBulkResponse batchUpdate(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            List<Map<String, Object>> dataList,
            String sql
    ) {
        SqlParameterSource[] batch = SqlParameterSourceUtils.createBatch(dataList.toArray());

        int[] updateCounts = namedParameterJdbcTemplate.batchUpdate(sql, batch);

        return new CreateBulkResponse(updateCounts, null);
    }
}
