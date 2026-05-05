package de.hoomit.mapping.test.domain;

import de.hoomit.mapping.fieldset.FieldSetDefinition;
import de.hoomit.mapping.fieldset.NamedFieldSet;

import java.util.List;

import static de.hoomit.mapping.fieldset.FieldSets.BASIC;

/**
 * Web Service DTO for a product.
 *
 * <p>This DTO demonstrates <b>cumulative field-set definitions</b>: higher-level
 * sets reference lower-level ones via the {@code FieldSets.BASIC} / {@code DEFAULT}
 * constants, eliminating duplicate field listings.</p>
 *
 * <ul>
 *   <li>BASIC    : code, name</li>
 *   <li>DEFAULT  : BASIC + price + available + stockLevel</li>
 *   <li>FULL     : (empty = ALL fields, sentinel)</li>
 *   <li>SEARCH   : BASIC + categoryNames                          (custom)</li>
 *   <li>CHECKOUT : BASIC + price + stockLevel                     (custom)</li>
 *   <li>MOBILE   : SEARCH + stockLevel  (transitive: BASIC → SEARCH → MOBILE)</li>
 *   <li>ADMIN    : (empty = ALL fields)                           (custom)</li>
 * </ul>
 */
@FieldSetDefinition(
        basic = {"code", "name"},
        defaults = {BASIC, "price", "available", "stockLevel"},
        full = {},
        custom = {
                @NamedFieldSet(name = "SEARCH", fields = {BASIC, "categoryNames"}),
                @NamedFieldSet(name = "CHECKOUT", fields = {BASIC, "price", "stockLevel"}),
                @NamedFieldSet(name = "MOBILE", fields = {"@SEARCH", "stockLevel"}),
                @NamedFieldSet(name = "ADMIN", fields = {})
        }
)
public class ProductWsDTO {
    private String code;
    private String name;
    private String description;
    private String price;
    private String currencyIso;
    private int stockLevel;
    private boolean available;
    private List<String> categoryNames;
    private String manufacturerName;
    private String imageUrl;

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(final String price) {
        this.price = price;
    }

    public String getCurrencyIso() {
        return currencyIso;
    }

    public void setCurrencyIso(final String currencyIso) {
        this.currencyIso = currencyIso;
    }

    public int getStockLevel() {
        return stockLevel;
    }

    public void setStockLevel(final int stockLevel) {
        this.stockLevel = stockLevel;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(final boolean available) {
        this.available = available;
    }

    public List<String> getCategoryNames() {
        return categoryNames;
    }

    public void setCategoryNames(final List<String> categoryNames) {
        this.categoryNames = categoryNames;
    }

    public String getManufacturerName() {
        return manufacturerName;
    }

    public void setManufacturerName(final String manufacturerName) {
        this.manufacturerName = manufacturerName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(final String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Override
    public String toString() {
        return "ProductWsDTO{code='" + code + "', name='" + name
                + "', price='" + price + "', available=" + available
                + ", stock=" + stockLevel + "}";
    }
}
