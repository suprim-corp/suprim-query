package dev.suprim.query.model.dto;

import lombok.Builder;

@Builder
public record DeleteResponse(int rows) {}
