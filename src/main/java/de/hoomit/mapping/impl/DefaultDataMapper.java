package de.hoomit.mapping.impl;

import de.hoomit.mapping.DataMapper;
import de.hoomit.mapping.annotation.WsDTOMapping;
import de.hoomit.mapping.context.MappingContext;
import de.hoomit.mapping.converter.Converter;
import de.hoomit.mapping.fieldset.FieldSetBuilder;
import de.hoomit.mapping.filter.FieldFilter;
import de.hoomit.mapping.mapper.CustomMapper;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Default implementation of {@link DataMapper}.
 *
 * <p>Mapping pipeline for a single object:
 * <ol>
 *   <li>If a {@link Converter} is registered for (source, dest), delegate entirely.</li>
 *   <li>Otherwise: run the {@link ReflectionMappingEngine} to copy matching fields.</li>
 *   <li>If a {@link CustomMapper} is registered, call {@code mapAtoB} as a post-processor.</li>
 * </ol>
 * </p>
 *
 * <h2>Auto-discovery</h2>
 * <p>Beans annotated with {@link WsDTOMapping} can be registered via
 * {@link #registerBeans(Iterable)} (Spring integration passes its ApplicationContext beans).
 * Alternatively, register directly with {@link #addConverter}/{@link #addMapper}/{@link #addFilter}.</p>
 *
 * <h2>Thread safety</h2>
 * <p>Safe for concurrent use once all beans are registered.</p>
 */
public class DefaultDataMapper implements DataMapper {

    private final MappingRegistry registry = new MappingRegistry();
    private final ReflectionMappingEngine engine = new ReflectionMappingEngine();

    // =========================================================================
    // Bean Registration
    // =========================================================================

    /**
     * Auto-register all beans in the iterable that are annotated with {@link WsDTOMapping}.
     */
    public void registerBeans(final Iterable<?> beans) {
        for (final Object bean : beans) {
            if (bean.getClass().isAnnotationPresent(WsDTOMapping.class)) {
                switch (bean) {
                    case Converter<?, ?> c -> registry.registerConverter(c);
                    case CustomMapper<?, ?> m -> registry.registerMapper(m);
                    case FieldFilter f -> registry.registerFilter(f);
                    default -> {
                    }
                }
            }
        }
    }

    public void addConverter(Converter<?, ?> converter) {
        registry.registerConverter(converter);
    }

    public void addMapper(CustomMapper<?, ?> mapper) {
        registry.registerMapper(mapper);
    }

    public void addFilter(FieldFilter filter) {
        registry.registerFilter(filter);
    }

    // =========================================================================
    // DataMapper – create & return
    // =========================================================================

    @Override
    public <S, D> D map(final S source, final Class<D> destClass) {
        return map(source, destClass, (String) null);
    }

    @Override
    public <S, D> D map(final S source, final Class<D> destClass, final String fields) {
        Objects.requireNonNull(source, "sourceObject must not be null");
        Objects.requireNonNull(destClass, "destinationClass must not be null");

        final Set<String> resolved = FieldSetBuilder.resolve(destClass, fields);
        final MappingContext ctx = MappingContext.builder()
                .fieldSetName(fields != null ? fields : "DEFAULT")
                .resolvedFields(resolved)
                .build();

        return doMap(source, destClass, ctx);
    }

    @Override
    public <S, D> D map(final S source, final Class<D> destClass, final Set<String> fields) {
        Objects.requireNonNull(source, "sourceObject must not be null");
        Objects.requireNonNull(destClass, "destinationClass must not be null");

        final MappingContext ctx = MappingContext.builder()
                .fieldSetName("EXPLICIT")
                .resolvedFields(fields != null ? fields : Collections.emptySet())
                .build();

        return doMap(source, destClass, ctx);
    }

    // =========================================================================
    // DataMapper – onto existing instance
    // =========================================================================

    @Override
    public <S, D> void map(final S source, final D dest) {
        map(source, dest, (String) null, false);
    }

    @Override
    public <S, D> void map(final S source, final D dest, final boolean mapNulls) {
        map(source, dest, (String) null, mapNulls);
    }

    @Override
    public <S, D> void map(final S source, final D dest, final String fields) {
        map(source, dest, fields, false);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S, D> void map(final S source, final D dest, final String fields, final boolean mapNulls) {
        Objects.requireNonNull(source, "sourceObject must not be null");
        Objects.requireNonNull(dest, "destinationObject must not be null");

        final Class<D> destClass = (Class<D>) dest.getClass();
        final Set<String> resolved = FieldSetBuilder.resolve(destClass, fields);
        final MappingContext ctx = MappingContext.builder()
                .fieldSetName(fields != null ? fields : "DEFAULT")
                .mapNulls(mapNulls)
                .resolvedFields(resolved)
                .build();

        doMapOnto(source, dest, ctx);
    }

    // =========================================================================
    // DataMapper – generic (parameterized types)
    // =========================================================================

    @Override
    @SuppressWarnings("unchecked")
    public <S, D> void mapGeneric(
            final S source, final D dest,
            final Type[] sourceTypeArgs, final Type[] destTypeArgs,
            final String fields, final Map<String, Class<?>> destTypeVariableMap) {

        Objects.requireNonNull(source, "sourceObject must not be null");
        Objects.requireNonNull(dest, "destinationObject must not be null");

        final Class<D> destClass = (Class<D>) dest.getClass();
        final Set<String> resolved = FieldSetBuilder.resolve(destClass, fields);
        final MappingContext ctx = MappingContext.builder()
                .fieldSetName(fields != null ? fields : "DEFAULT")
                .resolvedFields(resolved)
                .extra("sourceTypeArgs", sourceTypeArgs)
                .extra("destTypeArgs", destTypeArgs)
                .extra("destTypeVariableMap", destTypeVariableMap)
                .build();

        doMapOnto(source, dest, ctx);
    }

    // =========================================================================
    // DataMapper – collection mapping
    // =========================================================================

    @Override
    public <S, D> List<D> mapAsList(final Iterable<S> source, final Class<D> destClass, final String fields) {
        return StreamSupport.stream(source.spliterator(), false)
                .map(item -> map(item, destClass, fields))
                .collect(Collectors.toList());
    }

    @Override
    public <S, D> Set<D> mapAsSet(final Iterable<S> source, final Class<D> destClass, final String fields) {
        return StreamSupport.stream(source.spliterator(), false)
                .map(item -> map(item, destClass, fields))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public <S, D> void mapAsCollection(
            final Iterable<S> source, final Collection<D> destination,
            final Class<D> destClass, final String fields) {
        for (final S item : source) {
            destination.add(map(item, destClass, fields));
        }
    }

    // =========================================================================
    // Internal mapping pipeline
    // =========================================================================

    @SuppressWarnings("unchecked")
    private <S, D> D doMap(final S source, final Class<D> destClass, final MappingContext ctx) {
        // 1. Check for a registered Converter
        final Optional<Converter<S, D>> converter = registry.findConverter((Class<S>) source.getClass(), destClass);
        if (converter.isPresent()) {
            return converter.get().convert(source, ctx);
        }

        // 2. Instantiate destination
        final D dest = instantiate(destClass);

        // 3. Reflection mapping
        engine.map(source, dest, ctx, registry.getFilters());

        // 4. Post-process with CustomMapper if registered
        final Optional<CustomMapper<S, D>> mapper = registry.findMapper((Class<S>) source.getClass(), destClass);
        mapper.ifPresent(m -> m.mapAtoB(source, dest, ctx));

        return dest;
    }

    @SuppressWarnings("unchecked")
    private <S, D> void doMapOnto(final S source, final D dest, final MappingContext ctx) {
        final Class<S> srcClass = (Class<S>) source.getClass();
        final Class<D> destClass = (Class<D>) dest.getClass();

        // If a Converter exists, it creates a NEW object – we can copy its fields onto dest
        final Optional<Converter<S, D>> converter = registry.findConverter(srcClass, destClass);
        if (converter.isPresent()) {
            final D tmp = converter.get().convert(source, ctx);
            engine.map(tmp, dest, MappingContext.full(), Collections.emptyList());
            return;
        }

        // Reflection mapping
        engine.map(source, dest, ctx, registry.getFilters());

        // Post-process
        registry.findMapper(srcClass, destClass).ifPresent(m -> m.mapAtoB(source, dest, ctx));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private <D> D instantiate(final Class<D> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (final Exception e) {
            throw new MappingException(
                    "Cannot instantiate destination class " + clazz.getName()
                            + ". Ensure it has a public no-arg constructor.", e);
        }
    }
}
