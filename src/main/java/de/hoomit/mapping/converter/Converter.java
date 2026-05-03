package de.hoomit.mapping.converter;

import de.hoomit.mapping.annotation.WsDTOMapping;
import de.hoomit.mapping.context.MappingContext;

/**
 * Converts a source object of type {@code S} into a destination object of type {@code D}.
 *
 * <p>Converters are responsible for the <em>full</em> construction and population of
 * the destination object. Register implementations by annotating them with
 * {@link WsDTOMapping} so they are discovered automatically.
 * </p>
 *
 * <p>Example:
 * <pre>
 *   {@literal @}WsDTOMapping
 *   public class PriceConverter implements Converter&lt;PriceData, PriceWsDTO&gt; {
 *       {@literal @}Override
 *       public PriceWsDTO convert(PriceData source, MappingContext ctx) {
 *           PriceWsDTO dto = new PriceWsDTO();
 *           dto.setValue(source.getValue().toPlainString());
 *           dto.setCurrencyIso(source.getCurrencyIso());
 *           return dto;
 *       }
 *   }
 * </pre>
 * </p>
 *
 * @param <S> source type
 * @param <D> destination type
 */
public interface Converter<S, D> {

    /**
     * Convert {@code source} into a new instance of {@code D}.
     *
     * @param source  source object (never null when called by the framework)
     * @param context active mapping context carrying field-set and null-mapping info
     * @return newly created destination object
     */
    D convert(S source, MappingContext context);
}
