package de.hoomit.mapping.test.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Simulates a service-layer Data object (e.g. from a facade).
 */
public class ProductData {
    private String code;
    private String name;
    private String description;
    private BigDecimal basePrice;
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

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(final BigDecimal basePrice) {
        this.basePrice = basePrice;
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
}
