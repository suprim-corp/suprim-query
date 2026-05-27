package dev.suprim.query.mapping;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps {@code Map<String, Object>} query results to typed Java records or POJOs.
 *
 * <p>Supports:
 * <ul>
 *   <li>Java records — mapped via canonical constructor</li>
 *   <li>POJOs — mapped via no-arg constructor + field injection</li>
 *   <li>{@link Column} annotation for explicit column name mapping</li>
 *   <li>Automatic camelCase-to-snake_case name resolution</li>
 *   <li>Type coercion for common DB types (numbers, temporals, UUID, enums, booleans)</li>
 * </ul>
 *
 * <p>Thread-safe. Caches reflection metadata per type for repeated use.
 *
 * <pre>{@code
 * ResultMapper mapper = new ResultMapper();
 *
 * // Single row
 * UserDto user = mapper.mapOne(row, UserDto.class);
 *
 * // Optional (null-safe)
 * Optional<UserDto> maybeUser = mapper.mapOptional(row, UserDto.class);
 *
 * // List
 * List<UserDto> users = mapper.mapList(rows, UserDto.class);
 * }</pre>
 */
public final class ResultMapper {

    private final ConcurrentHashMap<Class<?>, TypeMetadata> cache = new ConcurrentHashMap<>();

    /**
     * Maps a single row to the target type.
     *
     * @param row  column-value map from query result (must not be null)
     * @param type target class — record or POJO with no-arg constructor
     * @param <T>  target type
     * @return mapped instance
     * @throws MappingException      if mapping fails (reflection error, type coercion failure)
     * @throws NullPointerException  if row or type is null
     */
    public <T> T mapOne(Map<String, Object> row, Class<T> type) {
        Objects.requireNonNull(row, "row must not be null");
        Objects.requireNonNull(type, "type must not be null");
        return mapRow(row, type);
    }

    /**
     * Maps a single row to Optional. Returns empty if row is null or empty.
     *
     * @param row  column-value map (nullable — returns empty Optional)
     * @param type target class
     * @param <T>  target type
     * @return Optional containing mapped instance, or empty
     * @throws MappingException      if mapping fails
     * @throws NullPointerException  if type is null
     */
    public <T> Optional<T> mapOptional(Map<String, Object> row, Class<T> type) {
        Objects.requireNonNull(type, "type must not be null");
        if (row == null || row.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapRow(row, type));
    }

    /**
     * Maps a list of rows to a typed list.
     *
     * @param rows list of column-value maps (must not be null)
     * @param type target class
     * @param <T>  target type
     * @return immutable list of mapped instances
     * @throws MappingException      if mapping fails for any row
     * @throws NullPointerException  if rows or type is null
     */
    public <T> List<T> mapList(List<Map<String, Object>> rows, Class<T> type) {
        Objects.requireNonNull(rows, "rows must not be null");
        Objects.requireNonNull(type, "type must not be null");
        return rows.stream()
                .map(row -> mapRow(row, type))
                .toList();
    }

    // --- Internal ---

    private <T> T mapRow(Map<String, Object> row, Class<T> type) {
        TypeMetadata metadata = cache.computeIfAbsent(type, TypeMetadata::resolve);

        if (metadata.isRecord()) {
            return mapRecord(row, type, metadata);
        }
        return mapPojo(row, type, metadata);
    }

    @SuppressWarnings("unchecked")
    private <T> T mapRecord(Map<String, Object> row, Class<T> type, TypeMetadata metadata) {
        Object[] args = new Object[metadata.components().size()];

        for (int i = 0; i < metadata.components().size(); i++) {
            ComponentMapping mapping = metadata.components().get(i);
            Object rawValue = resolveValue(row, mapping.columnName());
            args[i] = TypeCoercer.coerce(rawValue, mapping.targetType());
        }

        try {
            Constructor<?> constructor = metadata.constructor();
            constructor.setAccessible(true);
            return (T) constructor.newInstance(args);
        } catch (Exception e) {
            throw new MappingException(
                    "Failed to instantiate record " + type.getSimpleName(), e
            );
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T mapPojo(Map<String, Object> row, Class<T> type, TypeMetadata metadata) {
        try {
            Constructor<?> constructor = metadata.constructor();
            constructor.setAccessible(true);
            T instance = (T) constructor.newInstance();

            for (ComponentMapping mapping : metadata.components()) {
                Object rawValue = resolveValue(row, mapping.columnName());
                Object coerced = TypeCoercer.coerce(rawValue, mapping.targetType());

                Field field = mapping.field();
                field.setAccessible(true);
                field.set(instance, coerced);
            }

            return instance;
        } catch (MappingException e) {
            throw e;
        } catch (Exception e) {
            throw new MappingException(
                    "Failed to instantiate POJO " + type.getSimpleName(), e
            );
        }
    }

    /**
     * Resolves value from row: exact key match first, then case-insensitive fallback.
     */
    private Object resolveValue(Map<String, Object> row, String columnName) {
        if (row.containsKey(columnName)) {
            return row.get(columnName);
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(columnName)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
