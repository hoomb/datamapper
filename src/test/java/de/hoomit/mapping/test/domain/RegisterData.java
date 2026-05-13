package de.hoomit.mapping.test.domain;

/**
 * Test fixture: domain-side counterpart to {@link UserSignUpWsDTO}.
 *
 * <p>Note that this class carries no {@link de.hoomit.mapping.annotation.FieldMapping}
 * annotations of its own — the rename ({@code uid} ↔ {@code login}) is declared
 * on the DTO side. The framework's symmetric lookup handles both directions
 * from that single declaration.
 */
public class RegisterData {

    private String login;
    private String password;
    private String firstName;
    private String lastName;

    public String getLogin() {
        return login;
    }

    public void setLogin(final String login) {
        this.login = login;
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
