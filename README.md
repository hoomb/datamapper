# DataMapper Framework

> A lightweight, extensible Java object-mapping framework inspired by the SAP Hybris / SAP Commerce Cloud `DataMapper` API — featuring **field-set filtering with custom named sets**, **auto-discovery**, custom converters, post-processing mappers, and field filters. No Orika, no Hybris dependencies required.

---

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
  - [Requirements](#requirements)
  - [Build](#build)
- [Core Concepts](#core-concepts)
  - [Field Sets](#field-sets)
  - [Custom Named Field Sets](#custom-named-field-sets)
  - [Cumulative Field Sets](#cumulative-field-sets)
  - [MappingContext](#mappingcontext)
  - [Converters](#converters)
  - [CustomMappers](#custommappers)
  - [FieldFilters](#fieldfilters)
  - [Field Renaming with @FieldMapping](#field-renaming-with-fieldmapping)
  - [Auto-Discovery with @WsDTOMapping](#auto-discovery-with-wsdtomapping)
- [Usage Examples](#usage-examples)
  - [1. Basic Mapping](#1-basic-mapping)
  - [2. Field-Set Filtering](#2-field-set-filtering)
  - [3. Custom Named Field Sets](#3-custom-named-field-sets)
  - [4. Cumulative Field Sets](#4-cumulative-field-sets)
  - [5. Mapping onto an Existing Object](#5-mapping-onto-an-existing-object)
  - [6. Collection Mapping](#6-collection-mapping)
  - [7. Custom Converter](#7-custom-converter)
  - [8. CustomMapper Post-Processor](#8-custommapper-post-processor)
  - [9. FieldFilter](#9-fieldfilter)
  - [10. Generic Type Mapping](#10-generic-type-mapping)
  - [11. Field Renaming Between DTO and Domain](#11-field-renaming-between-dto-and-domain)
- [Code Style Conventions](#code-style-conventions)
- [API Reference](#api-reference)
- [Mapping Pipeline](#mapping-pipeline)
- [Comparison with SAP Hybris DataMapper](#comparison-with-sap-hybris-datamapper)
- [Project Structure](#project-structure)
- [Running the Tests](#running-the-tests)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

When building REST APIs, you typically have two separate object layers:

- **Service/Data objects** — domain model objects returned by facades or services (e.g. `ProductData`)
- **WS DTOs** — lean payload objects serialised to JSON/XML (e.g. `ProductWsDTO`)

Manually writing boilerplate mapping code between these layers is tedious and error-prone. This framework automates that process while giving you fine-grained control over *which fields* are included in each response via **field sets** — a core concept from SAP Hybris/Commerce web services, extended here with support for arbitrary custom names.

```java
// Map a ProductData → ProductWsDTO, returning only DEFAULT fields
final ProductWsDTO dto = dataMapper.map(productData, ProductWsDTO.class, "DEFAULT");

// Or use a custom named set declared on the DTO class
final ProductWsDTO card = dataMapper.map(productData, ProductWsDTO.class, "SEARCH");

// Or an explicit comma-separated selection
final ProductWsDTO partial = dataMapper.map(productData, ProductWsDTO.class, "code,name,price");
```

---

## Key Features

- **Field-Set Filtering** — `BASIC`, `DEFAULT`, `FULL` levels plus **arbitrary custom named sets**, explicit field lists, and mixed descriptors like `"SEARCH,description"`
- **Cumulative Definitions** — sets reference each other (`defaults = {BASIC, "price"}`) with transitive expansion and cycle detection
- **Reflection-Based Mapping** — automatic getter → setter matching with a cached method-lookup engine; zero boilerplate for simple cases
- **Custom Converters** — take full ownership of a source→destination transformation
- **CustomMapper Post-Processors** — augment or override specific fields after the reflective pass
- **FieldFilters** — programmatically exclude fields based on their value (e.g. skip empty collections)
- **Field Renaming** — bridge naming differences between source and destination via `@FieldMapping` (repeatable; works symmetrically from either side)
- **Auto-Discovery** — annotate beans with `@WsDTOMapping` and register them in one call; integrates cleanly with Spring's `ApplicationContext`
- **Collection Helpers** — `mapAsList`, `mapAsSet`, `mapAsCollection`
- **Generic Type Support** — `mapGeneric` for parameterised types like `PageData<ProductData>`
- **Null Handling** — per-call `mapNulls` flag controls whether null source values overwrite destination fields
- **No external dependencies** — only the JDK; JUnit 5 is test-scope only

---

## Architecture

```
┌───────────────────────────────────────────────────────────────────┐
│                        DataMapper (interface)                     │
│   map()  mapAsList()  mapAsSet()  mapAsCollection()  mapGeneric() │
└───────────────────────────┬───────────────────────────────────────┘
                            │ implements
                            ▼
┌───────────────────────────────────────────────────────────────────┐
│                       DefaultDataMapper                           │
│                                                                   │
│   ┌──────────────┐  ┌───────────────────┐  ┌──────────────────┐   │
│   │  Converter   │  │ ReflectionMapping │  │  CustomMapper    │   │
│   │  Registry    │  │     Engine        │  │  (post-process)  │   │
│   └──────────────┘  └───────────────────┘  └──────────────────┘   │
│   ┌──────────────┐  ┌───────────────────┐                         │
│   │ FieldFilter  │  │  FieldSetBuilder  │                         │
│   │  Chain       │  │  (built-in +      │                         │
│   │              │  │   custom names)   │                         │
│   └──────────────┘  └───────────────────┘                         │
└───────────────────────────────────────────────────────────────────┘
```

---

## Getting Started

### Requirements

- Java 21+
- Maven 3.6+ (or compile manually — no external runtime dependencies)

### Install

```xml
<dependency>
    <groupId>de.hoomit.projects</groupId>
    <artifactId>datamapper</artifactId>
    <version>1.1.11</version>
</dependency>
```

## OR


### Build

```bash
git clone https://github.com/your-org/datamapper.git
cd datamapper
mvn clean install
```

To run only the tests:

```bash
mvn test
```
---
## Using with Spring Boot

In a Spring Boot application, the `DataMapper` is registered as a bean automatically. Just inject it where you need it:

```java
@Autowired
private DataMapper dataMapper;
```

If you prefer Constructor injection:

```java
private final DataMapper dataMapper;

public ProductService(final DataMapper dataMapper) {
    this.dataMapper = dataMapper;
}
```

## Hiding default and null fields in JSON responses

Thanks to the `@JacksonAnnotationsInside` meta-annotation on `@FieldSetDefinition`, any DTO marked with `@FieldSetDefinition` is automatically serialized with `@JsonInclude(JsonInclude.Include.NON_DEFAULT)` — fields whose values equal their type's default (`null`, `0`, `false`, empty collections) are omitted from the JSON output.

If you want to apply the same behavior to a DTO that doesn't use `@FieldSetDefinition`, add the annotation explicitly:

```java
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ProductDTO {
    // ...
}
```

---

## Core Concepts

### Field Sets

Field sets control *which fields* of a destination DTO are populated during a mapping call. You declare them on the DTO class itself using `@FieldSetDefinition`:

```java
@FieldSetDefinition(
    basic    = {"code", "name"},
    defaults = {"code", "name", "price", "available", "stockLevel"},
    full     = {}   // empty = ALL fields of the class
)
public class ProductWsDTO {
    private String  code;
    private String  name;
    private String  description;
    private String  price;
    private boolean available;
    private int     stockLevel;
    private String  manufacturerName;
    // ... getters and setters
}
```

The three built-in levels:

| Level | Description |
|-------|-------------|
| `BASIC` | Minimal fields — typically just identifier and display name |
| `DEFAULT` | Standard API payload — what most callers need |
| `FULL` | Everything; an empty `full = {}` array is a sentinel meaning "all declared fields" |

Field descriptor strings passed to `map()` support several forms:

| String | Behaviour |
|--------|-----------|
| `null` / `""` | Treated as `DEFAULT` |
| `"BASIC"` | Uses the BASIC field set |
| `"DEFAULT"` | Uses the DEFAULT field set |
| `"FULL"` | Uses the FULL field set |
| `"SEARCH"` | Any custom named set declared on the DTO (see below) |
| `"code,name,price"` | Explicit comma-separated list |
| `"DEFAULT,description"` | DEFAULT fields **plus** `description` |
| `"SEARCH,description"` | Any named set **plus** extra fields |

All set names are **case-insensitive** at resolution time.

---

### Custom Named Field Sets

Beyond the three built-in levels, you can declare any number of **custom named sets** on a DTO class via `@FieldSetDefinition#custom()`. Each entry uses the `@NamedFieldSet` sub-annotation:

```java
@FieldSetDefinition(
    basic    = {"code", "name"},
    defaults = {"code", "name", "price", "available", "stockLevel"},
    full     = {},
    custom   = {
        @NamedFieldSet(name = "SEARCH",   fields = {"code", "name", "categoryNames"}),
        @NamedFieldSet(name = "CHECKOUT", fields = {"code", "name", "price", "stockLevel"}),
        @NamedFieldSet(name = "ADMIN",    fields = {})  // empty = ALL fields, like FULL
    }
)
public class ProductWsDTO { ... }
```

Custom names follow the same rules as built-in levels:

- Names are **case-insensitive** (`"SEARCH"`, `"search"`, and `"Search"` all resolve identically).
- An **empty `fields` array** is the same sentinel used by `full()` — it means "include every declared field of the class."
- Custom names **must not shadow** the three reserved names: `BASIC`, `DEFAULT`, `FULL`.
- Custom names can be **mixed with extra fields** in a descriptor: `"SEARCH,description"` resolves to the SEARCH set plus the `description` field.

This is particularly useful for shaping different API endpoints from a single DTO class:

```java
// Search result card — minimal
final ProductWsDTO card = dataMapper.map(product, ProductWsDTO.class, "SEARCH");

// Cart line item — code, name, price, stock
final ProductWsDTO line = dataMapper.map(product, ProductWsDTO.class, "CHECKOUT");

// Admin export — every field
final ProductWsDTO export = dataMapper.map(product, ProductWsDTO.class, "ADMIN");
```

---

### Cumulative Field Sets

In real APIs, higher-level sets almost always include lower-level ones. Repeating the same field names in `basic`, `defaults`, and `full` creates duplication that drifts over time.

The framework supports **cumulative definitions** via reference constants in the `de.hoomit.mapping.fieldset.FieldSets` class. Inside any `String[]` array, an entry starting with `@` is a reference to another set and is expanded transitively at cache-build time.

The recommended pattern uses a static import for maximum readability:

```java
import static de.hoomit.mapping.fieldset.FieldSets.*;

@FieldSetDefinition(
    basic    = {"code", "name"},
    defaults = {BASIC, "price", "available", "stockLevel"},
    full     = {DEFAULT, "description", "manufacturerName", "imageUrl"},
    custom   = {
        @NamedFieldSet(name = "SEARCH",   fields = {BASIC, "categoryNames"}),
        @NamedFieldSet(name = "CHECKOUT", fields = {BASIC, "price", "stockLevel"}),
        @NamedFieldSet(name = "MOBILE",   fields = {ref("SEARCH"), "stockLevel"})
    }
)
public class ProductWsDTO { ... }
```

In this example:
- `DEFAULT` resolves to `{code, name, price, available, stockLevel}`
- `FULL` resolves to `DEFAULT`'s fields plus `description, manufacturerName, imageUrl`
- `SEARCH` resolves to `{code, name, categoryNames}`
- `MOBILE` resolves transitively through SEARCH → BASIC: `{code, name, categoryNames, stockLevel}`

**Reference rules:**

| Rule | Behaviour |
|------|-----------|
| Built-in references | Use constants `BASIC`, `DEFAULT`, `FULL` from `FieldSets` |
| Custom references | Use `FieldSets.ref("SEARCH")` or the literal `"@SEARCH"` |
| Case-insensitive | `"@search"`, `"@SEARCH"`, `"@Search"` all resolve identically |
| Transitive | `MOBILE → SEARCH → BASIC` is fully expanded |
| Circular | Throws `IllegalStateException` with the cycle path on first resolution |
| Unknown | Throws `IllegalStateException` listing all known set names |
| Reserved-name shadowing | A custom set named `"BASIC"`, `"DEFAULT"`, or `"FULL"` throws on resolution |

The empty-array sentinel for "all fields" still works — `full = {}` and `@NamedFieldSet(name = "ADMIN", fields = {})` both expand to every declared field of the class.

---

### MappingContext

`MappingContext` is a value object threaded through the entire mapping pipeline. It carries:

| Property | Description |
|----------|-------------|
| `fieldSetName` | The raw field descriptor string |
| `mapNulls` | Whether `null` source values overwrite destination fields |
| `fieldPrefix` | Dot-path prefix for nested field resolution |
| `resolvedFields` | Pre-resolved `Set<String>` (populated by the framework) |
| Extension map | Arbitrary key/value pairs for custom components |

```java
// Built automatically by the framework, but you can inspect it in custom components:
final MappingContext ctx = MappingContext.builder()
    .fieldSetName("DEFAULT")
    .mapNulls(false)
    .fieldPrefix("product")
    .build();

final boolean include = ctx.includes("price");   // true/false based on field set
```

---

### Converters

A `Converter<S, D>` takes **full ownership** of producing the destination object. The reflection engine is bypassed entirely when a converter is registered for a given type pair.

```java
@WsDTOMapping
public class PriceConverter implements Converter<PriceData, PriceWsDTO> {

    @Override
    public PriceWsDTO convert(final PriceData source, final MappingContext ctx) {
        final PriceWsDTO dto = new PriceWsDTO();
        dto.setValue(source.getValue().toPlainString());
        dto.setCurrencyIso(source.getCurrencyIso());
        dto.setFormattedValue(String.format("%s %.2f",
            source.getCurrencyIso(), source.getValue()));
        return dto;
    }
}
```

---

### CustomMappers

A `CustomMapper<A, B>` is a **post-processor**: it runs *after* the default reflection engine has already copied matching fields. Use it to handle fields that don't share the same name or type between source and destination.

```java
@WsDTOMapping
public class ProductCustomMapper implements CustomMapper<ProductData, ProductWsDTO> {

    @Override
    public void mapAtoB(final ProductData source, final ProductWsDTO dest,
                        final MappingContext ctx) {
        // Runs after reflection; handle the price field (BigDecimal → String)
        if (ctx.includes("price") && source.getBasePrice() != null) {
            dest.setPrice(String.format("%s %.2f",
                source.getCurrencyIso(), source.getBasePrice()));
        }
    }

    @Override
    public void mapBtoA(final ProductWsDTO source, final ProductData dest,
                        final MappingContext ctx) {
        // Optional reverse direction
        if (source.getPrice() != null) {
            final String[] parts = source.getPrice().split(" ");
            if (parts.length == 2) {
                dest.setBasePrice(new BigDecimal(parts[1]));
                dest.setCurrencyIso(parts[0]);
            }
        }
    }
}
```

---

### FieldFilters

A `FieldFilter` is consulted for every field *after* the field-set check passes. It can reject a field based on the actual value, the mapping context, or the source/destination types.

```java
@WsDTOMapping
public class EmptyCollectionFilter implements FieldFilter {

    @Override
    public boolean isApplicable(final Class<?> sourceType, final Class<?> destType) {
        return true; // apply globally
    }

    @Override
    public boolean include(final String fieldName, final Object value,
                           final MappingContext ctx) {
        if (value instanceof final Collection<?> col) {
            return !col.isEmpty(); // skip empty collections
        }
        return true;
    }
}
```

---

### Field Renaming with @FieldMapping

When a source and destination field carry different names — common when bridging external DTOs to internal domain types — declare a `@FieldMapping` on the field that needs renaming:

```java
public class UserSignUpWsDTO {

    @FieldMapping(pairedClass = RegisterData.class, pairedField = "login")
    private String uid;

    private String password;
}
```

This single declaration covers both mapping directions: `UserSignUpWsDTO → RegisterData` (write `uid`'s value into `login`) **and** `RegisterData → UserSignUpWsDTO` (write `login`'s value into `uid`). The framework consults `@FieldMapping` declarations on both sides of the class pair, so you only need to annotate one of them.

**Multiple targets on a single field.** `@FieldMapping` is `@Repeatable`. A DTO field that maps to several domain types just stacks the annotations — no container annotation needed:

```java
public class UserSignUpWsDTO {

    @FieldMapping(pairedClass = RegisterData.class,   pairedField = "login")
    @FieldMapping(pairedClass = LegacyUserData.class, pairedField = "username")
    private String uid;
}
```

When mapping `UserSignUpWsDTO → RegisterData`, the first declaration applies; when mapping to `LegacyUserData`, the second applies. Each is selected by its `pairedClass` argument at mapping time.

**Where to put the annotation.** Either side works. Put it on the DTO when the domain class is third-party or generated. Put it on the domain class when several DTOs map to the same domain field with the same rename. If both sides declare the same rename, the source-side declaration wins — but this is best avoided to keep the configuration single-sourced.

**Interaction with field sets.** Field-set descriptors always refer to the **destination** field name. To include the renamed field in a custom set on `RegisterData`, list `login` (not `uid`). This matches the principle that field sets describe the shape of the output.

| Rule | Behaviour |
|------|-----------|
| Annotation location | Either source or destination class; both work |
| Repeatable | Multiple `@FieldMapping` on the same field, one per `pairedClass` |
| Direction-agnostic | A single declaration works for both source→dest and dest→source |
| Conflict resolution | Source-side declaration wins if both sides annotate the same rename |
| Field-set names | Always refer to the destination field name |

---

### Auto-Discovery with @WsDTOMapping

Mark any `Converter`, `CustomMapper`, or `FieldFilter` bean with `@WsDTOMapping` and register it in one call:

```java
final DefaultDataMapper dataMapper = new DefaultDataMapper();

dataMapper.registerBeans(List.of(
    new ProductCustomMapper(),
    new PriceConverter(),
    new EmptyCollectionFilter()
));
```

**Spring integration** — pass the application context's bean list directly:

```java
@Configuration
public class MappingConfig {

    @Bean
    public DefaultDataMapper dataMapper(final ApplicationContext ctx) {
        final DefaultDataMapper mapper = new DefaultDataMapper();
        mapper.registerBeans(ctx.getBeansWithAnnotation(WsDTOMapping.class).values());
        return mapper;
    }
}
```

Or register individual components manually:

```java
dataMapper.addConverter(new PriceConverter());
dataMapper.addMapper(new ProductCustomMapper());
dataMapper.addFilter(new EmptyCollectionFilter());
```

---

## Usage Examples

### 1. Basic Mapping

The simplest usage — reflective field matching with no field-set filtering (maps everything):

```java
final DefaultDataMapper dataMapper = new DefaultDataMapper();

final ProductData source = new ProductData();
source.setCode("P001");
source.setName("Laptop Pro");
source.setDescription("High-end laptop");
source.setStockLevel(42);

// Create and return a new ProductWsDTO
final ProductWsDTO dto = dataMapper.map(source, ProductWsDTO.class);

System.out.println(dto.getCode());   // "P001"
System.out.println(dto.getName());   // "Laptop Pro"
```

---

### 2. Field-Set Filtering

```java
// BASIC: only code and name are mapped
final ProductWsDTO basic = dataMapper.map(source, ProductWsDTO.class, "BASIC");
// basic.getDescription() == null  ✓

// DEFAULT: code, name, price, available, stockLevel
final ProductWsDTO defaults = dataMapper.map(source, ProductWsDTO.class, "DEFAULT");
// defaults.getStockLevel() == 42  ✓

// FULL: every field
final ProductWsDTO full = dataMapper.map(source, ProductWsDTO.class, "FULL");
// full.getDescription() == "High-end laptop"  ✓

// Explicit field list
final ProductWsDTO partial = dataMapper.map(source, ProductWsDTO.class, "code,description");
// partial.getCode() == "P001"         ✓
// partial.getName() == null           ✓  (not in list)
// partial.getDescription() == "..."   ✓

// Mix: level + extra field
final ProductWsDTO mixed = dataMapper.map(source, ProductWsDTO.class, "DEFAULT,description");
// Gets all DEFAULT fields PLUS description
```

---

### 3. Custom Named Field Sets

Custom sets are declared once on the DTO and used freely from anywhere in the application:

```java
@FieldSetDefinition(
    basic    = {"code", "name"},
    defaults = {"code", "name", "price", "available", "stockLevel"},
    full     = {},
    custom   = {
        @NamedFieldSet(name = "SEARCH",   fields = {"code", "name", "categoryNames"}),
        @NamedFieldSet(name = "CHECKOUT", fields = {"code", "name", "price", "stockLevel"}),
        @NamedFieldSet(name = "ADMIN",    fields = {})  // empty = ALL fields
    }
)
public class ProductWsDTO { ... }
```

Used at the call site:

```java
// Search result card with category breadcrumbs
final ProductWsDTO card = dataMapper.map(product, ProductWsDTO.class, "SEARCH");
// card.getCode(), card.getName(), card.getCategoryNames() are populated
// All other fields (description, manufacturerName, imageUrl) are null

// Cart line item — focused on what the cart needs
final ProductWsDTO line = dataMapper.map(product, ProductWsDTO.class, "CHECKOUT");

// Admin export — empty fields {} = all fields
final ProductWsDTO export = dataMapper.map(product, ProductWsDTO.class, "ADMIN");

// Custom set + extra field
final ProductWsDTO enriched = dataMapper.map(product, ProductWsDTO.class, "SEARCH,description");

// Case-insensitive
final ProductWsDTO same = dataMapper.map(product, ProductWsDTO.class, "search");  // works
```

**When to use custom sets vs. explicit field lists:**

- Use **custom sets** when the same combination of fields is reused across multiple endpoints — declare it once, change it once.
- Use **explicit field lists** for one-off API endpoints or ad-hoc queries.

---

### 4. Cumulative Field Sets

Avoid duplicating field lists. Reference one set from another using the `FieldSets` constants:

```java
import static de.hoomit.mapping.fieldset.FieldSets.*;

@FieldSetDefinition(
    basic    = {"code", "name"},
    defaults = {BASIC, "price", "available", "stockLevel"},
    full     = {DEFAULT, "description", "manufacturerName"},
    custom   = {
        @NamedFieldSet(name = "SEARCH", fields = {BASIC, "categoryNames"}),
        @NamedFieldSet(name = "MOBILE", fields = {ref("SEARCH"), "stockLevel"})
    }
)
public class ProductWsDTO { ... }
```

```java
// DEFAULT now produces: {code, name, price, available, stockLevel}
final ProductWsDTO d = dataMapper.map(product, ProductWsDTO.class, "DEFAULT");

// MOBILE chains through SEARCH → BASIC, producing:
// {code, name, categoryNames, stockLevel}
final ProductWsDTO m = dataMapper.map(product, ProductWsDTO.class, "MOBILE");
```

Adding a new field to BASIC automatically propagates to every set that references it, eliminating the silent drift that comes from duplicated field lists.

---

### 5. Mapping onto an Existing Object

```java
final ProductWsDTO existing = new ProductWsDTO();
existing.setCode("OLD");
existing.setDescription("preserved description");

final ProductData update = new ProductData();
update.setCode("P002");
update.setName("Gaming Mouse");

// Map only BASIC fields (code + name); description is untouched
dataMapper.map(update, existing, "BASIC");

System.out.println(existing.getCode());        // "P002"   (updated)
System.out.println(existing.getDescription()); // "preserved description" (untouched)

// Control null handling
dataMapper.map(update, existing, "FULL", /* mapNulls= */ false);
// null fields in 'update' will NOT overwrite non-null fields in 'existing'
```

---

### 6. Collection Mapping

```java
final List<ProductData> products = productFacade.getProductsForCategory("electronics");

// Map to List
final List<ProductWsDTO> dtoList =
    dataMapper.mapAsList(products, ProductWsDTO.class, "DEFAULT");

// Map to Set (deduplicates)
final Set<ProductWsDTO> dtoSet =
    dataMapper.mapAsSet(products, ProductWsDTO.class, "BASIC");

// Append into an existing collection
final List<ProductWsDTO> accumulator = new ArrayList<>();
dataMapper.mapAsCollection(products, accumulator, ProductWsDTO.class, "SEARCH");
```

---

### 7. Custom Converter

When you need full control over how a destination object is built (e.g. complex type transformations or calculated fields):

```java
@WsDTOMapping
public class CategoryConverter implements Converter<CategoryData, CategoryWsDTO> {

    @Override
    public CategoryWsDTO convert(final CategoryData source, final MappingContext ctx) {
        final CategoryWsDTO dto = new CategoryWsDTO();
        dto.setCode(source.getCode().toLowerCase());
        dto.setName(source.getName());
        dto.setUrl("/categories/" + source.getCode());

        if (ctx.includes("productCount")) {
            dto.setProductCount(source.getProducts().size());
        }
        return dto;
    }
}

// Register and use
dataMapper.addConverter(new CategoryConverter());

final CategoryWsDTO dto = dataMapper.map(categoryData, CategoryWsDTO.class, "DEFAULT,productCount");
System.out.println(dto.getUrl()); // "/categories/electronics"
```

---

### 8. CustomMapper Post-Processor

Ideal when most fields map 1-to-1 by name, but a few need special handling:

```java
@WsDTOMapping
public class OrderCustomMapper implements CustomMapper<OrderData, OrderWsDTO> {

    @Override
    public void mapAtoB(final OrderData source, final OrderWsDTO dest,
                        final MappingContext ctx) {
        // The reflection engine already copied matching fields.
        // Handle the fields that need custom logic:

        if (ctx.includes("statusDisplay")) {
            dest.setStatusDisplay(source.getStatus().getDisplayName());
        }

        if (ctx.includes("totalItems")) {
            dest.setTotalItems(source.getEntries().stream()
                .mapToInt(OrderEntryData::getQuantity).sum());
        }
    }
}
```

---

### 9. FieldFilter

Use filters to apply cross-cutting rules across all mappings — for example, removing sensitive fields in non-admin contexts:

```java
@WsDTOMapping
public class SensitiveFieldFilter implements FieldFilter {

    private static final Set<String> SENSITIVE = Set.of(
        "internalCost", "supplierCode", "marginPercent"
    );

    @Override
    public boolean isApplicable(final Class<?> sourceType, final Class<?> destType) {
        return true;
    }

    @Override
    public boolean include(final String fieldName, final Object value,
                           final MappingContext ctx) {
        final boolean isAdmin = Boolean.TRUE.equals(ctx.get("isAdmin"));
        if (SENSITIVE.contains(fieldName) && !isAdmin) {
            return false; // strip sensitive fields for non-admin callers
        }
        return true;
    }
}

// Usage: pass extra context attributes
final MappingContext ctx = MappingContext.builder()
    .fieldSetName("FULL")
    .extra("isAdmin", userHasAdminRole())
    .build();
```

---

### 10. Generic Type Mapping

For paginated responses or other parameterised wrapper types:

```java
// Source: ProductSearchPageData<SearchStateData, ProductData>
// Dest:   ProductSearchPageWsDTO<SearchStateWsDTO, ProductWsDTO>

final ProductSearchPageWsDTO<SearchStateWsDTO, ProductWsDTO> dest =
    new ProductSearchPageWsDTO<>();

dataMapper.mapGeneric(
    sourcePageData,
    dest,
    new Type[]{ SearchStateData.class, ProductData.class },   // source type args
    new Type[]{ SearchStateWsDTO.class, ProductWsDTO.class }, // dest type args
    "DEFAULT",
    Map.of("S", SearchStateWsDTO.class, "R", ProductWsDTO.class)
);
```

---

### 11. Field Renaming Between DTO and Domain

A `UserSignUpWsDTO` (external API) and a `RegisterData` (internal domain) carry the user identifier under different field names:

```java
public class UserSignUpWsDTO {

    @FieldMapping(pairedClass = RegisterData.class, pairedField = "login")
    private String uid;

    private String password;
    private String firstName;
    private String lastName;
    // getters / setters ...
}

public class RegisterData {

    private String login;
    private String password;
    private String firstName;
    private String lastName;
    // getters / setters ...
}
```

The annotation works in both directions:

```java
// Incoming: DTO → Domain (uid → login)
final UserSignUpWsDTO incoming = parseFromJson(request);
final RegisterData domain = dataMapper.map(incoming, RegisterData.class, "FULL");
// domain.getLogin() returns the value originally in incoming.getUid()

// Outgoing: Domain → DTO (login → uid)
final RegisterData stored = userFacade.load(userId);
final UserSignUpWsDTO outgoing = dataMapper.map(stored, UserSignUpWsDTO.class, "FULL");
// outgoing.getUid() returns the value originally in stored.getLogin()
```

For a DTO that maps to several domain types, stack the annotations:

```java
public class UserSignUpWsDTO {

    @FieldMapping(pairedClass = RegisterData.class,   pairedField = "login")
    @FieldMapping(pairedClass = LegacyUserData.class, pairedField = "username")
    private String uid;
    // ...
}

// Selected by pairedClass at mapping time
final RegisterData   reg    = dataMapper.map(dto, RegisterData.class,   "FULL");  // uid → login
final LegacyUserData legacy = dataMapper.map(dto, LegacyUserData.class, "FULL");  // uid → username
```

Field-set descriptors refer to destination names — so to include the renamed field, list `"login"`, not `"uid"`:

```java
final RegisterData partial = dataMapper.map(dto, RegisterData.class, "login");
// partial.getLogin() is populated; other fields stay null
```

---

## Code Style Conventions

The framework follows a few conventions consistently across its codebase. Contributors are expected to honour them:

**`final` everywhere applicable.** All method parameters and all local variables that are not reassigned are declared `final`. This protects against accidental mutation, communicates intent, and makes the code safer to refactor.

```java
public <S, D> D map(final S source, final Class<D> destClass, final String fields) {
    final Set<String> resolved = FieldSetBuilder.resolve(destClass, fields);
    final MappingContext ctx = MappingContext.builder()
            .fieldSetName(fields != null ? fields : "DEFAULT")
            .resolvedFields(resolved)
            .build();
    return doMap(source, destClass, ctx);
}
```

**Lambdas and streams over imperative loops.** Where a `for` loop is purely transformative, the implementation prefers a stream pipeline. Readability and intent come first — loops remain in the rare cases where they are genuinely clearer.

```java
// Filter chain — lambda form
private boolean passFilters(final List<FieldFilter> filters, final String fieldName,
                            final Object value, final MappingContext ctx,
                            final Class<?> src, final Class<?> dest) {
    return filters.stream()
            .noneMatch(filter ->
                    filter.isApplicable(src, dest) && !filter.include(fieldName, value, ctx));
}
```

```java
// Bean auto-registration — stream + pattern matching
public void registerBeans(final Iterable<?> beans) {
    StreamSupport.stream(beans.spliterator(), false)
            .filter(bean -> bean.getClass().isAnnotationPresent(WsDTOMapping.class))
            .forEach(bean -> {
                if (bean instanceof final Converter<?, ?> c) {
                    registry.registerConverter(c);
                } else if (bean instanceof final CustomMapper<?, ?> m) {
                    registry.registerMapper(m);
                } else if (bean instanceof final FieldFilter f) {
                    registry.registerFilter(f);
                }
            });
}
```

**Pattern matching for `instanceof`.** When a type check is followed by a cast, use Java 16+ pattern matching with a `final` binding variable:

```java
if (value instanceof final Collection<?> col) {
    return !col.isEmpty();
}
```

---

## API Reference

### DataMapper Interface

```java
// Create & return (new destination instance)
<S, D> D map(final S source, final Class<D> dest)
<S, D> D map(final S source, final Class<D> dest, final String fields)
<S, D> D map(final S source, final Class<D> dest, final Set<String> fields)

// Map onto existing instance
<S, D> void map(final S source, final D dest)
<S, D> void map(final S source, final D dest, final boolean mapNulls)
<S, D> void map(final S source, final D dest, final String fields)
<S, D> void map(final S source, final D dest, final String fields, final boolean mapNulls)

// Generic (parameterized) types
<S, D> void mapGeneric(final S source, final D dest,
                       final Type[] sourceTypeArgs, final Type[] destTypeArgs,
                       final String fields, final Map<String, Class<?>> destTypeVariableMap)

// Collections
<S, D> List<D>  mapAsList(final Iterable<S> source, final Class<D> dest, final String fields)
<S, D> Set<D>   mapAsSet(final Iterable<S> source, final Class<D> dest, final String fields)
<S, D> void     mapAsCollection(final Iterable<S> source, final Collection<D> dest,
                                final Class<D> destClass, final String fields)
```

### DefaultDataMapper Registration

```java
void registerBeans(final Iterable<?> beans)    // auto-discovers @WsDTOMapping
void addConverter(final Converter<?, ?> c)
void addMapper(final CustomMapper<?, ?> m)
void addFilter(final FieldFilter f)
```

### Field Set Annotations

```java
@FieldSetDefinition(
    basic    = String[],         // BASIC level fields
    defaults = String[],         // DEFAULT level fields
    full     = String[],         // FULL level fields ({} = all class fields)
    custom   = NamedFieldSet[]   // any number of custom named sets
)

@NamedFieldSet(
    name   = String,             // case-insensitive; cannot be BASIC/DEFAULT/FULL
    fields = String[]            // {} = all class fields, like full()
)
```

### Field Renaming Annotation

```java
// Repeatable — stack multiple on a single field for multiple paired classes.
// Place on either the source or the destination class; both sides are honoured.
@FieldMapping(
    pairedClass = Class<?>,      // the class on the other side of the mapping
    pairedField = String         // field name on the paired class
)
```

### Cumulative Reference Constants

```java
// Inside any String[] in @FieldSetDefinition or @NamedFieldSet,
// these constants and the ref() helper expand transitively at cache-build time.
public final class FieldSets {
    public static final String BASIC   = "@BASIC";
    public static final String DEFAULT = "@DEFAULT";
    public static final String FULL    = "@FULL";
    public static String ref(final String customName);   // → "@" + customName.toUpperCase()
}
```

---

## Mapping Pipeline

For each object mapping call, the framework executes the following pipeline:

```
Input: sourceObject, destinationClass, fields, mapNulls
  │
  ▼
1. FieldSetBuilder.resolve(destClass, fields)
   → Tokenises descriptor, looks up each token in the unified
     name-map (built-in + custom sets), produces Set<String>
  │
  ▼
2. MappingContext built with fieldSet, mapNulls flag, resolved fields
  │
  ▼
3. Is a Converter<S,D> registered?
   ├─ YES → converter.convert(source, ctx) → return result
   └─ NO  ↓
  │
  ▼
4. Instantiate destination via no-arg constructor
  │
  ▼
5. ReflectionMappingEngine
   For each setter on destination class:
     a. Resolve source-side name via FieldMappingRegistry
        (uses @FieldMapping renames if declared; falls back to
        the destination field name for 1:1 matches)
     b. ctx.includes(destFieldName)?         → skip if false
     c. Read value via source getter (or field)
     d. value == null && !mapNulls?          → skip if true
     e. FieldFilter.include(field, value)?   → skip if false
     f. setter.invoke(dest, value)
  │
  ▼
6. Is a CustomMapper<S,D> registered?
   └─ YES → mapper.mapAtoB(source, dest, ctx)
  │
  ▼
Output: populated destination object
```

The `FieldSetBuilder` cache is a unified `Map<Class<?>, Map<String, Set<String>>>` keyed by upper-cased set name. Built-in levels and custom names share the same map, so resolution is a single `O(1)` lookup regardless of which kind is requested.

---

## Comparison with SAP Hybris DataMapper

| Feature | SAP Hybris DataMapper | This Framework |
|---|---|---|
| Core interface | `de.hybris.platform.webservicescommons.mapping.DataMapper` | `de.hoomit.mapping.DataMapper` |
| Default impl | `DefaultDataMapper extends ConfigurableMapper` | `DefaultDataMapper` (pure reflection) |
| Mapping engine | Orika (`MapperFacade`) | Custom reflection engine |
| Built-in field-set levels | ✅ `BASIC`, `DEFAULT`, `FULL` | ✅ `BASIC`, `DEFAULT`, `FULL` |
| **Custom named field sets** | ❌ Not supported out of the box | ✅ `@NamedFieldSet` inside `@FieldSetDefinition#custom()` |
| **Cumulative field-set definitions** | ❌ Not supported | ✅ `FieldSets.BASIC` / `DEFAULT` / `FULL` references with transitive expansion and cycle detection |
| **Field renaming** | ✅ XML `FieldMapper` bean | ✅ `@FieldMapping` annotation (repeatable, symmetric, no XML) |
| Auto-discovery | ✅ `@WsDTOMapping` + Spring context | ✅ `@WsDTOMapping` + `registerBeans()` |
| Custom converters | ✅ Orika `Converter` | ✅ `Converter<S,D>` |
| Custom mappers | ✅ Orika `Mapper` | ✅ `CustomMapper<A,B>` |
| Field filters | ✅ Orika `Filter` | ✅ `FieldFilter` |
| mapNulls flag | ✅ `MAP_NULLS` context key | ✅ `mapNulls` param / context |
| Generic types | ✅ `mapGeneric()` | ✅ `mapGeneric()` |
| Collection helpers | ✅ `mapAsList/Set/Collection` | ✅ `mapAsList/Set/Collection` |
| Hybris dependency | ❌ Required | ✅ None |
| Spring dependency | ❌ Required | ✅ Optional |

---

## Project Structure

```
datamapper/
├── pom.xml
└── src/
    ├── main/java/com/framework/mapping/
    │   ├── DataMapper.java                    # Central interface
    │   ├── annotation/
    │   │   ├── WsDTOMapping.java              # Auto-discovery marker
    │   │   ├── FieldMapping.java              # Per-field rename (repeatable)
    │   │   └── FieldMappings.java             # Container for repeated @FieldMapping
    │   ├── context/
    │   │   └── MappingContext.java            # Immutable context value object
    │   ├── converter/
    │   │   └── Converter.java                 # Full-ownership converter interface
    │   ├── mapper/
    │   │   └── CustomMapper.java              # Post-processing mapper interface
    │   ├── filter/
    │   │   └── FieldFilter.java               # Per-field exclusion interface
    │   ├── fieldset/
    │   │   ├── FieldLevel.java                # BASIC / DEFAULT / FULL enum
    │   │   ├── FieldSetDefinition.java        # @FieldSetDefinition (basic/defaults/full/custom)
    │   │   ├── NamedFieldSet.java             # @NamedFieldSet for custom named sets
    │   │   ├── FieldSets.java                 # BASIC/DEFAULT/FULL constants + ref() helper
    │   │   └── FieldSetBuilder.java           # Resolves field sets with caching + reference expansion
    │   └── impl/
    │       ├── DefaultDataMapper.java         # Main implementation
    │       ├── MappingRegistry.java           # Converter/mapper/filter store
    │       ├── FieldMappingRegistry.java      # @FieldMapping rename cache
    │       ├── ReflectionMappingEngine.java   # Getter→setter reflection engine
    │       └── MappingException.java          # Unchecked runtime exception
    └── test/java/com/framework/mapping/
        └── test/
            ├── DataMapperTest.java            # Field-set, converter, filter tests
            ├── FieldMappingTest.java          # @FieldMapping rename tests
            ├── domain/
            │   ├── ProductData.java           # Service-layer domain object
            │   ├── ProductWsDTO.java          # WS DTO with cumulative + custom field sets
            │   ├── UserSignUpWsDTO.java       # DTO with repeated @FieldMapping
            │   ├── RegisterData.java          # Domain target for UserSignUpWsDTO
            │   ├── LegacyUserData.java        # Second domain target (multi-target test)
            │   ├── AuditEvent.java            # Dest-side-only @FieldMapping fixture
            │   ├── CyclicFieldSetDTO.java     # Fixture: circular reference (negative test)
            │   ├── UnknownRefDTO.java         # Fixture: unknown reference (negative test)
            │   └── ReservedNameDTO.java       # Fixture: name shadowing (negative test)
            └── mappers/
                ├── ProductPriceMapper.java    # CustomMapper example
                └── NullCollectionFilter.java  # FieldFilter example
```

---

## Running the Tests

```bash
mvn test
```

The test suite covers:

**Built-in field sets**
- `BASIC` / `DEFAULT` / `FULL` field-set filtering
- `null` field descriptor defaults to `DEFAULT`
- Explicit field lists (`"code,description"`)
- Mixed descriptors (`"DEFAULT,description"`)
- `Set<String>` field selection

**Custom named field sets**
- Custom `SEARCH` set returns only declared fields
- Custom `CHECKOUT` set restricts to its declared fields
- Custom `ADMIN` set with empty `fields = {}` behaves like `FULL`
- Custom set combined with extras (`"SEARCH,description"`)
- Case-insensitive name resolution (`"SEARCH"` ≡ `"search"` ≡ `"Search"`)

**Cumulative field-set definitions**
- DEFAULT inherits BASIC's fields via `{BASIC, ...}` reference
- Custom SEARCH inherits BASIC's fields
- Custom CHECKOUT inherits BASIC's fields
- Transitive resolution through MOBILE → SEARCH → BASIC

**Error handling**
- Circular reference throws `IllegalStateException`
- Unknown reference throws `IllegalStateException` with helpful message
- Custom name shadowing a reserved level throws `IllegalStateException`

**CustomMapper post-processing**
- Price formatting (`BigDecimal` → `"EUR 1299.99"` string)
- CustomMapper not invoked for fields outside the active field set

**Mapping onto existing objects**
- Only fields in the active set are updated
- `mapNulls=false` — null does not overwrite destination
- `mapNulls=true` — null overwrites destination

**Collections**
- `mapAsList`, `mapAsSet`, `mapAsCollection`

**FieldFilter**
- Empty collections excluded
- Non-empty collections passed through

**Field renaming with @FieldMapping**
- Source-side declaration applies to `source → dest` direction
- Destination-side declaration applies symmetrically (`source → dest` and reverse)
- Multiple `@FieldMapping` on one field, selected by `pairedClass`
- Round-trip preserves the original value
- Field-set descriptor refers to the destination name after rename
- Registry caches results across calls

**Edge cases**
- Zero-value primitives mapped correctly

---

## Contributing

Contributions are welcome! Please open an issue to discuss your idea before submitting a PR.

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit your changes: `git commit -m 'Add my feature'`
4. Push to the branch: `git push origin feature/my-feature`
5. Open a Pull Request

Please ensure all tests pass (`mvn test`) and add new tests for any new behaviour. Match the existing code style: `final` on parameters and unreassigned locals, lambdas/streams over imperative loops, and pattern matching for `instanceof`.

---

## License

This project is licensed under the **MIT License**. See [LICENSE](LICENSE) for details.

---

> Inspired by the `de.hybris.platform.webservicescommons.mapping.DataMapper` API from SAP Hybris / SAP Commerce Cloud. This project has no affiliation with or dependency on SAP or Hybris.
