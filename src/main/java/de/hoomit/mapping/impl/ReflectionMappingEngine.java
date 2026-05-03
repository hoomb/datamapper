package de.hoomit.mapping.impl;

import de.hoomit.mapping.context.MappingContext;
import de.hoomit.mapping.filter.FieldFilter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core engine that copies fields from a source object to a destination object
 * using reflection, guided by a {@link MappingContext} and a list of
 * {@link FieldFilter}s.
 *
 * <p>Matching strategy (in order):
 * <ol>
 *   <li>Getter on source (getXxx / isXxx) → setter on dest (setXxx)</li>
 *   <li>Direct field access as fallback</li>
 * </ol>
 * </p>
 */
class ReflectionMappingEngine {

    private static final Map<Class<?>, Map<String, Method>> GETTER_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Map<String, Method>> SETTER_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, List<Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------

    void map(final Object source, final Object dest, final MappingContext ctx, final List<FieldFilter> filters) {
        final Class<?> srcClass = source.getClass();
        final Class<?> destClass = dest.getClass();

        final Map<String, Method> getters = gettersOf(srcClass);
        final Map<String, Method> setters = settersOf(destClass);

        for (final Map.Entry<String, Method> setterEntry : setters.entrySet()) {
            final String fieldName = setterEntry.getKey();
            final Method setter = setterEntry.getValue();

            // 1. Field-set filter
            if (!ctx.includes(fieldName)) continue;

            // 2. Resolve value from getter or direct field
            final Object value = resolveValue(source, fieldName, getters);

            // 3. Null handling
            if (value == null && !ctx.isMapNulls()) continue;

            // 4. FieldFilter chain
            if (!passFilters(filters, fieldName, value, ctx, srcClass, destClass)) continue;

            // 5. Write to destination
            try {
                setter.invoke(dest, value);
            } catch (Exception e) {
                // Type mismatch – attempt direct field write
                writeDirectField(dest, fieldName, value, ctx.isMapNulls());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Object resolveValue(final Object source, final String fieldName, final Map<String, Method> getters) {
        // Try getter first
        final Method getter = getters.get(fieldName);
        if (getter != null) {
            try {
                return getter.invoke(source);
            } catch (final Exception ignored) {
            }
        }
        // Fall back to direct field access
        return readDirectField(source, fieldName);
    }

    private Object readDirectField(final Object obj, final String fieldName) {
        Class<?> clazz = obj.getClass();
        while (clazz != null && clazz != Object.class) {
            try {
                final Field f = clazz.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f.get(obj);
            } catch (NoSuchFieldException ignored) {
                clazz = clazz.getSuperclass();
            } catch (final Exception e) {
                return null;
            }
        }
        return null;
    }

    private void writeDirectField(final Object obj, final String fieldName, final Object value, final boolean mapNulls) {
        if (value == null && !mapNulls) return;
        Class<?> clazz = obj.getClass();
        while (clazz != null && clazz != Object.class) {
            try {
                final Field f = clazz.getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(obj, value);
                return;
            } catch (final NoSuchFieldException ignored) {
                clazz = clazz.getSuperclass();
            } catch (Exception ignored) {
            }
        }
    }

    private boolean passFilters(final List<FieldFilter> filters, final String fieldName, final Object value,
                                final MappingContext ctx, final Class<?> src, final Class<?> dest) {
        return filters.stream()
                .noneMatch(filter -> filter.isApplicable(src, dest) && !filter.include(fieldName, value, ctx));
    }

    // -------------------------------------------------------------------------
    // Cache-building helpers
    // -------------------------------------------------------------------------

    private Map<String, Method> gettersOf(final Class<?> clazz) {
        return GETTER_CACHE.computeIfAbsent(clazz, c -> {
            final Map<String, Method> map = new LinkedHashMap<>();
            Class<?> cur = c;
            while (cur != null && cur != Object.class) {
                for (final Method m : cur.getDeclaredMethods()) {
                    if (!Modifier.isPublic(m.getModifiers())) continue;
                    if (m.getParameterCount() != 0) continue;
                    final String name = m.getName();
                    if (name.startsWith("get") && name.length() > 3) {
                        map.putIfAbsent(decapitalize(name.substring(3)), m);
                    } else if (name.startsWith("is") && name.length() > 2
                            && (m.getReturnType() == boolean.class
                            || m.getReturnType() == Boolean.class)) {
                        map.putIfAbsent(decapitalize(name.substring(2)), m);
                    }
                }
                cur = cur.getSuperclass();
            }
            return Collections.unmodifiableMap(map);
        });
    }

    private Map<String, Method> settersOf(final Class<?> clazz) {
        return SETTER_CACHE.computeIfAbsent(clazz, c -> {
            final Map<String, Method> map = new LinkedHashMap<>();
            Class<?> cur = c;
            while (cur != null && cur != Object.class) {
                for (final Method m : cur.getDeclaredMethods()) {
                    if (!Modifier.isPublic(m.getModifiers())) continue;
                    if (m.getParameterCount() != 1) continue;
                    final String name = m.getName();
                    if (name.startsWith("set") && name.length() > 3) {
                        map.putIfAbsent(decapitalize(name.substring(3)), m);
                    }
                }
                cur = cur.getSuperclass();
            }
            return Collections.unmodifiableMap(map);
        });
    }

    private static String decapitalize(final String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}
