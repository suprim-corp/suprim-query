package dev.suprim.query.mapping;

import java.lang.reflect.Field;

/**
 * Maps a single record component or POJO field to its database column name and Java type.
 *
 * @param columnName  the database column name (from {@link Column} annotation or snake_case conversion)
 * @param targetType  the Java type of the field/component
 * @param field       the reflective Field reference (null for records, which use constructor args)
 */
record ComponentMapping(
        String columnName,
        Class<?> targetType,
        Field field
) {}
