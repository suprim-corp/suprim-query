package dev.suprim.query.jdbc.processor;

import dev.suprim.query.exception.DbErrorCode;
import dev.suprim.query.exception.DbException;
import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.DbSort;
import dev.suprim.query.model.context.ReadContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.Objects.isNull;

@Slf4j
@Order(12)
@RequiredArgsConstructor
public class OrderByProcessor implements ReadProcessor {

    private static final Set<String> ALLOWED_DIRECTIONS = Set.of("ASC", "DESC");

    @Override
    public void process(ReadContext readContext) throws DbException {
        if (isNull(readContext.getSorts()) || readContext.getSorts().isEmpty()) {
            return;
        }

        List<DbSort> dbSortList = new ArrayList<>();

        for (String sort : readContext.getSorts()) {
            log.debug("SORT - {}", sort);

            if (isNull(sort) || sort.isBlank()) {
                throw new DbException(DbErrorCode.INVALID_REQUEST,
                        "Sort expression must not be blank");
            }

            String[] sortParts = sort.split(";");
            String rawColumn = sortParts[0].trim();
            String rawDirection = sortParts.length == 2 ? sortParts[1].trim().toUpperCase() : "ASC";

            if (!ALLOWED_DIRECTIONS.contains(rawDirection)) {
                throw new DbException(DbErrorCode.INVALID_REQUEST,
                        "Invalid sort direction '" + rawDirection + "'. Allowed: ASC, DESC");
            }

            // Validate column against table metadata — prevents SQL injection
            // via unrecognised column names and ensures canonical quoting
            DbColumn col = readContext.getRoot().buildColumn(rawColumn);

            dbSortList.add(new DbSort(
                    readContext.getTableName(),
                    readContext.getRoot().alias(),
                    col.name(),
                    rawDirection
            ));
        }

        readContext.setDbSortList(dbSortList);
    }
}
