package dev.suprim.query.model.dto;

import java.util.Map;

public record CreationResponse(int row, Map<String, Object> keys) {}
