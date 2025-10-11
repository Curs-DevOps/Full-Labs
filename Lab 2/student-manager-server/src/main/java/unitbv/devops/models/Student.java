package unitbv.devops.models;

import java.util.Objects;

/**
 * Immutable domain model representing a student.
 * Each student has a first and last name.
 */
public final class Student {
    private final String firstName;
    private final String lastName;

    public Student(String firstName, String lastName) {
        this.firstName = Objects.requireNonNull(firstName, "firstName must not be null").trim();
        this.lastName  = Objects.requireNonNull(lastName,  "lastName must not be null").trim();
    }

    public String firstName() { return firstName; }
    public String lastName()  { return lastName; }

    @Override
    public String toString() {
        return firstName + " " + lastName;
    }

    /** Serialize to file line, using ';' separator (e.g. Ana;Popescu). */
    public String toFileLine() {
        return firstName + ";" + lastName;
    }

    /** Parse a line from file ("Ana;Popescu" -> new Student("Ana", "Popescu")). */
    public static Student fromFileLine(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }

        String[] parts = line.split(";", 2);
        String first = parts.length > 0 ? parts[0].trim() : "";
        String last  = parts.length > 1 ? parts[1].trim() : "";

        if (first.isEmpty() && last.isEmpty()) {
            return null;
        }

        return new Student(first, last);
    }
}
