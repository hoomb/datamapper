package de.hoomit.mapping.test.domain;

import de.hoomit.mapping.fieldset.FieldSetDefinition;

import static de.hoomit.mapping.fieldset.FieldSets.BASIC;
import static de.hoomit.mapping.fieldset.FieldSets.DEFAULT;

/**
 * Test fixture: BASIC → DEFAULT → BASIC. Used to verify cycle detection.
 */
@FieldSetDefinition(
        basic = {DEFAULT, "code"},   // BASIC depends on DEFAULT
        defaults = {BASIC, "name"}      // DEFAULT depends on BASIC  → cycle
)
public class CyclicFieldSetDTO {
    private String code;
    private String name;

    public String getCode() {
        return code;
    }

    public void setCode(final String v) {
        code = v;
    }

    public String getName() {
        return name;
    }

    public void setName(final String v) {
        name = v;
    }
}
