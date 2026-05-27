package dev.suprim.query.model.dto;

import lombok.Builder;

import java.util.List;
import java.util.Optional;

/**
 * Pagination wrapper for query results.
 *
 * @param data    the result rows for the current page
 * @param total   total number of matching rows (from COUNT query)
 * @param limit   maximum rows per page
 * @param offset  zero-based offset of the first row in this page
 * @param hasNext whether more rows exist beyond this page
 * @param <T>     the row type
 */
@Builder
public record Page<T>(
        List<T> data,
        long total,
        int limit,
        long offset,
        boolean hasNext
) {
    public Page {
        data = Optional.ofNullable(data)
                .map(List::copyOf)
                .orElseGet(List::of);
    }
}
