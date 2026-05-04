package dev.suprim.query.jdbc.config;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.isNull;

@Slf4j
@Getter
public class RoutingDataSource extends AbstractRoutingDataSource {
    private final String defaultId;
    private final Set<String> knownIds;

    public RoutingDataSource(@NonNull Map<String, DataSource> targets, @NonNull String defaultId) {
        if (targets.isEmpty()) {
            throw new IllegalStateException("No data sources configured");
        }

        this.defaultId = defaultId;

        if (!targets.containsKey(defaultId)) {
            throw new IllegalArgumentException("defaultId '%s' not found in targets".formatted(defaultId));
        }

        super.setTargetDataSources((Map) targets);
        super.setDefaultTargetDataSource(targets.get(defaultId));
        super.setLenientFallback(true);
        super.afterPropertiesSet();

        this.knownIds = Collections.unmodifiableSet(targets.keySet());
        log.info("RoutingDataSource ready. defaultId={}, tenants={}", defaultId, knownIds);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        final String key = DatabaseContextHolder.getCurrentDbId();

        if (isNull(key)) {
            // null is expected in single-DB setups or when context is not set
            log.trace("No datasource id in context, using default: '{}'", defaultId);
            return defaultId;
        }

        if (!knownIds.contains(key)) {
            log.warn("Unknown datasource key '{}', falling back to default '{}'", key, defaultId);
            return defaultId;
        }

        log.debug("Datasource Id - {}", key);

        return key;
    }
}
