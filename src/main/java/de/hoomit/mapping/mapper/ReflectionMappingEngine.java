package de.hoomit.mapping.mapper;

import de.hoomit.mapping.context.MappingContext;
import de.hoomit.mapping.filter.FieldFilter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
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

    private final FieldMappingRegistry fieldMappings;

    ReflectionMappingEngine(final FieldMappingRegistry fieldMappings) {
        this.fieldMappings = fieldMappings;
    }

    // -------------------------------------------------------------------------

    void map(final Object source, final Object dest,
             final MappingContext ctx, final List<FieldFilter> filters) {

        final Class<?> srcClass = source.getClass();
        final Class<?> destClass = dest.getClass();
        final Map<String, Method> getters = gettersOf(srcClass);
        final Map<String, Method> setters = settersOf(destClass);
        final Map<String, String> renames = fieldMappings.renamesFor(srcClass, destClass);

        // Inverted view: destination field name -> source field name to read from.
        // The registry stores sourceName -> destName; we iterate over setters, so
        // we need the reverse direction.
        final Map<String, String> sourceForDest = invert(renames);

        setters.forEach((destFieldName, setter) -> {

            // 1. Field-set filter (descriptors refer to DESTINATION names)
            if (!ctx.includes(destFieldName)) return;

            // 2. Resolve which source-side name to read.
            //    If a @FieldMapping declares a rename, use the source name;
            //    otherwise fall back to the destination name (1:1 by convention).
            final String sourceFieldName = sourceForDest.getOrDefault(destFieldName, destFieldName);

            // 3. Resolve value from getter or direct field
            final Object value = resolveValue(source, sourceFieldName, getters);

            // 4. Null handling
            if (value == null && !ctx.isMapNulls()) return;

            // 5. FieldFilter chain (filters see the destination field name)
            if (!passFilters(filters, destFieldName, value, ctx, srcClass, destClass)) return;

            // 6. Write to destination
            try {
                setter.invoke(dest, value);
            } catch (final Exception e) {
                writeDirectField(dest, destFieldName, value, ctx.isMapNulls());
            }
        });
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Object resolveValue(final Object source, final String fieldName,
                                final Map<String, Method> getters) {
        final Method getter = getters.get(fieldName);
        if (getter != null) {
            try {
                return getter.invoke(source);
            } catch (final Exception ignored) {
            }
        }
        return readDirectField(source, fieldName);
    }

    private Object readDirectField(final Object obj, final String fieldName) {
        for (Class<?> c = obj.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                final Field f = c.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f.get(obj);
            } catch (final NoSuchFieldException ignored) {
                // walk up
            } catch (final Exception e) {
                return null;
            }
        }
        return null;
    }

    private void writeDirectField(final Object obj, final String fieldName,
                                  final Object value, final boolean mapNulls) {
        if (value == null && !mapNulls) return;
        for (Class<?> c = obj.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                final Field f = c.getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(obj, value);
                return;
            } catch (final NoSuchFieldException ignored) {
                // walk up
            } catch (final Exception ignored) {
            }
        }
    }

    private boolean passFilters(final List<FieldFilter> filters, final String fieldName,
                                final Object value, final MappingContext ctx,
                                final Class<?> src, final Class<?> dest) {
        return filters.stream()
                .noneMatch(filter ->
                        filter.isApplicable(src, dest) && !filter.include(fieldName, value, ctx));
    }

    private static Map<String, String> invert(final Map<String, String> renames) {
        if (renames.isEmpty()) return Collections.emptyMap();
        return renames.entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getValue,
                        Map.Entry::getKey,
                        (a, b) -> a));   // collisions: first one wins; validator catches these at registration
    }
    // -------------------------------------------------------------------------
    // Cache-building helpers
    // -------------------------------------------------------------------------

    private Map<String, Method> gettersOf(final Class<?> clazz) {
        return GETTER_CACHE.computeIfAbsent(clazz, c -> {
            final Map<String, Method> map = new LinkedHashMap<>();
            for (Class<?> cur = c; cur != null && cur != Object.class; cur = cur.getSuperclass()) {
                Arrays.stream(cur.getDeclaredMethods())
                        .filter(m -> Modifier.isPublic(m.getModifiers()))
                        .filter(m -> m.getParameterCount() == 0)
                        .forEach(m -> {
                            final String name = m.getName();
                            if (name.startsWith("get") && name.length() > 3) {
                                map.putIfAbsent(decapitalize(name.substring(3)), m);
                            } else if (name.startsWith("is") && name.length() > 2
                                    && (m.getReturnType() == boolean.class
                                    || m.getReturnType() == Boolean.class)) {
                                map.putIfAbsent(decapitalize(name.substring(2)), m);
                            }
                        });
            }
            return Collections.unmodifiableMap(map);
        });
    }

    private Map<String, Method> settersOf(final Class<?> clazz) {
        return SETTER_CACHE.computeIfAbsent(clazz, c -> {
            final Map<String, Method> map = new LinkedHashMap<>();
            for (Class<?> cur = c; cur != null && cur != Object.class; cur = cur.getSuperclass()) {
                Arrays.stream(cur.getDeclaredMethods())
                        .filter(m -> Modifier.isPublic(m.getModifiers()))
                        .filter(m -> m.getParameterCount() == 1)
                        .filter(m -> m.getName().startsWith("set") && m.getName().length() > 3)
                        .forEach(m -> map.putIfAbsent(decapitalize(m.getName().substring(3)), m));
            }
            return Collections.unmodifiableMap(map);
        });
    }

    private static String decapitalize(final String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}
