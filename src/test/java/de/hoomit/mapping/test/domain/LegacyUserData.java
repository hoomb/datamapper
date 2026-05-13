package de.hoomit.mapping.test.domain;

/**
 * Test fixture: a second domain target for {@link UserSignUpWsDTO}, used to
 * verify that a single field can declare multiple {@code @FieldMapping}
 * annotations and that each is selected based on the {@code pairedClass}
 * argument at mapping time.
 *
 * <p>Maps {@code UserSignUpWsDTO.uid} to {@code LegacyUserData.username}.
 */
public class LegacyUserData {

    private String username;
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }
}
