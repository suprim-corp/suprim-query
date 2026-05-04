package dev.suprim.query.jdbc.config;

public record EnvironmentProperties(
        boolean enableDatetimeFormatting,
        String timeFormat,
        String dateFormat,
        String dateTimeFormat,
        int defaultFetchLimit
) {
}
