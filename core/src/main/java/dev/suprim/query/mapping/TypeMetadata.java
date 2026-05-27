package dev.suprim.query.mapping;

import tools.jackson.databind.PropertyNamingStrategies;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reflection metadata for a target mapping type (record or POJO).
 *
 * <p>Resolves column-to-field mappings at first access, then caches the result.
 * Converts camelCase field names to snake_case when no {@link Column} annotation is present.
 *
 * @param isRecord    true if the target type is a Java record
 * @param constructor canonical constructor (records) or no-arg constructor (POJOs)
 * @param components  ordered list of field/component mappings
 */
record TypeMetadata(
        boolean isRecord,
        Constructor<?> constructor,
        List<ComponentMapping> components
) {

    /**
     * Resolves metadata for the given type. Determines whether it's a record or POJO
     * and extracts constructor + field mappings.
     *
     * @param type target class
     * @return resolved metadata
     * @throws MappingException if no suitable constructor is found
     */
    static TypeMetadata resolve(Class<?> type) {
        return type.isRecord() ? resolveRecord(type) : resolvePojo(type);
    }

    private static TypeMetadata resolveRecord(Class<?> type) {
        RecordComponent[] components = type.getRecordComponents();
        List<ComponentMapping> mappings = new ArrayList<>(components.length);
        Class<?>[] paramTypes = new Class<?>[components.length];

        for (int i = 0; i < components.length; i++) {
            RecordComponent rc = components[i];
            String columnName = resolveColumnName(rc);
            paramTypes[i] = rc.getType();
            mappings.add(new ComponentMapping(columnName, rc.getType(), null));
        }

        try {
            Constructor<?> constructor = type.getDeclaredConstructor(paramTypes);
            return new TypeMetadata(true, constructor, List.copyOf(mappings));
        } catch (NoSuchMethodException e) {
            throw new MappingException(
                    "No canonical constructor found for record " + type.getSimpleName(), e
            );
        }
    }

    private static TypeMetadata resolvePojo(Class<?> type) {
        List<ComponentMapping> mappings = new ArrayList<>();
        Map<String, Field> fieldMap = collectFields(type);

        for (Field field : fieldMap.values()) {
            String columnName = resolveColumnName(field);
            mappings.add(new ComponentMapping(columnName, field.getType(), field));
        }

        try {
            Constructor<?> constructor = type.getDeclaredConstructor();
            return new TypeMetadata(false, constructor, List.copyOf(mappings));
        } catch (NoSuchMethodException e) {
            throw new MappingException(
                    "No no-arg constructor found for " + type.getSimpleName()
                            + ". Use a record or add a no-arg constructor.", e
            );
        }
    }

    private static String resolveColumnName(RecordComponent rc) {
        Column column = rc.getAnnotation(Column.class);
        if (column != null) {
            return column.value();
        }
        return toSnakeCase(rc.getName());
    }

    private static String resolveColumnName(Field field) {
        Column column = field.getAnnotation(Column.class);
        if (column != null) {
            return column.value();
        }
        return toSnakeCase(field.getName());
    }

    /**
     * Collects all non-static, non-synthetic fields from the class hierarchy.
     * Subclass fields take priority (putIfAbsent ensures first-found wins).
     */
    private static Map<String, Field> collectFields(Class<?> type) {
        Map<String, Field> fields = new HashMap<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (field.isSynthetic()) {
                    continue;
                }
                fields.putIfAbsent(field.getName(), field);
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    /**
     * Converts camelCase to snake_case by extending Jackson's SnakeCaseStrategy
     * to access the protected {@code translate()} method.
     */
    private static final class SnakeCaseConverter extends PropertyNamingStrategies.SnakeCaseStrategy {

        static final SnakeCaseConverter INSTANCE = new SnakeCaseConverter();

        @Override
        public String translate(String input) {
            return super.translate(input);
        }
    }

    /**
     * Converts camelCase to snake_case using Jackson's built-in strategy.
     * Examples: "firstName" -> "first_name", "createdAt" -> "created_at"
     */
    static String toSnakeCase(String name) {
        return SnakeCaseConverter.INSTANCE.translate(name);
    }
}
