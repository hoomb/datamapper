package de.hoomit.mapping.fieldset;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares which fields belong to each field set on a DTO class.
 *
 * <p>Three built-in levels are always available:
 * <ul>
 *   <li>{@link #basic()}    – minimal fields, e.g. identifier + display name only</li>
 *   <li>{@link #defaults()} – standard API payload</li>
 *   <li>{@link #full()}     – every field; an empty array is a sentinel for "all fields"</li>
 * </ul>
 * </p>
 *
 * <p>Any number of <b>custom named sets</b> can be added via {@link #custom()}.
 * Custom names are case-insensitive and must not shadow {@code BASIC}, {@code DEFAULT},
 * or {@code FULL}.
 * </p>
 *
 * <p>Example:
 * <pre>
 *   {@literal @}FieldSetDefinition(
 *       basic    = {"code", "name"},
 *       defaults = {"code", "name", "price", "available", "stockLevel"},
 *       full     = {},    // empty = ALL fields
 *       custom   = {
 *           {@literal @}NamedFieldSet(name = "SEARCH",   fields = {"code", "name", "categoryNames"}),
 *           {@literal @}NamedFieldSet(name = "CHECKOUT", fields = {"code", "name", "price", "stockLevel"}),
 *           {@literal @}NamedFieldSet(name = "ADMIN",    fields = {})   // all fields
 *       }
 *   )
 *   public class ProductWsDTO { ... }
 * </pre>
 * </p>
 *
 * <p>Custom sets can also be combined with extra fields in the descriptor string,
 * just like the built-in levels:
 * <pre>
 *   dataMapper.map(product, ProductWsDTO.class, "SEARCH,description");
 * </pre>
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface FieldSetDefinition {

    /**
     * Fields included at the {@code BASIC} level.
     */
    String[] basic() default {};

    /**
     * Fields included at the {@code DEFAULT} level.
     */
    String[] defaults() default {};

    /**
     * Fields included at the {@code FULL} level.
     * An empty array means "include ALL declared fields of this class".
     */
    String[] full() default {};

    /**
     * Zero or more custom named field sets.
     * Each entry defines an arbitrary name and its associated field list.
     * Names are case-insensitive; an empty {@code fields} array means "all fields".
     *
     * @see NamedFieldSet
     */
    NamedFieldSet[] custom() default {};
}
