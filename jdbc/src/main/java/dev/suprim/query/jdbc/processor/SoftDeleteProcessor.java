package dev.suprim.query.jdbc.processor;

import dev.suprim.query.exception.DbException;
import dev.suprim.query.model.SoftDeleteProperties;
import dev.suprim.query.model.context.ReadContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;

import static java.util.Objects.isNull;

/**
 * Appends a soft-delete filter (e.g. {@code deleted_at IS NULL}) to the WHERE clause
 * unless the caller explicitly opts out via {@code ReadContext.includeSoftDeleted}.
 * <p>
 * Runs after {@link RootWhereProcessor} (Order 8) so that the user's RSQL filter
 * is already resolved before we append the soft-delete condition.
 */
@Slf4j
@Order(9)
@RequiredArgsConstructor
public class SoftDeleteProcessor implements ReadProcessor {

    private final SoftDeleteProperties softDeleteProperties;

    @Override
    public void process(ReadContext readContext) throws DbException {
        if (!softDeleteProperties.appliesTo(readContext.getTableName())) {
            return;
        }

        if (readContext.isIncludeSoftDeleted()) {
            log.debug("Soft-delete filter skipped (includeSoftDeleted=true) for table: {}",
                    readContext.getTableName());
            return;
        }

        String column = softDeleteProperties.column();
        String alias = isNull(readContext.getRoot()) ? null : readContext.getRoot().alias();
        String qualifiedColumn = (!isNull(alias) && !alias.isBlank())
                ? alias + "." + column
                : column;

        String softDeleteCondition = qualifiedColumn + " IS NULL";

        String existingWhere = readContext.getRootWhere();
        if (isNull(existingWhere) || existingWhere.isBlank()) {
            readContext.setRootWhere(softDeleteCondition);
        } else {
            readContext.setRootWhere(existingWhere + " AND " + softDeleteCondition);
        }

        log.debug("Soft-delete filter applied: {} for table: {}", softDeleteCondition, readContext.getTableName());
    }
}
