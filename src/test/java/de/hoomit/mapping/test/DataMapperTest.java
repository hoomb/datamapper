package de.hoomit.mapping.test;

import de.hoomit.mapping.mapper.DefaultDataMapper;
import de.hoomit.mapping.test.domain.ProductData;
import de.hoomit.mapping.test.domain.ProductWsDTO;
import de.hoomit.mapping.test.mappers.NullCollectionFilter;
import de.hoomit.mapping.test.mappers.ProductPriceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DefaultDataMapper covering:
 * - Basic field-set level filtering (BASIC / DEFAULT / FULL)
 * - Explicit field string
 * - Custom mapper post-processing
 * - Null-handling (mapNulls flag)
 * - Collection mapping (mapAsList / mapAsSet / mapAsCollection)
 * - FieldFilter
 * - Map onto existing object
 * - Set<String> field selection
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DataMapperTest {

    private DefaultDataMapper dataMapper;

    @BeforeEach
    void setUp() {
        dataMapper = new DefaultDataMapper();
        // Auto-register all @WsDTOMapping beans
        dataMapper.registerBeans(List.of(
                new ProductPriceMapper(),
                new NullCollectionFilter()
        ));
    }

    // =========================================================================
    // Test fixtures
    // =========================================================================

    private ProductData fullProduct() {
        ProductData p = new ProductData();
        p.setCode("P001");
        p.setName("Laptop Pro");
        p.setDescription("High-end laptop for professionals");
        p.setBasePrice(new BigDecimal("1299.99"));
        p.setCurrencyIso("EUR");
        p.setStockLevel(42);
        p.setAvailable(true);
        p.setCategoryNames(List.of("Electronics", "Computers"));
        p.setManufacturerName("TechCorp");
        p.setImageUrl("https://example.com/images/P001.jpg");
        return p;
    }

    // =========================================================================
    // Field-set level tests
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("BASIC field set: only code and name are mapped")
    void testBasicFieldSet() {
        ProductWsDTO dto = dataMapper.map(fullProduct(), ProductWsDTO.class, "BASIC");

        assertEquals("P001", dto.getCode());
        assertEquals("Laptop Pro", dto.getName());
        assertNull(dto.getDescription(), "description should NOT be in BASIC set");
        assertNull(dto.getPrice(), "price should NOT be in BASIC set");
        assertEquals(0, dto.getStockLevel(), "stockLevel should NOT be in BASIC set");
    }

    @Test
    @Order(2)
    @DisplayName("DEFAULT field set: code, name, price, available, stockLevel")
    void testDefaultFieldSet() {
        ProductWsDTO dto = dataMapper.map(fullProduct(), ProductWsDTO.class, "DEFAULT");

        assertEquals("P001", dto.getCode());
        assertEquals("Laptop Pro", dto.getName());
        assertEquals(42, dto.getStockLevel());
        assertTrue(dto.isAvailable());
        assertNotNull(dto.getPrice(), "price should be mapped via CustomMapper");
        assertNull(dto.getDescription(), "description should NOT be in DEFAULT set");
    }

    @Test
    @Order(3)
    @DisplayName("FULL field set: all fields including description and manufacturer")
    void testFullFieldSet() {
        ProductWsDTO dto = dataMapper.map(fullProduct(), ProductWsDTO.class, "FULL");

        assertEquals("P001", dto.getCode());
        assertEquals("Laptop Pro", dto.getName());
        assertEquals("High-end laptop for professionals", dto.getDescription());
        assertEquals("TechCorp", dto.getManufacturerName());
        assertEquals("https://example.com/images/P001.jpg", dto.getImageUrl());
        assertNotNull(dto.getPrice());
        assertTrue(dto.isAvailable());
    }

    @Test
    @Order(4)
    @DisplayName("null fields string defaults to DEFAULT")
    void testNullFieldsDefaultsToDefault() {
        ProductWsDTO dto = dataMapper.map(fullProduct(), ProductWsDTO.class, (String) null);
        // Same as DEFAULT
        assertNotNull(dto.getCode());
        assertNull(dto.getDescription());
    }

    // =========================================================================
    // Explicit field selection
    // =========================================================================

    @Test
    @Order(5)
    @DisplayName("Explicit field list: 'code,description' only those two fields")
    void testExplicitFieldList() {
        ProductWsDTO dto = dataMapper.map(fullProduct(), ProductWsDTO.class, "code,description");

        assertEquals("P001", dto.getCode());
        assertEquals("High-end laptop for professionals", dto.getDescription());
        assertNull(dto.getName(), "name not in explicit list");
        assertEquals(0, dto.getStockLevel(), "stockLevel not in explicit list");
    }

    @Test
    @Order(6)
    @DisplayName("Mixed field string: 'DEFAULT,description' adds description to DEFAULT")
    void testMixedLevelPlusExtra() {
        ProductWsDTO dto = dataMapper.map(fullProduct(), ProductWsDTO.class, "DEFAULT,description");

        assertNotNull(dto.getCode());
        assertNotNull(dto.getPrice());
        assertEquals("High-end laptop for professionals", dto.getDescription());
    }

    @Test
    @Order(7)
    @DisplayName("Set<String> field selection")
    void testSetFieldSelection() {
        Set<String> fields = new LinkedHashSet<>(Arrays.asList("code", "name", "stockLevel"));
        ProductWsDTO dto = dataMapper.map(fullProduct(), ProductWsDTO.class, fields);

        assertEquals("P001", dto.getCode());
        assertEquals("Laptop Pro", dto.getName());
        assertEquals(42, dto.getStockLevel());
        assertNull(dto.getDescription());
    }

    // =========================================================================
    // CustomMapper post-processing
    // =========================================================================

    @Test
    @Order(8)
    @DisplayName("CustomMapper formats price as 'EUR 1299.99'")
    void testCustomMapperPriceFormatting() {
        final ProductData productData = fullProduct();
        final ProductWsDTO dto = dataMapper.map(productData, ProductWsDTO.class, "DEFAULT");
        final String expected = NumberFormat.getCurrencyInstance(Locale.GERMANY).format(productData.getBasePrice());

        assertEquals(expected, dto.getPrice());
    }

    @Test
    @Order(9)
    @DisplayName("Price field excluded from BASIC: CustomMapper is not called for it")
    void testCustomMapperSkippedForBasic() {
        ProductWsDTO dto = dataMapper.map(fullProduct(), ProductWsDTO.class, "BASIC");
        assertNull(dto.getPrice(), "price field excluded by BASIC field set");
    }

    // =========================================================================
    // Map onto existing object
    // =========================================================================

    @Test
    @Order(10)
    @DisplayName("map onto existing object: only updates matching fields")
    void testMapOntoExistingObject() {
        ProductWsDTO existing = new ProductWsDTO();
        existing.setCode("OLD");
        existing.setDescription("should stay");

        ProductData source = new ProductData();
        source.setCode("P002");
        source.setName("Mouse");
        source.setAvailable(true);
        source.setStockLevel(5);

        dataMapper.map(source, existing, "BASIC");

        assertEquals("P002", existing.getCode(), "code should be updated");
        assertEquals("Mouse", existing.getName(), "name should be updated");
        assertEquals("should stay", existing.getDescription(), "description untouched (not in BASIC)");
    }

    @Test
    @Order(11)
    @DisplayName("mapNulls=false: null source value does not overwrite dest field")
    void testMapNullsFalse() {
        ProductWsDTO dest = new ProductWsDTO();
        dest.setName("Original");

        ProductData source = new ProductData();
        source.setCode("X");
        source.setName(null);   // null!

        dataMapper.map(source, dest, "FULL", false);

        assertEquals("Original", dest.getName(), "name should NOT be overwritten by null");
        assertEquals("X", dest.getCode());
    }

    @Test
    @Order(12)
    @DisplayName("mapNulls=true: null source value DOES overwrite dest field")
    void testMapNullsTrue() {
        ProductWsDTO dest = new ProductWsDTO();
        dest.setName("Original");

        ProductData source = new ProductData();
        source.setCode("X");
        source.setName(null);

        dataMapper.map(source, dest, "FULL", true);

        assertNull(dest.getName(), "name SHOULD be overwritten by null when mapNulls=true");
    }

    // =========================================================================
    // Collection mapping
    // =========================================================================

    @Test
    @Order(13)
    @DisplayName("mapAsList: maps a list of ProductData to List<ProductWsDTO>")
    void testMapAsList() {
        ProductData p1 = new ProductData();
        p1.setCode("A");
        p1.setName("Alpha");
        ProductData p2 = new ProductData();
        p2.setCode("B");
        p2.setName("Beta");

        List<ProductWsDTO> dtos = dataMapper.mapAsList(List.of(p1, p2), ProductWsDTO.class, "BASIC");

        assertEquals(2, dtos.size());
        assertEquals("A", dtos.get(0).getCode());
        assertEquals("B", dtos.get(1).getCode());
    }

    @Test
    @Order(14)
    @DisplayName("mapAsSet: result is a Set with no duplicates")
    void testMapAsSet() {
        ProductData p1 = new ProductData();
        p1.setCode("A");
        ProductData p2 = new ProductData();
        p2.setCode("B");

        Set<ProductWsDTO> dtos = dataMapper.mapAsSet(List.of(p1, p2), ProductWsDTO.class, "BASIC");
        assertEquals(2, dtos.size());
    }

    @Test
    @Order(15)
    @DisplayName("mapAsCollection: appends into an existing collection")
    void testMapAsCollection() {
        ProductData p1 = new ProductData();
        p1.setCode("A");
        ProductData p2 = new ProductData();
        p2.setCode("B");

        List<ProductWsDTO> dest = new ArrayList<>();
        dest.add(new ProductWsDTO()); // pre-existing item
        dataMapper.mapAsCollection(List.of(p1, p2), dest, ProductWsDTO.class, "BASIC");

        assertEquals(3, dest.size(), "should have original + 2 new items");
        assertEquals("A", dest.get(1).getCode());
    }

    // =========================================================================
    // FieldFilter test
    // =========================================================================

    @Test
    @Order(16)
    @DisplayName("NullCollectionFilter: empty category list is excluded from output")
    void testNullCollectionFilter() {
        ProductData source = fullProduct();
        source.setCategoryNames(Collections.emptyList()); // empty list

        ProductWsDTO dto = dataMapper.map(source, ProductWsDTO.class, "FULL");

        assertNull(dto.getCategoryNames(),
                "Empty collection should be excluded by NullCollectionFilter");
    }

    @Test
    @Order(17)
    @DisplayName("NullCollectionFilter: non-empty list is included normally")
    void testNonEmptyCollectionNotFiltered() {
        ProductData source = fullProduct();
        // categoryNames = ["Electronics", "Computers"]

        ProductWsDTO dto = dataMapper.map(source, ProductWsDTO.class, "FULL");

        assertNotNull(dto.getCategoryNames());
        assertEquals(2, dto.getCategoryNames().size());
    }

    // =========================================================================
    // Custom named field sets
    // =========================================================================

    @Test
    @Order(19)
    @DisplayName("Custom 'SEARCH' set: code, name, categoryNames only")
    void testCustomSearchFieldSet() {
        final ProductWsDTO dto = dataMapper.map(fullProduct(), ProductWsDTO.class, "SEARCH");

        assertEquals("P001", dto.getCode());
        assertEquals("Laptop Pro", dto.getName());
        assertNotNull(dto.getCategoryNames(), "categoryNames is part of SEARCH set");
        assertEquals(2, dto.getCategoryNames().size());
        assertNull(dto.getDescription(), "description NOT in SEARCH set");
        assertNull(dto.getPrice(), "price NOT in SEARCH set");
        assertEquals(0, dto.getStockLevel(), "stockLevel NOT in SEARCH set");
    }

    @Test
    @Order(20)
    @DisplayName("Custom 'CHECKOUT' set: code, name, price, stockLevel only")
    void testCustomCheckoutFieldSet() {
        final ProductWsDTO dto = dataMapper.map(fullProduct(), ProductWsDTO.class, "CHECKOUT");

        assertEquals("P001", dto.getCode());
        assertEquals("Laptop Pro", dto.getName());
        assertEquals(42, dto.getStockLevel());
        assertNotNull(dto.getPrice(), "price should be in CHECKOUT set (via CustomMapper)");
        assertNull(dto.getDescription(), "description NOT in CHECKOUT set");
        assertNull(dto.getManufacturerName(), "manufacturerName NOT in CHECKOUT set");
        assertFalse(dto.isAvailable(), "available NOT in CHECKOUT set — stays false (default)");
    }

    @Test
    @Order(21)
    @DisplayName("Custom 'ADMIN' set (empty fields = all): behaves like FULL")
    void testCustomAdminFieldSet() {
        final ProductWsDTO dto = dataMapper.map(fullProduct(), ProductWsDTO.class, "ADMIN");

        assertEquals("P001", dto.getCode());
        assertEquals("Laptop Pro", dto.getName());
        assertEquals("High-end laptop for professionals", dto.getDescription());
        assertEquals("TechCorp", dto.getManufacturerName());
        assertNotNull(dto.getPrice(), "price mapped via CustomMapper");
        assertTrue(dto.isAvailable());
    }

    @Test
    @Order(22)
    @DisplayName("Custom set + extra field: 'SEARCH,description' extends SEARCH with description")
    void testCustomSetPlusExtraField() {
        final ProductWsDTO dto = dataMapper.map(fullProduct(), ProductWsDTO.class, "SEARCH,description");

        assertEquals("P001", dto.getCode());
        assertEquals("Laptop Pro", dto.getName());
        assertNotNull(dto.getCategoryNames());
        assertEquals("High-end laptop for professionals", dto.getDescription(),
                "description added via mixed descriptor");
        assertNull(dto.getPrice(), "price still not in SEARCH,description");
    }

    @Test
    @Order(23)
    @DisplayName("Custom set is case-insensitive: 'search' resolves same as 'SEARCH'")
    void testCustomFieldSetCaseInsensitive() {
        final ProductWsDTO upper = dataMapper.map(fullProduct(), ProductWsDTO.class, "SEARCH");
        final ProductWsDTO lower = dataMapper.map(fullProduct(), ProductWsDTO.class, "search");
        final ProductWsDTO mixed = dataMapper.map(fullProduct(), ProductWsDTO.class, "Search");

        assertEquals(upper.getCode(), lower.getCode());
        assertEquals(upper.getCategoryNames(), lower.getCategoryNames());
        assertEquals(upper.getCode(), mixed.getCode());
        assertNull(lower.getPrice());
        assertNull(mixed.getPrice());
    }

    @Test
    @Order(18)
    @DisplayName("Map product with zero stock: zero int is mapped correctly")
    void testZeroIntMapped() {
        ProductData source = new ProductData();
        source.setCode("Z");
        source.setStockLevel(0);
        source.setAvailable(false);

        ProductWsDTO dto = dataMapper.map(source, ProductWsDTO.class, "DEFAULT");

        assertEquals(0, dto.getStockLevel());
        assertFalse(dto.isAvailable());
    }

    // =========================================================================
    // Cumulative field-set definitions (NEW)
    // =========================================================================

    @Test
    @Order(24)
    @DisplayName("Cumulative: DEFAULT inherits BASIC's fields ({BASIC, price, available, stockLevel})")
    void testCumulativeDefaultIncludesBasic() {
        final ProductWsDTO dto = dataMapper.map(fullProduct(), ProductWsDTO.class, "DEFAULT");

        // From BASIC
        assertEquals("P001", dto.getCode());
        assertEquals("Laptop Pro", dto.getName());
        // From DEFAULT's own additions
        assertEquals(42, dto.getStockLevel());
        assertTrue(dto.isAvailable());
        assertNotNull(dto.getPrice(), "price added by DEFAULT (via CustomMapper)");
        // Outside DEFAULT
        assertNull(dto.getDescription());
    }

    @Test
    @Order(25)
    @DisplayName("Cumulative: custom SEARCH inherits BASIC ({BASIC, categoryNames})")
    void testCumulativeSearchIncludesBasic() {
        final ProductWsDTO dto = dataMapper.map(fullProduct(), ProductWsDTO.class, "SEARCH");

        assertEquals("P001", dto.getCode(), "from BASIC");
        assertEquals("Laptop Pro", dto.getName(), "from BASIC");
        assertNotNull(dto.getCategoryNames(), "from SEARCH itself");
        assertNull(dto.getDescription(), "not in SEARCH");
        assertNull(dto.getPrice(), "not in SEARCH");
    }

    @Test
    @Order(26)
    @DisplayName("Cumulative transitive: MOBILE → SEARCH → BASIC produces {code, name, categoryNames, stockLevel}")
    void testCumulativeTransitive() {
        final ProductWsDTO dto = dataMapper.map(fullProduct(), ProductWsDTO.class, "MOBILE");

        assertEquals("P001", dto.getCode(), "from BASIC (transitively)");
        assertEquals("Laptop Pro", dto.getName(), "from BASIC (transitively)");
        assertNotNull(dto.getCategoryNames(), "from SEARCH");
        assertEquals(42, dto.getStockLevel(), "from MOBILE itself");
        assertNull(dto.getDescription(), "not in MOBILE chain");
        assertNull(dto.getPrice(), "not in MOBILE chain");
    }

    @Test
    @Order(27)
    @DisplayName("Cumulative: CHECKOUT inherits BASIC ({BASIC, price, stockLevel})")
    void testCumulativeCheckoutIncludesBasic() {
        final ProductWsDTO dto = dataMapper.map(fullProduct(), ProductWsDTO.class, "CHECKOUT");

        assertEquals("P001", dto.getCode());
        assertEquals("Laptop Pro", dto.getName());
        assertEquals(42, dto.getStockLevel());
        assertNotNull(dto.getPrice());
        assertNull(dto.getDescription());
        assertFalse(dto.isAvailable(), "available not in CHECKOUT");
    }

    // =========================================================================
    // Error handling for malformed field-set definitions
    // =========================================================================

    @Test
    @Order(28)
    @DisplayName("Cyclic field-set definition throws IllegalStateException")
    void testCyclicReferenceDetected() {
        final de.hoomit.mapping.test.domain.CyclicFieldSetDTO src
                = new de.hoomit.mapping.test.domain.CyclicFieldSetDTO();
        src.setCode("X");
        src.setName("Y");

        final IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> dataMapper.map(src,
                        de.hoomit.mapping.test.domain.CyclicFieldSetDTO.class,
                        "DEFAULT"));

        assertTrue(ex.getMessage().contains("Circular"),
                "Error message should mention circular reference; got: " + ex.getMessage());
    }

    @Test
    @Order(29)
    @DisplayName("Unknown reference throws IllegalStateException with helpful message")
    void testUnknownReferenceDetected() {
        final de.hoomit.mapping.test.domain.UnknownRefDTO src
                = new de.hoomit.mapping.test.domain.UnknownRefDTO();
        src.setCode("X");
        src.setName("Y");

        final IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> dataMapper.map(src,
                        de.hoomit.mapping.test.domain.UnknownRefDTO.class,
                        "DEFAULT"));

        assertTrue(ex.getMessage().contains("Unknown field-set reference"),
                "Error message should mention unknown reference; got: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("@DOESNOTEXIST"),
                "Error message should include the bad token");
    }

    @Test
    @Order(30)
    @DisplayName("Custom name shadowing reserved level throws IllegalStateException")
    void testReservedNameShadowingDetected() {
        final de.hoomit.mapping.test.domain.ReservedNameDTO src
                = new de.hoomit.mapping.test.domain.ReservedNameDTO();
        src.setCode("X");
        src.setName("Y");

        final IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> dataMapper.map(src,
                        de.hoomit.mapping.test.domain.ReservedNameDTO.class,
                        "DEFAULT"));

        assertTrue(ex.getMessage().contains("shadows"),
                "Error message should mention shadowing; got: " + ex.getMessage());
    }
}
