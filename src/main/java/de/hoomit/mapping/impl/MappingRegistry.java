package de.hoomit.mapping.impl;

import de.hoomit.mapping.annotation.WsDTOMapping;
import de.hoomit.mapping.converter.Converter;
import de.hoomit.mapping.filter.FieldFilter;
import de.hoomit.mapping.mapper.CustomMapper;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores all registered {@link Converter}, {@link CustomMapper}, and {@link FieldFilter}
 * beans and provides fast lookup by source+destination type pair.
 *
 * <p>Beans annotated with {@link WsDTOMapping} are auto-discovered when
 * {@link DefaultDataMapper} is initialised.</p>
 */
public class MappingRegistry {

    /**
     * Key = (sourceClass, destClass)
     */
    private final Map<TypePair, Converter<?, ?>> converters = new ConcurrentHashMap<>();
    private final Map<TypePair, CustomMapper<?, ?>> mappers = new ConcurrentHashMap<>();
    private final List<FieldFilter> filters = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    public void registerConverter(final Converter<?, ?> converter) {
        final TypePair pair = resolveTypePair(converter.getClass(), Converter.class);
        if (pair != null) {
            converters.put(pair, converter);
        }
    }

    public void registerMapper(final CustomMapper<?, ?> mapper) {
        final TypePair pair = resolveTypePair(mapper.getClass(), CustomMapper.class);
        if (pair != null) {
            mappers.put(pair, mapper);
            // Also register reverse pair (B→A)
            mappers.putIfAbsent(new TypePair(pair.destClass, pair.sourceClass), mapper);
        }
    }

    public void registerFilter(final FieldFilter filter) {
        filters.add(filter);
    }

    // -------------------------------------------------------------------------
    // Lookup
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public <S, D> Optional<Converter<S, D>> findConverter(final Class<S> src, final Class<D> dest) {
        return Optional.ofNullable((Converter<S, D>) converters.get(new TypePair(src, dest)));
    }

    @SuppressWarnings("unchecked")
    public <A, B> Optional<CustomMapper<A, B>> findMapper(final Class<A> src, final Class<B> dest) {
        return Optional.ofNullable((CustomMapper<A, B>) mappers.get(new TypePair(src, dest)));
    }

    public List<FieldFilter> getFilters() {
        return Collections.unmodifiableList(filters);
    }

    // -------------------------------------------------------------------------
    // Generic type introspection helpers
    // -------------------------------------------------------------------------

    private TypePair resolveTypePair(final Class<?> implClass, final Class<?> targetInterface) {
        for (final Type iface : implClass.getGenericInterfaces()) {
            if (!(iface instanceof ParameterizedType pt)) continue;
            if (!(pt.getRawType() instanceof Class<?> raw)) continue;
            if (!targetInterface.isAssignableFrom(raw)) continue;

            final Type[] args = pt.getActualTypeArguments();
            if (args.length >= 2
                    && args[0] instanceof Class<?> src
                    && args[1] instanceof Class<?> dest) {
                return new TypePair(src, dest);
            }
        }

        // Check superclass for indirect implementations
        final Class<?> superClass = implClass.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            return resolveTypePair(superClass, targetInterface);
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // TypePair key
    // -------------------------------------------------------------------------

    record TypePair(Class<?> sourceClass, Class<?> destClass) {
        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (!(o instanceof TypePair tp)) return false;
            return sourceClass == tp.sourceClass && destClass == tp.destClass;
        }

        @Override
        public int hashCode() {
            return 31 * System.identityHashCode(sourceClass)
                    + System.identityHashCode(destClass);
        }
    }
}
