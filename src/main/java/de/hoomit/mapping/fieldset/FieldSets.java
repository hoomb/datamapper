package de.hoomit.mapping.fieldset;

/**
 * Compile-time constants used inside {@link FieldSetDefinition} arrays to
 * express <b>cumulative field-set definitions</b>.
 *
 * <p>An entry starting with {@code @} inside a {@code basic}, {@code defaults},
 * {@code full}, or {@link NamedFieldSet#fields()} array is a <em>reference</em>
 * to another field set. The {@link FieldSetBuilder} expands these references
 * transitively at cache-build time, so a higher-level set inherits all fields
 * of the sets it references.</p>
 *
 * <p>Recommended usage with a static import:
 * <pre>
 *   import static de.hoomit.mapping.fieldset.FieldSets.*;
 *
 *   {@literal @}FieldSetDefinition(
 *       basic    = {"code", "name"},
 *       defaults = {BASIC, "price", "available", "stockLevel"},
 *       full     = {DEFAULT, "description", "manufacturerName", "imageUrl"},
 *       custom   = {
 *           {@literal @}NamedFieldSet(name = "SEARCH",   fields = {BASIC, "categoryNames"}),
 *           {@literal @}NamedFieldSet(name = "CHECKOUT", fields = {BASIC, "price", "stockLevel"})
 *       }
 *   )
 *   public class ProductWsDTO { ... }
 * </pre>
 * </p>
 *
 * <p>To reference a custom named set, use {@link #ref(String)}:
 * <pre>
 *   custom = {
 *       {@literal @}NamedFieldSet(name = "SEARCH", fields = {BASIC, "categoryNames"}),
 *       {@literal @}NamedFieldSet(name = "MOBILE", fields = {ref("SEARCH"), "stockLevel"})
 *   }
 * </pre>
 * </p>
 *
 * <p><b>Rules:</b>
 * <ul>
 *   <li>References are resolved transitively (FULL → DEFAULT → BASIC works).</li>
 *   <li>References are case-insensitive.</li>
 *   <li>Circular references throw {@link IllegalStateException} on first resolution.</li>
 *   <li>Unknown references throw {@link IllegalStateException} on first resolution.</li>
 * </ul>
 * </p>
 */
public final class FieldSets {

    /**
     * Reference to the {@code BASIC} field set.
     */
    public static final String BASIC = "@BASIC";

    /**
     * Reference to the {@code DEFAULT} field set.
     */
    public static final String DEFAULT = "@DEFAULT";

    /**
     * Reference to the {@code FULL} field set.
     */
    public static final String FULL = "@FULL";

    /**
     * Build a reference to a custom named field set.
     *
     * @param name name of the custom set declared in {@link FieldSetDefinition#custom()}
     * @return reference token usable inside any {@code fields} array
     */
    public static String ref(final String name) {
        return "@" + name.toUpperCase();
    }

    private FieldSets() {
    }
}
