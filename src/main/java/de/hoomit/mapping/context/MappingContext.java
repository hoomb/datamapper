package de.hoomit.mapping.context;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Carries all context information for a single mapping operation.
 *
 * <p>Use the {@link Builder} for immutable construction:
 * <pre>
 *   MappingContext ctx = MappingContext.builder()
 *       .fieldSetName("FULL")
 *       .mapNulls(false)
 *       .build();
 * </pre>
 * </p>
 */
public final class MappingContext {
    public static final String FIELDSET_DEFAULT = "DEFAULT";
    public static final String FIELDSET_FULL = "FULL";

    // -------------------------------------------------------------------------

    private final String fieldSetName;
    private final boolean mapNulls;
    private final String fieldPrefix;
    private final Set<String> resolvedFields;   // lazily populated by DefaultDataMapper
    private final Map<String, Object> extras;   // extension map for custom attributes

    private MappingContext(final Builder builder) {
        this.fieldSetName = builder.fieldSetName;
        this.mapNulls = builder.mapNulls;
        this.fieldPrefix = builder.fieldPrefix;
        this.resolvedFields = builder.resolvedFields;
        this.extras = Map.copyOf(builder.extras);
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public String getFieldSetName() {
        return fieldSetName;
    }

    public boolean isMapNulls() {
        return mapNulls;
    }

    public String getFieldPrefix() {
        return fieldPrefix;
    }

    public Set<String> getResolvedFields() {
        return resolvedFields;
    }

    /**
     * Retrieve a custom extension attribute.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(final String key) {
        return (T) extras.get(key);
    }

    /**
     * Check if a field should be included in this mapping pass.
     */
    public boolean includes(final String fieldName) {
        if (resolvedFields == null || resolvedFields.isEmpty()) {
            return true;
        }

        final String prefixed = (fieldPrefix != null && !fieldPrefix.isBlank())
                ? fieldPrefix + "." + fieldName
                : fieldName;
        return resolvedFields.contains(fieldName) || resolvedFields.contains(prefixed);
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Convenience: DEFAULT field set, no null mapping.
     */
    public static MappingContext defaults() {
        return builder().fieldSetName(FIELDSET_DEFAULT).build();
    }

    /**
     * Convenience: FULL field set, map nulls.
     */
    public static MappingContext full() {
        return builder().fieldSetName(FIELDSET_FULL).mapNulls(true).build();
    }

    public static final class Builder {
        private String fieldSetName = FIELDSET_DEFAULT;
        private boolean mapNulls = false;
        private String fieldPrefix = "";
        private Set<String> resolvedFields = Collections.emptySet();
        private final Map<String, Object> extras = new HashMap<>();

        public Builder fieldSetName(final String fieldSetName) {
            this.fieldSetName = fieldSetName;
            return this;
        }

        public Builder mapNulls(final boolean mapNulls) {
            this.mapNulls = mapNulls;
            return this;
        }

        public Builder fieldPrefix(final String fieldPrefix) {
            this.fieldPrefix = fieldPrefix;
            return this;
        }

        public Builder resolvedFields(final Set<String> resolvedFields) {
            this.resolvedFields = resolvedFields;
            return this;
        }

        public Builder extra(final String key, final Object val) {
            this.extras.put(key, val);
            return this;
        }

        public MappingContext build() {
            return new MappingContext(this);
        }
    }

    @Override
    public String toString() {
        return "MappingContext{fieldSet='" + fieldSetName + '\''
                + ", mapNulls=" + mapNulls
                + ", prefix='" + fieldPrefix + '\''
                + '}';
    }
}
