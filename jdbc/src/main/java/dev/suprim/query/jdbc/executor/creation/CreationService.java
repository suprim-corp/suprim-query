package dev.suprim.query.jdbc.executor.creation;

import dev.suprim.query.model.dto.CreationResponse;

import java.util.List;
import java.util.Map;

public interface CreationService {
    CreationResponse execute(
            String dbId,
            String schema,
            String table,
            List<String> columns,
            Map<String, Object> data,
            boolean tsIdEnabled,
            List<String> sequences
    );
}
