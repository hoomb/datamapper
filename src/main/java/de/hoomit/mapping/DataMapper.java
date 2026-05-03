package de.hoomit.mapping;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Central mapping interface that provides object-to-object mapping with optional
 * <em>field-set filtering</em>.
 *
 * <p>Field strings follow Hybris conventions:
 * <ul>
 *   <li>{@code null} / empty   → DEFAULT field set</li>
 *   <li>{@code "BASIC"}        → BASIC fields only</li>
 *   <li>{@code "DEFAULT"}      → DEFAULT fields</li>
 *   <li>{@code "FULL"}         → all fields</li>
 *   <li>{@code "id,name,price"}  → explicit list</li>
 *   <li>{@code "DEFAULT,description"} → level + extras</li>
 * </ul>
 * </p>
 *
 * <p>Context keys:
 * <ul>
 *   <li>{@link #FIELD_SET_NAME} – the field set descriptor string</li>
 *   <li>{@link #MAP_NULLS}      – whether null values overwrite destination fields</li>
 *   <li>{@link #FIELD_PREFIX}   – dotted prefix for nested field resolution</li>
 * </ul>
 * </p>
 */
public interface DataMapper {

    /**
     * Context key for the field set descriptor.
     */
    String FIELD_SET_NAME = "fieldSetName";

    /**
     * Context key for the mapNulls flag.
     */
    String MAP_NULLS = "mapNulls";

    /**
     * Context key for the field prefix.
     */
    String FIELD_PREFIX = "fieldPrefix";

    // =========================================================================
    // Single-object mapping – create & return
    // =========================================================================

    /**
     * Create and return a new instance of type {@code D} mapped from {@code sourceObject}.
     * Uses the DEFAULT field set.
     */
    <S, D> D map(S sourceObject, Class<D> destinationClass);

    /**
     * Create and return a new instance of type {@code D} mapped from {@code sourceObject},
     * applying field filtering described by {@code fields}.
     */
    <S, D> D map(S sourceObject, Class<D> destinationClass, String fields);

    /**
     * Create and return a new instance of type {@code D} mapped from {@code sourceObject},
     * restricting output to the given {@code fields} set.
     */
    <S, D> D map(S sourceObject, Class<D> destinationClass, Set<String> fields);

    // =========================================================================
    // Single-object mapping – onto existing instance
    // =========================================================================

    /**
     * Map properties of {@code sourceObject} onto an existing {@code destinationObject}.
     * Uses DEFAULT field set; does not overwrite with nulls.
     */
    <S, D> void map(S sourceObject, D destinationObject);

    /**
     * Map properties of {@code sourceObject} onto {@code destinationObject},
     * controlling null handling via {@code mapNulls}.
     */
    <S, D> void map(S sourceObject, D destinationObject, boolean mapNulls);

    /**
     * Map selected fields of {@code sourceObject} onto {@code destinationObject}.
     */
    <S, D> void map(S sourceObject, D destinationObject, String fields);

    /**
     * Map selected fields with explicit null handling.
     */
    <S, D> void map(S sourceObject, D destinationObject, String fields, boolean mapNulls);

    // =========================================================================
    // Generic mapping (parameterized types, e.g. ProductSearchPageData<S,P>)
    // =========================================================================

    /**
     * Map with explicit type arguments for parameterized source/destination types.
     *
     * <p>Use this when the objects are generic and the mapper must know the actual
     * type parameters, e.g. {@code ProductSearchPageData<SearchStateData, ProductData>}.</p>
     *
     * @param sourceObject              the object to map from
     * @param destObject                the object to map onto
     * @param sourceActualTypeArguments actual type parameters of the source generic class
     * @param destActualTypeArguments   actual type parameters of the dest generic class
     * @param fields                    field descriptor string
     * @param destTypeVariableMap       mapping from type variable names to concrete classes in dest
     */
    <S, D> void mapGeneric(
            S sourceObject,
            D destObject,
            Type[] sourceActualTypeArguments,
            Type[] destActualTypeArguments,
            String fields,
            Map<String, Class<?>> destTypeVariableMap);

    // =========================================================================
    // Collection mapping
    // =========================================================================

    /**
     * Map an {@link Iterable} of {@code S} into a new {@link List} of {@code D}.
     */
    <S, D> List<D> mapAsList(Iterable<S> source, Class<D> destinationClass, String fields);

    /**
     * Map an {@link Iterable} of {@code S} into a new {@link Set} of {@code D}.
     */
    <S, D> Set<D> mapAsSet(Iterable<S> source, Class<D> destinationClass, String fields);

    /**
     * Map an {@link Iterable} of {@code S} into an existing {@link Collection} of {@code D}.
     */
    <S, D> void mapAsCollection(
            Iterable<S> source,
            Collection<D> destination,
            Class<D> destinationClass,
            String fields);
}
