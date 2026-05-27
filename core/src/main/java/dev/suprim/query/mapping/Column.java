package dev.suprim.query.mapping;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps a record component or field to a database column name.
 * Use when the Java name differs from the DB column name.
 *
 * <pre>{@code
 * public record UserDto(
 *     UUID id,
 *     @Column("first_name") String firstName,
 *     @Column("created_at") LocalDateTime createdAt
 * ) {}
 * }</pre>
 */
@Target({ElementType.RECORD_COMPONENT, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {
	/**
	 * The database column name this component/field maps to.
	 */
	String value();
}
