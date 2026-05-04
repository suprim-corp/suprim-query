package dev.suprim.query.jdbc.processor;

import dev.suprim.query.exception.DbException;
import dev.suprim.query.exception.DbRuntimeException;
import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.context.ReadContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Objects.isNull;

@Slf4j
@Order(4)
public class RootTableFieldProcessor implements ReadProcessor {
    @Override
    public void process(ReadContext readContext) {
        String rawFields = readContext.getFields();
        String fields = isNull(rawFields) ? null : rawFields.trim();

        log.debug("Fields - {}", fields);

        if (isNull(fields)) {
            // Most likely a count query
            return;
        }

        List<DbColumn> columnList = new ArrayList<>();
        if ("*".equals(fields)) {
            // Include all fields of the root table
            columnList.addAll(readContext.getRoot().buildColumns());
        } else {
            // Query has specific columns so parse and map it.
            List<DbColumn> columns = Arrays.stream(readContext.getFields().split(","))
                    .map(col -> {
                        try {
                            return readContext.getRoot().buildColumn(col);
                        } catch (DbException e) {
                            throw new DbRuntimeException(e);
                        }
                    })
                    .toList();
            columnList.addAll(columns);
        }

        log.debug("Column List - {}", columnList);
        readContext.setCols(columnList);
    }
}
