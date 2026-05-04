package de.hoomit.mapping.test.mappers;

import de.hoomit.mapping.annotation.WsDTOMapping;
import de.hoomit.mapping.context.MappingContext;
import de.hoomit.mapping.filter.FieldFilter;

import java.util.Collection;

/**
 * A {@link FieldFilter} that excludes null or empty collection fields from
 * being written to the destination object.
 */
@WsDTOMapping
public class NullCollectionFilter implements FieldFilter {

    @Override
    public boolean isApplicable(Class<?> sourceType, Class<?> destType) {
        return true; // applies globally
    }

    @Override
    public boolean include(String fieldName, Object value, MappingContext ctx) {
        if (value instanceof Collection<?> col) {
            return !col.isEmpty();
        }
        return true;
    }
}
