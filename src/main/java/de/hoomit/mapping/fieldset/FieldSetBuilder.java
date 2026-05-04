package de.hoomit.mapping.fieldset;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Resolves the set of field names that should be mapped for a given
 * destination class and field-level / field-string combination.
 *
 * <p>Field strings follow the same grammar as in SAP Hybris, extended with
 * support for arbitrary custom named sets declared via
 * {@link de.hoomit.mapping.fieldset.FieldSetDefinition#custom()}:
 * <ul>
 *   <li>{@code "BASIC"}     – built-in BASIC level</li>
 *   <li>{@code "DEFAULT"}   – built-in DEFAULT level</li>
 *   <li>{@code "FULL"}      – all fields</li>
 *   <li>{@code "SEARCH"}    – any custom name declared on the DTO</li>
 *   <li>{@code "id,name"}   – explicit comma-separated field list</li>
 *   <li>{@code "DEFAULT,description"} – level/name + extra fields</li>
 *   <li>{@code null} or empty – equivalent to DEFAULT</li>
 * </ul>
 * </p>
 */
public final class FieldSetBuilder {

    /**
     * Unified cache: maps each DTO class to a map of upper-cased set-name → field names.
     * Built-in names (BASIC, DEFAULT, FULL) share the same map as custom names.
     */
    private static final Map<Class<?>, Map<String, Set<String>>> CACHE
            = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Resolve the set of fields to map for {@code destClass} given a
     * field-descriptor string such as {@code "DEFAULT,description"} or
     * a custom name such as {@code "SEARCH"}.
     *
     * @param destClass the destination DTO class
     * @param fields    field descriptor (may be {@code null} → DEFAULT)
     * @return immutable set of field names; empty set means "map all fields"
     */
    public static Set<String> resolve(final Class<?> destClass, final String fields) {
        if (fields == null || fields.isBlank()) {
            return resolveByName(destClass, FieldLevel.DEFAULT.name());
        }

        final Map<String, Set<String>> namedSets =
                CACHE.computeIfAbsent(destClass, FieldSetBuilder::buildNamedMap);

        final Set<String> result = Arrays.stream(fields.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .flatMap(token -> {
                    final Set<String> named = namedSets.get(token.toUpperCase());
                    return named != null ? named.stream() : Stream.of(token);
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return Collections.unmodifiableSet(result);
    }

    /**
     * Resolve by an explicit built-in {@link FieldLevel}.
     *
     * @param destClass the destination DTO class
     * @param level     the {@link FieldLevel} to resolve
     * @return immutable set of field names for the given level
     */
    public static Set<String> resolveByLevel(final Class<?> destClass, final FieldLevel level) {
        return resolveByName(destClass, level.name());
    }

    /**
     * Resolve by an arbitrary named set, including custom names declared via
     * {@link de.hoomit.mapping.fieldset.FieldSetDefinition#custom()}.
     *
     * @param destClass the destination DTO class
     * @param name      set name (case-insensitive)
     * @return immutable set of field names, or an empty set when the name is unknown
     */
    public static Set<String> resolveByName(final Class<?> destClass, final String name) {
        return CACHE
                .computeIfAbsent(destClass, FieldSetBuilder::buildNamedMap)
                .getOrDefault(name.toUpperCase(), Collections.emptySet());
    }

    // -------------------------------------------------------------------------
    // Cache construction
    // -------------------------------------------------------------------------

    private static Map<String, Set<String>> buildNamedMap(final Class<?> clazz) {
        final Map<String, Set<String>> map = new LinkedHashMap<>();
        final FieldSetDefinition def = clazz.getAnnotation(FieldSetDefinition.class);

        if (def != null) {
            // Built-in levels
            map.put(FieldLevel.BASIC.name(), toSet(def.basic()));
            map.put(FieldLevel.DEFAULT.name(), toSet(def.defaults()));
            map.put(FieldLevel.FULL.name(),
                    def.full().length == 0 ? allFieldsOf(clazz) : toSet(def.full()));

            // Custom named sets — empty fields() is the "all fields" sentinel
            Arrays.stream(def.custom())
                    .forEach(ns -> map.put(
                            ns.name().toUpperCase(),
                            ns.fields().length == 0 ? allFieldsOf(clazz) : toSet(ns.fields())));
        } else {
            // No annotation — fall back to mapping all fields for every level
            final Set<String> all = allFieldsOf(clazz);
            Arrays.stream(FieldLevel.values())
                    .forEach(level -> map.put(level.name(), all));
        }
        return Collections.unmodifiableMap(map);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Set<String> allFieldsOf(final Class<?> clazz) {
        final Set<String> names = new LinkedHashSet<>();
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            Arrays.stream(c.getDeclaredFields())
                    .map(Field::getName)
                    .forEach(names::add);
        }
        return Collections.unmodifiableSet(names);
    }

    private static Set<String> toSet(final String[] arr) {
        return Arrays.stream(arr)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // Prevent instantiation
    private FieldSetBuilder() {
    }
}
