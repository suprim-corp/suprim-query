package dev.suprim.query.model;

public record ArrayTypeValueHolder(String jdbcType, String sqlType, Object[] values) {}
