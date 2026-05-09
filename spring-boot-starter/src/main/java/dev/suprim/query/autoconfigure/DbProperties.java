package dev.suprim.query.autoconfigure;

import dev.suprim.query.jdbc.config.DatabaseConnectionDetail;
import dev.suprim.query.jdbc.config.DatabaseProperties;
import dev.suprim.query.model.SoftDeleteProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Getter
@Setter
@ConfigurationProperties(prefix = "db")
public class DbProperties extends DatabaseProperties {

    private SoftDeleteConfig softDelete;

    /**
     * Sets databases and auto-sets defaultDatabaseId if not already set.
     * Uses inherited 'databases' field from DatabaseProperties - do NOT shadow it.
     */
    @Override
    public void setDatabases(List<DatabaseConnectionDetail> databases) {
        super.setDatabases(databases);
        if (nonNull(databases) && !databases.isEmpty() && isNull(getDefaultDatabaseId())) {
            setDefaultDatabaseId(databases.get(0).id());
        }
    }

    /**
     * Resolves soft-delete configuration into an immutable {@link SoftDeleteProperties}.
     */
    public SoftDeleteProperties resolveSoftDeleteProperties() {
        if (isNull(softDelete) || !softDelete.isEnabled()) {
            return SoftDeleteProperties.disabled();
        }
        return new SoftDeleteProperties(
                true,
                softDelete.getColumn(),
                softDelete.getTables()
        );
    }

    @Getter
    @Setter
    public static class SoftDeleteConfig {
        private boolean enabled = false;
        private String column = "deleted_at";
        private List<String> tables;
    }
}
