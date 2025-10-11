package unitbv.devops.ports;

import unitbv.devops.models.Student;
import java.io.IOException;
import java.util.List;

/**
 * Abstraction for any Student data storage mechanism.
 * <p>
 * Implementations can be file-based, in-memory, or database-backed.
 * <p>
 * All methods may throw IOException since storage involves I/O operations.
 */
public interface StudentStore {

    /**
     * Returns all students currently stored.
     *
     * @return list of students (never null, may be empty)
     * @throws IOException if file read fails
     */
    List<Student> list() throws IOException;

    /**
     * Returns the total number of students.
     *
     * @return number of stored students
     * @throws IOException if file read fails
     */
    int count() throws IOException;

    /**
     * Adds multiple students at once.
     * <p>
     * Each entry in {@code names} should represent one line of input
     * (for example, "Ana Popescu" or "Bogdan Ionescu").
     *
     * @param names list of lines to add (each containing first and last name)
     * @throws IOException if file write fails
     */
    void addAll(List<String> names) throws IOException;

    /**
     * Deletes a student at a given index (0-based).
     *
     * @param index index of the student to remove
     * @throws IOException if file write fails
     * @throws IndexOutOfBoundsException if index invalid
     */
    void deleteByIndex(int index) throws IOException;

    /**
     * Removes all students from the store.
     *
     * @throws IOException if file write fails
     */
    void clear() throws IOException;
}
