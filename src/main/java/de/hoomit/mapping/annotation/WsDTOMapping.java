package de.hoomit.mapping.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for Mapper, Converter, and Filter beans that should be
 * auto-discovered and registered by the DefaultDataMapper.
 *
 * <p>Equivalent to Hybris {@code @WsDTOMapping}.</p>
 *
 * <p>Usage:
 * <pre>
 *   {@literal @}WsDTOMapping
 *   public class ProductDataMapper implements CustomMapper&lt;ProductData, ProductWsDTO&gt; { ... }
 * </pre>
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface WsDTOMapping {
}
