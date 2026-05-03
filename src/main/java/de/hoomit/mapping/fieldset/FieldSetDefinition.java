package de.hoomit.mapping.fieldset;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares which fields belong to each {@link FieldLevel} on a DTO class.
 *
 * <p>Example:
 * <pre>
 *   {@literal @}FieldSetDefinition(
 *       basic   = {"id", "name"},
 *       defaults = {"id", "name", "price", "stock"},
 *       full    = {}   // empty means ALL fields
 *   )
 *   public class ProductWsDTO { ... }
 * </pre>
 * </p>
 *
 * <p>An empty array for {@code full} is a sentinel meaning "include everything".</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface FieldSetDefinition {

    /**
     * Fields included at {@link FieldLevel#BASIC}.
     */
    String[] basic() default {};

    /**
     * Fields included at {@link FieldLevel#DEFAULT}.
     */
    String[] defaults() default {};

    /**
     * Fields included at {@link FieldLevel#FULL}.
     * An empty array means "all fields of the class".
     */
    String[] full() default {};
}
