package de.hoomit.mapping.test.domain;

import de.hoomit.mapping.fieldset.FieldSetDefinition;
import de.hoomit.mapping.fieldset.NamedFieldSet;

/**
 * Test fixture: custom name "BASIC" shadows the reserved built-in level.
 */
@FieldSetDefinition(
        basic = {"code"},
        defaults = {"name"},
        custom = {
                @NamedFieldSet(name = "BASIC", fields = {"code", "name"})   // illegal: shadows built-in
        }
)
public class ReservedNameDTO {
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
