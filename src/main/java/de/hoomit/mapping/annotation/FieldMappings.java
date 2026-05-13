package de.hoomit.mapping.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container for repeated {@link FieldMapping} declarations on a single field.
 * Users normally don't write this annotation directly — the compiler wraps
 * repeated {@code @FieldMapping} occurrences automatically.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FieldMappings {
    FieldMapping[] value();
}