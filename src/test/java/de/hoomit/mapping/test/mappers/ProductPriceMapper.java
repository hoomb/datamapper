package de.hoomit.mapping.test.mappers;

import de.hoomit.mapping.annotation.WsDTOMapping;
import de.hoomit.mapping.context.MappingContext;
import de.hoomit.mapping.mapper.CustomMapper;
import de.hoomit.mapping.test.domain.ProductData;
import de.hoomit.mapping.test.domain.ProductWsDTO;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Post-processes ProductData → ProductWsDTO to format the price field.
 * The reflective engine cannot map BigDecimal → String automatically,
 * so this CustomMapper handles the conversion.
 */
@WsDTOMapping
public class ProductPriceMapper implements CustomMapper<ProductData, ProductWsDTO> {

    @Override
    public void mapAtoB(final ProductData source, final ProductWsDTO dest, final MappingContext ctx) {
        if (ctx.includes("price") && source.getBasePrice() != null) {
            final BigDecimal price = source.getBasePrice();
            final NumberFormat eurFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY);
            dest.setPrice(eurFormat.format(price));
        }
    }

    @Override
    public void mapBtoA(final ProductWsDTO source, final ProductData dest, final MappingContext ctx) {
        // Parse back if needed – simplified
        if (source.getPrice() != null) {
            final String[] parts = source.getPrice().split(" ");
            if (parts.length == 2) {
                try {
                    dest.setBasePrice(new BigDecimal(parts[1]));
                    dest.setCurrencyIso(parts[0]);
                } catch (final NumberFormatException ignored) {
                }
            }
        }
    }
}
