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
 * {@link FieldSetDefinition#custom()}, and for <b>cumulative definitions</b>
 * via {@link FieldSets} reference constants:
 * <ul>
 *   <li>{@code "BASIC"}     – built-in BASIC level</li>
 *   <li>{@code "DEFAULT"}   – built-in DEFAULT level</li>
 *   <li>{@code "FULL"}      – all fields</li>
 *   <li>{@code "SEARCH"}    – any custom name declared on the DTO</li>
 *   <li>{@code "id,name"}   – explicit comma-separated field list</li>
 *   <li>{@code "DEFAULT,description"} – set + extra fields</li>
 *   <li>{@code null} or empty – equivalent to DEFAULT</li>
 * </ul>
 * </p>
 *
 * <p>Inside {@link FieldSetDefinition} annotation arrays, entries starting with
 * {@code @} are <em>references</em> to other sets and are expanded transitively.
 * See {@link FieldSets} for the recommended constants.</p>
 */
public final class FieldSetBuilder {

    /**
     * Reference prefix: any entry beginning with this is a set reference.
     */
    private static final String REF_PREFIX = "@";

    /**
     * Unified cache: maps each DTO class to a map of upper-cased set-name → field names.
     * Built-in names (BASIC, DEFAULT, FULL) share the same map as custom names.
     */
    private static final Map<Class<?>, Map<String, Set<String>>> CACHE = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Resolve the set of fields to map for {@code destClass} given a
     * field-descriptor string such as {@code "DEFAULT,description"} or
     * a custom name such as {@code "SEARCH"}.
     */
    public static Set<String> resolve(final Class<?> destClass, final String fields) {
        if (fields == null || fields.isBlank()) {
            return resolveByName(destClass, FieldLevel.DEFAULT.name());
        }

        final Map<String, Set<String>> namedSets = CACHE.computeIfAbsent(destClass, FieldSetBuilder::buildNamedMap);

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
     * Resolve by an arbitrary set name (case-insensitive).
     */
    public static Set<String> resolveByName(final Class<?> destClass, final String name) {
        return CACHE
                .computeIfAbsent(destClass, FieldSetBuilder::buildNamedMap)
                .getOrDefault(name.toUpperCase(), Collections.emptySet());
    }

    // -------------------------------------------------------------------------
    // Cache construction (two-phase: collect raw, then resolve references)
    // -------------------------------------------------------------------------

    private static Map<String, Set<String>> buildNamedMap(final Class<?> clazz) {
        final FieldSetDefinition def = clazz.getAnnotation(FieldSetDefinition.class);

        // No annotation: every level returns all fields
        if (def == null) {
            final Set<String> all = allFieldsOf(clazz);
            final Map<String, Set<String>> fallback = new LinkedHashMap<>();
            Arrays.stream(FieldLevel.values())
                    .forEach(level -> fallback.put(level.name(), all));
            return Collections.unmodifiableMap(fallback);
        }

        // Phase 1 — collect raw entries per upper-cased name. Empty array stays empty
        // and is treated as the "all fields" sentinel later.
        final Map<String, String[]> raw = new LinkedHashMap<>();
        raw.put(FieldLevel.BASIC.name(), def.basic());
        raw.put(FieldLevel.DEFAULT.name(), def.defaults());
        raw.put(FieldLevel.FULL.name(), def.full());

        Arrays.stream(def.custom()).forEach(ns -> {
            final String upper = ns.name().toUpperCase();
            if (isReservedLevel(upper)) {
                throw new IllegalStateException(
                        "Custom field-set name '" + ns.name() + "' on " + clazz.getName()
                                + " shadows a reserved built-in level (BASIC/DEFAULT/FULL).");
            }
            if (raw.put(upper, ns.fields()) != null) {
                throw new IllegalStateException(
                        "Duplicate custom field-set name '" + ns.name() + "' on " + clazz.getName());
            }
        });

        // Phase 2 — resolve each set, expanding @REFs transitively with cycle detection.
        final Map<String, Set<String>> resolved = new LinkedHashMap<>();
        raw.keySet().forEach(name ->
                resolveSet(name, raw, resolved, new LinkedHashSet<>(), clazz));

        return Collections.unmodifiableMap(resolved);
    }

    private static Set<String> resolveSet(
            final String name,
            final Map<String, String[]> raw,
            final Map<String, Set<String>> resolved,
            final Set<String> visiting,
            final Class<?> clazz) {

        if (resolved.containsKey(name)) {
            return resolved.get(name);
        }
        if (visiting.contains(name)) {
            throw new IllegalStateException(
                    "Circular field-set reference detected on " + clazz.getName()
                            + ": " + String.join(" -> ", visiting) + " -> " + name);
        }

        visiting.add(name);

        final String[] entries = raw.get(name);
        final Set<String> result = (entries.length == 0)
                ? new LinkedHashSet<>(allFieldsOf(clazz))   // empty = sentinel for "all fields"
                : expandEntries(entries, name, raw, resolved, visiting, clazz);

        visiting.remove(name);

        final Set<String> immutable = Collections.unmodifiableSet(result);
        resolved.put(name, immutable);
        return immutable;
    }

    private static Set<String> expandEntries(
            final String[] entries,
            final String currentName,
            final Map<String, String[]> raw,
            final Map<String, Set<String>> resolved,
            final Set<String> visiting,
            final Class<?> clazz) {

        final Set<String> out = new LinkedHashSet<>();
        for (final String entry : entries) {
            if (entry.startsWith(REF_PREFIX)) {
                final String refName = entry.substring(REF_PREFIX.length()).toUpperCase();
                if (!raw.containsKey(refName)) {
                    throw new IllegalStateException(
                            "Unknown field-set reference '" + entry + "' inside set '"
                                    + currentName + "' on " + clazz.getName()
                                    + ". Known sets: " + raw.keySet());
                }
                out.addAll(resolveSet(refName, raw, resolved, visiting, clazz));
            } else {
                out.add(entry);
            }
        }
        return out;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static boolean isReservedLevel(final String upperName) {
        return Arrays.stream(FieldLevel.values())
                .anyMatch(level -> level.name().equals(upperName));
    }

    private static Set<String> allFieldsOf(final Class<?> clazz) {
        final Set<String> names = new LinkedHashSet<>();
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            Arrays.stream(c.getDeclaredFields())
                    .map(Field::getName)
                    .forEach(names::add);
        }
        return Collections.unmodifiableSet(names);
    }

    private FieldSetBuilder() {
    }
}
