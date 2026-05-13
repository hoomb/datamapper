package de.hoomit.mapping.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that the annotated field corresponds to a differently-named field
 * on a paired class. Used by the framework to bridge naming differences between
 * source and destination types (e.g. DTO {@code uid} ↔ domain {@code login}).
 *
 * <p>This annotation is {@link Repeatable}: multiple {@code @FieldMapping}
 * declarations on the same field are wrapped in {@link FieldMappings} by the
 * compiler. Read both forms uniformly via
 * {@code field.getAnnotationsByType(FieldMapping.class)}.
 *
 * <p>The annotation is direction-agnostic. Whether the annotated class is the
 * source or the destination of a mapping call, the framework consults the
 * declaration to find the corresponding name on the other side.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Repeatable(FieldMappings.class)
public @interface FieldMapping {

    /**
     * The class on the other side of the mapping boundary.
     */
    Class<?> pairedClass();

    /**
     * The field name on the paired class that this field corresponds to.
     */
    String pairedField();
}