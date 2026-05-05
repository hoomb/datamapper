package de.hoomit.mapping.test.domain;

import de.hoomit.mapping.fieldset.FieldSetDefinition;

/**
 * Test fixture: references a set that does not exist.
 */
@FieldSetDefinition(
        basic = {"code"},
        defaults = {"@DOESNOTEXIST", "name"}    // unknown reference
)
public class UnknownRefDTO {
    private String code;
    private String name;

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
}
