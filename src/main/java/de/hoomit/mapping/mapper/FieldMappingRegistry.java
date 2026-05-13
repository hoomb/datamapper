package de.hoomit.mapping.mapper;

import de.hoomit.mapping.annotation.FieldMapping;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Computes and caches field-name renames between class pairs based on
 * {@link FieldMapping} declarations. Lookup is symmetric: an annotation
 * declared on either the source or the destination side is honoured in both
 * directions.
 *
 * <p>For a given {@code (sourceClass, destClass)} pair, the registry returns
 * a {@code Map<sourceFieldName, destFieldName>} — i.e. when copying a value
 * out of {@code sourceFieldName}, write it into {@code destFieldName} on the
 * destination.
 */
public final class FieldMappingRegistry {

    private record Key(Class<?> source, Class<?> dest) {
    }

    private final Map<Key, Map<String, String>> cache = new ConcurrentHashMap<>();

    /**
     * Returns the source→destination field-name renames for the given pair.
     * Empty map (never {@code null}) if no renames apply.
     */
    public Map<String, String> renamesFor(final Class<?> sourceClass, final Class<?> destClass) {
        return cache.computeIfAbsent(new Key(sourceClass, destClass), this::compute);
    }

    private Map<String, String> compute(final Key key) {
        final Map<String, String> renames = new HashMap<>();

        // Source-side declarations: sourceField -> pairedField (on dest).
        // Source side wins on conflict, so collect these first with put().
        Arrays.stream(key.source().getDeclaredFields())
                .forEach(field -> Arrays.stream(field.getAnnotationsByType(FieldMapping.class))
                        .filter(mapping -> mapping.pairedClass().equals(key.dest()))
                        .forEach(mapping -> renames.put(field.getName(), mapping.pairedField())));

        // Destination-side declarations: pairedField (on source) -> destField.
        // putIfAbsent preserves any source-side declaration that already won.
        Arrays.stream(key.dest().getDeclaredFields())
                .forEach(field -> Arrays.stream(field.getAnnotationsByType(FieldMapping.class))
                        .filter(mapping -> mapping.pairedClass().equals(key.source()))
                        .forEach(mapping -> renames.putIfAbsent(mapping.pairedField(), field.getName())));

        return renames.isEmpty() ? Collections.emptyMap() : Map.copyOf(renames);
    }
}