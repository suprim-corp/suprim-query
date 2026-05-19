package dev.suprim.query.autoconfigure;

import java.lang.annotation.*;

/**
 * Marks a {@link javax.sql.DataSource} bean for management by suprim-query.
 *
 * <p>When {@code db.databases} configuration is absent, suprim-query will auto-detect
 * all {@code DataSource} beans annotated with this annotation. The bean name becomes
 * the database ID used in query operations.
 *
 * <p>DataSource beans without this annotation are ignored by suprim-query.
 *
 * <h3>Usage example:</h3>
 * <pre>{@code
 * @Bean
 * @Primary
 * @SuprimSchemas({"public"})
 * public DataSource mainDataSource() { ... }
 * }</pre>
 *
 * @see DbAutoConfiguration
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SuprimSchemas {

    /**
     * The database schemas to load metadata for.
     * Defaults to {@code {"public"}} if not specified.
     */
    String[] value() default {"public"};
}
