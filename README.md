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
  - [MappingContext](#mappingcontext)
  - [Converters](#converters)
  - [CustomMappers](#custommappers)
  - [FieldFilters](#fieldfilters)
  - [Auto-Discovery with @WsDTOMapping](#auto-discovery-with-wsdtomapping)
- [Usage Examples](#usage-examples)
  - [1. Basic Mapping](#1-basic-mapping)
  - [2. Field-Set Filtering](#2-field-set-filtering)
  - [3. Custom Named Field Sets](#3-custom-named-field-sets)
  - [4. Mapping onto an Existing Object](#4-mapping-onto-an-existing-object)
  - [5. Collection Mapping](#5-collection-mapping)
  - [6. Custom Converter](#6-custom-converter)
  - [7. CustomMapper Post-Processor](#7-custommapper-post-processor)
  - [8. FieldFilter](#8-fieldfilter)
  - [9. Generic Type Mapping](#9-generic-type-mapping)
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
- **Reflection-Based Mapping** — automatic getter → setter matching with a cached method-lookup engine; zero boilerplate for simple cases
- **Custom Converters** — take full ownership of a source→destination transformation
- **CustomMapper Post-Processors** — augment or override specific fields after the reflective pass
- **FieldFilters** — programmatically exclude fields based on their value (e.g. skip empty collections)
- **Auto-Discovery** — annotate beans with `@WsDTOMapping` and register them in one call; integrates cleanly with Spring's `ApplicationContext`
- **Collection Helpers** — `mapAsList`, `mapAsSet`, `mapAsCollection`
- **Generic Type Support** — `mapGeneric` for parameterised types like `PageData<ProductData>`
- **Null Handling** — per-call `mapNulls` flag controls whether null source values overwrite destination fields
- **No external dependencies** — only the JDK; JUnit 5 is test-scope only

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        DataMapper (interface)                    │
│   map()  mapAsList()  mapAsSet()  mapAsCollection()  mapGeneric()│
└───────────────────────────┬─────────────────────────────────────┘
                            │ implements
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                      DefaultDataMapper                           │
│                                                                  │
│  ┌──────────────┐  ┌───────────────────┐  ┌──────────────────┐ │
│  │  Converter   │  │ ReflectionMapping │  │  CustomMapper    │ │
│  │  Registry    │  │     Engine        │  │  (post-process)  │ │
│  └──────────────┘  └───────────────────┘  └──────────────────┘ │
│  ┌──────────────┐  ┌───────────────────┐                        │
│  │ FieldFilter  │  │  FieldSetBuilder  │                        │
│  │  Chain       │  │  (built-in +      │                        │
│  │              │  │   custom names)   │                        │
│  └──────────────┘  └───────────────────┘                        │
└─────────────────────────────────────────────────────────────────┘
```

---

## Getting Started

### Requirements

- Java 17+
- Maven 3.6+ (or compile manually — no external runtime dependencies)

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

### 4. Mapping onto an Existing Object

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

### 5. Collection Mapping

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

### 6. Custom Converter

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

### 7. CustomMapper Post-Processor

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

### 8. FieldFilter

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

### 9. Generic Type Mapping

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
     a. ctx.includes(fieldName)?           → skip if false
     b. Read value via getter (or field)
     c. value == null && !mapNulls?        → skip if true
     d. FieldFilter.include(field, value)? → skip if false
     e. setter.invoke(dest, value)
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
| Core interface | `de.hybris.platform.webservicescommons.mapping.DataMapper` | `com.framework.mapping.DataMapper` |
| Default impl | `DefaultDataMapper extends ConfigurableMapper` | `DefaultDataMapper` (pure reflection) |
| Mapping engine | Orika (`MapperFacade`) | Custom reflection engine |
| Built-in field-set levels | ✅ `BASIC`, `DEFAULT`, `FULL` | ✅ `BASIC`, `DEFAULT`, `FULL` |
| **Custom named field sets** | ❌ Not supported out of the box | ✅ `@NamedFieldSet` inside `@FieldSetDefinition#custom()` |
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
    │   │   └── WsDTOMapping.java              # Auto-discovery marker
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
    │   │   └── FieldSetBuilder.java           # Resolves field sets with caching
    │   └── impl/
    │       ├── DefaultDataMapper.java         # Main implementation
    │       ├── MappingRegistry.java           # Converter/mapper/filter store
    │       ├── ReflectionMappingEngine.java   # Getter→setter reflection engine
    │       └── MappingException.java          # Unchecked runtime exception
    └── test/java/com/framework/mapping/
        └── test/
            ├── DataMapperTest.java            # 23 JUnit 5 tests
            ├── domain/
            │   ├── ProductData.java           # Service-layer domain object
            │   └── ProductWsDTO.java          # WS DTO with @FieldSetDefinition + custom sets
            └── mappers/
                ├── ProductPriceMapper.java    # CustomMapper example
                └── NullCollectionFilter.java  # FieldFilter example
```

---

## Running the Tests

```bash
mvn test
```

Expected output:

```
[INFO] Tests run: 23, Failures: 0, Errors: 0, Skipped: 0
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