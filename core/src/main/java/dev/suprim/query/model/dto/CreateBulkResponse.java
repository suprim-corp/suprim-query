package dev.suprim.query.model.dto;

import java.util.List;
import java.util.Map;

public record CreateBulkResponse(int[] rows, List<Map<String, Object>> keys) {}
