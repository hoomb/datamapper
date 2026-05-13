package de.hoomit.mapping.test.domain;

import de.hoomit.mapping.annotation.FieldMapping;

/**
 * Test fixture: a WS DTO that maps to two different domain types
 * via repeated {@link FieldMapping} declarations on the same field.
 *
 * <ul>
 *   <li>{@code uid} → {@link RegisterData#getLogin() login}</li>
 *   <li>{@code uid} → {@link LegacyUserData#getUsername() username}</li>
 * </ul>
 */
public class UserSignUpWsDTO {

    @FieldMapping(pairedClass = RegisterData.class,   pairedField = "login")
    @FieldMapping(pairedClass = LegacyUserData.class, pairedField = "username")
    private String uid;

    private String password;
    private String firstName;
    private String lastName;

    public String getUid() {
        return uid;
    }

    public void setUid(final String uid) {
        this.uid = uid;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }
}
