package dev.suprim.query.model.dto;

import java.util.Map;

/**
 * Represents a single update operation within a bulk update batch.
 *
 * @param data   column-value pairs to update
 * @param filter RSQL filter scoping which rows to update
 */
public record BulkUpdate(
        Map<String, Object> data,
        String filter
) {
}
