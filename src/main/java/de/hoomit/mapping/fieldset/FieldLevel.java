package de.hoomit.mapping.fieldset;

/**
 * Defines the verbosity levels for field sets.
 *
 * <ul>
 *   <li>{@link #BASIC}   – minimal fields, e.g. id + name only</li>
 *   <li>{@link #DEFAULT} – typical API response payload</li>
 *   <li>{@link #FULL}    – every available field</li>
 * </ul>
 * <p>
 * A higher level always includes all fields from lower levels.
 */
public enum FieldLevel {
    BASIC(0),
    DEFAULT(1),
    FULL(2);

    private final int rank;

    FieldLevel(int rank) {
        this.rank = rank;
    }

    public boolean includes(FieldLevel other) {
        return this.rank >= other.rank;
    }
}
