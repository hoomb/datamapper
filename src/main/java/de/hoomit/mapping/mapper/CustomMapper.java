package de.hoomit.mapping.mapper;

import de.hoomit.mapping.annotation.WsDTOMapping;
import de.hoomit.mapping.context.MappingContext;

/**
 * Performs custom field-level mapping <em>after</em> the default reflective mapping
 * has already run. This lets you handle fields that don't follow the standard
 * naming convention, apply type conversions, or add computed properties.
 *
 * <p>Register implementations by annotating them with
 * {@link WsDTOMapping}.</p>
 *
 * <p>Example:
 * <pre>
 *   {@literal @}WsDTOMapping
 *   public class ProductCustomMapper implements CustomMapper&lt;ProductData, ProductWsDTO&gt; {
 *       {@literal @}Override
 *       public void mapAtoB(ProductData a, ProductWsDTO b, MappingContext ctx) {
 *           // custom A→B logic
 *           b.setDisplayName(a.getName().toUpperCase());
 *       }
 *
 *       {@literal @}Override
 *       public void mapBtoA(ProductWsDTO b, ProductData a, MappingContext ctx) {
 *           // reverse mapping (optional)
 *           a.setName(b.getDisplayName().toLowerCase());
 *       }
 *   }
 * </pre>
 * </p>
 *
 * @param <A> first type (typically the Service/Data layer object)
 * @param <B> second type (typically the WsDTO)
 */
public interface CustomMapper<A, B> {

    /**
     * Map fields from {@code a} onto {@code b}.
     *
     * @param a       source object
     * @param b       partially populated destination object
     * @param context active mapping context
     */
    void mapAtoB(A a, B b, MappingContext context);

    /**
     * Map fields from {@code b} back onto {@code a} (reverse direction).
     * Default is a no-op; override when bidirectional mapping is needed.
     *
     * @param b       source object
     * @param a       partially populated destination object
     * @param context active mapping context
     */
    default void mapBtoA(B b, A a, MappingContext context) {
        // no-op by default
    }
}
