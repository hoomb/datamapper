package de.hoomit.mapping.test.domain;

import de.hoomit.mapping.fieldset.FieldSetDefinition;

import java.util.List;

/**
 * Web Service DTO for a product.
 * <p>
 * Field set levels:
 * - BASIC:   code, name
 * - DEFAULT: code, name, price, available
 * - FULL:    (all fields, empty = sentinel)
 */
@FieldSetDefinition(
        basic = {"code", "name"},
        defaults = {"code", "name", "price", "available", "stockLevel"},
        full = {} // empty = include ALL fields
)
public class ProductWsDTO {
    private String code;
    private String name;
    private String description;
    private String price;          // formatted price string (custom converter)
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
