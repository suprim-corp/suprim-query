package dev.suprim.query.jdbc.operation;

import dev.suprim.query.dialect.Dialect;
import dev.suprim.query.exception.DbException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Set;
import org.springframework.jdbc.core.ColumnMapRowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
@RequiredArgsConstructor
public class SimpleRowMapper extends ColumnMapRowMapper {
    private final Dialect dialect;

    @Override
    protected Object getColumnValue(ResultSet rs, int index) throws SQLException {
        String columnType = rs.getMetaData().getColumnTypeName(index);

        log.debug("columnType - {}", columnType);

        // Handle pg varchar array
        if ("_varchar".equalsIgnoreCase(columnType)) {
            try {
                return dialect.convertToStringArray(rs.getArray(index));
            } catch (DbException e) {
                throw new RuntimeException(e);
            }
        }

        // Handle pg jsonb, json
        if (Set.of("json", "jsonb").contains(columnType.toLowerCase())) {
            try {
                return dialect.convertJsonToVO(rs.getObject(index));
            } catch (DbException e) {
                throw new RuntimeException(e);
            }
        }

        return super.getColumnValue(rs, index);
    }
}
