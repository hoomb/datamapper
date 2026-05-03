package de.hoomit.mapping.fieldset;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Resolves the set of field names that should be mapped for a given
 * destination class and field-level / field-string combination.
 *
 * <p>Field strings follow the same grammar as in SAP Hybris:
 * <ul>
 *   <li>{@code "BASIC"}  – maps to {@link FieldLevel#BASIC}</li>
 *   <li>{@code "DEFAULT"} – maps to {@link FieldLevel#DEFAULT}</li>
 *   <li>{@code "FULL"} – maps to all fields</li>
 *   <li>{@code "id,name,price"} – explicit comma-separated list</li>
 *   <li>{@code "DEFAULT,description"} – level + extras</li>
 *   <li>{@code null} or empty – equivalent to DEFAULT</li>
 * </ul>
 * </p>
 */
public class FieldSetBuilder {

    private static final Map<Class<?>, Map<FieldLevel, Set<String>>> CACHE = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Resolve the set of fields to map for {@code destClass} given a
     * field-descriptor string such as {@code "DEFAULT,description"}.
     *
     * @param destClass the destination DTO class
     * @param fields    field descriptor (may be null → DEFAULT)
     * @return set of field names; empty set means "map all fields"
     */
    public static Set<String> resolve(final Class<?> destClass, final String fields) {
        if (fields == null || fields.isBlank()) {
            return resolveByLevel(destClass, FieldLevel.DEFAULT);
        }

        // Split on comma, trim each token
        final String[] tokens = Arrays.stream(fields.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);

        final Set<String> result = new LinkedHashSet<>();
        Arrays.stream(tokens).forEach(token -> {
            parseLevel(token)
                    .ifPresentOrElse(
                            level -> result.addAll(resolveByLevel(destClass, level)),
                            () -> result.add(token));
        });
        return Collections.unmodifiableSet(result);
    }

    /**
     * Resolve by an explicit {@link FieldLevel}.
     */
    public static Set<String> resolveByLevel(final Class<?> destClass, final FieldLevel level) {
        return CACHE
                .computeIfAbsent(destClass, FieldSetBuilder::buildLevelMap)
                .getOrDefault(level, Collections.emptySet());
    }

// -------------------------------------------------------------------------
// Internals
// -------------------------------------------------------------------------

    private static Map<FieldLevel, Set<String>> buildLevelMap(final Class<?> clazz) {
        final Map<FieldLevel, Set<String>> map = new EnumMap<>(FieldLevel.class);
        final FieldSetDefinition def = clazz.getAnnotation(FieldSetDefinition.class);

        if (def != null) {
            map.put(FieldLevel.BASIC, toSet(def.basic()));
            map.put(FieldLevel.DEFAULT, toSet(def.defaults()));
            // Empty full() → sentinel for "all fields"
            map.put(FieldLevel.FULL, def.full().length == 0 ? allFieldsOf(clazz) : toSet(def.full()));
        } else {
            // No annotation – derive heuristically
            final Set<String> all = allFieldsOf(clazz);
            map.put(FieldLevel.BASIC, all);      // same for all levels
            map.put(FieldLevel.DEFAULT, all);
            map.put(FieldLevel.FULL, all);
        }
        return Collections.unmodifiableMap(map);
    }

    private static Set<String> allFieldsOf(final Class<?> clazz) {
        final Set<String> names = new LinkedHashSet<>();
        Class<?> c = clazz;
        while (c != null && c != Object.class) {
            for (final Field f : c.getDeclaredFields()) {
                names.add(f.getName());
            }
            c = c.getSuperclass();
        }
        return Collections.unmodifiableSet(names);
    }

    private static Set<String> toSet(final String[] arr) {
        return Arrays.stream(arr)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Optional<FieldLevel> parseLevel(final String token) {
        try {
            return Optional.of(FieldLevel.valueOf(token.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    // Prevent instantiation
    private FieldSetBuilder() {
    }
}
