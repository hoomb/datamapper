package de.hoomit.mapping.fieldset;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a single custom-named field set for use inside {@link FieldSetDefinition#custom()}.
 *
 * <p>Example:
 * <pre>
 *   {@literal @}FieldSetDefinition(
 *       basic    = {"code", "name"},
 *       defaults = {"code", "name", "price"},
 *       full     = {},
 *       custom   = {
 *           {@literal @}NamedFieldSet(name = "SEARCH",   fields = {"code", "name", "categoryNames"}),
 *           {@literal @}NamedFieldSet(name = "CHECKOUT", fields = {"code", "name", "price", "stockLevel"}),
 *           {@literal @}NamedFieldSet(name = "EXPORT",   fields = {}) // empty = ALL fields, same as FULL
 *       }
 *   )
 *   public class ProductWsDTO { ... }
 * </pre>
 * </p>
 *
 * <p>Names are case-insensitive at resolution time; they are normalised to upper-case
 * internally. Names must not clash with the three reserved levels:
 * {@code BASIC}, {@code DEFAULT}, {@code FULL}.</p>
 *
 * <p>An empty {@code fields} array is a sentinel meaning "include ALL declared fields
 * of the DTO class", identical to the behaviour of {@link FieldSetDefinition#full()}.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})          // only legal inside @FieldSetDefinition — not directly on a class
@Documented
public @interface NamedFieldSet {

    /**
     * The unique name of this field set (case-insensitive).
     * Must not be {@code "BASIC"}, {@code "DEFAULT"}, or {@code "FULL"}.
     */
    String name();

    /**
     * The field names included in this set.
     * An empty array means "include ALL fields of the annotated DTO class".
     */
    String[] fields() default {};
}
