package de.hoomit.mapping.test.domain;

import de.hoomit.mapping.annotation.FieldMapping;

/**
 * Test fixture: verifies the symmetric-lookup behaviour of
 * {@link FieldMapping}.
 *
 * <p>Unlike {@link UserSignUpWsDTO} (which declares its renames on the
 * source side), {@code AuditEvent} declares the rename on the
 * <em>destination</em> side. The framework must still pick this up when
 * mapping {@code UserSignUpWsDTO → AuditEvent}, producing the rename
 * {@code uid → actorId}.
 */
public class AuditEvent {

    @FieldMapping(pairedClass = UserSignUpWsDTO.class, pairedField = "uid")
    private String actorId;

    public String getActorId() {
        return actorId;
    }

    public void setActorId(final String actorId) {
        this.actorId = actorId;
    }
}
