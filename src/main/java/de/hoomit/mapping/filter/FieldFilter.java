package de.hoomit.mapping.filter;

import de.hoomit.mapping.annotation.WsDTOMapping;
import de.hoomit.mapping.context.MappingContext;

/**
 * Determines whether a specific field should be included or excluded from
 * the mapping output.
 *
 * <p>Filters are consulted per field <em>after</em> the field-set check.
 * Register implementations by annotating them with
 * {@link WsDTOMapping}.</p>
 *
 * <p>Example – exclude fields with empty collections:
 * <pre>
 *   {@literal @}WsDTOMapping
 *   public class EmptyCollectionFilter implements FieldFilter {
 *       {@literal @}Override
 *       public boolean isApplicable(Class&lt;?&gt; sourceType, Class&lt;?&gt; destType) {
 *           return true;
 *       }
 *
 *       {@literal @}Override
 *       public boolean include(String fieldName, Object value, MappingContext ctx) {
 *           if (value instanceof Collection) {
 *               return !((Collection&lt;?&gt;) value).isEmpty();
 *           }
 *           return true;
 *       }
 *   }
 * </pre>
 * </p>
 */
public interface FieldFilter {

    /**
     * Whether this filter is relevant for the given source/destination type pair.
     *
     * @param sourceType source class
     * @param destType   destination class
     * @return {@code true} if this filter should be consulted for this type pair
     */
    boolean isApplicable(Class<?> sourceType, Class<?> destType);

    /**
     * Decide whether a field value should be written to the destination.
     *
     * @param fieldName field name on the destination class
     * @param value     value from the source object (may be null)
     * @param context   active mapping context
     * @return {@code true} if the field should be included
     */
    boolean include(String fieldName, Object value, MappingContext context);
}
