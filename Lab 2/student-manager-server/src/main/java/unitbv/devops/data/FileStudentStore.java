package unitbv.devops.data;

import unitbv.devops.models.Student;
import unitbv.devops.ports.StudentStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * File-backed implementation of StudentStore.
 * Each student is stored as a single line in UTF-8: "FirstName;LastName".
 * Thread-safe and uses atomic writes to avoid data corruption.
 */
public class FileStudentStore implements StudentStore {
    private final Path file;
    private final Object lock = new Object();

    public FileStudentStore(Path file) throws IOException {
        this.file = file;
        initFile();
    }

    /** Ensure file and parent directory exist. */
    private void initFile() throws IOException {
        synchronized (lock) {
            Files.createDirectories(file.getParent());
            if (Files.notExists(file)) {
                Files.writeString(file, "", StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            }
        }
    }

    /** Return all students currently stored. */
    @Override
    public List<Student> list() throws IOException {
        synchronized (lock) {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            List<Student> out = new ArrayList<>(lines.size());
            for (String line : lines) {
                Student s = Student.fromFileLine(line);
                if (s != null) out.add(s);
            }
            return out;
        }
    }

    /** Count the number of stored students. */
    @Override
    public int count() throws IOException {
        return list().size();
    }

    /**
     * Add a batch of student names.
     * Each string is expected in "FirstName LastName" format.
     * Lines are trimmed; empty lines are ignored.
     */
    @Override
    public void addAll(List<String> names) throws IOException {
        if (names == null || names.isEmpty()) return;
        synchronized (lock) {
            List<Student> current = list();
            for (String n : names) {
                if (n == null) continue;
                String trimmed = n.trim();
                if (trimmed.isEmpty()) continue;

                // Split into first and last by first space
                String[] parts = trimmed.split("\\s+", 2);
                String first = parts.length > 0 ? parts[0] : "";
                String last  = parts.length > 1 ? parts[1] : "";
                if (!first.isEmpty()) {
                    current.add(new Student(first, last));
                }
            }
            writeAllAtomic(current);
        }
    }

    /** Delete a student by index (0-based). */
    @Override
    public void deleteByIndex(int index) throws IOException {
        synchronized (lock) {
            List<Student> current = list();
            if (index < 0 || index >= current.size()) {
                throw new IndexOutOfBoundsException("index out of range");
            }
            current.remove(index);
            writeAllAtomic(current);
        }
    }

    /** Clear all students. */
    @Override
    public void clear() throws IOException {
        synchronized (lock) {
            writeAllAtomic(List.of());
        }
    }

    /** Write all students atomically to disk. */
    private void writeAllAtomic(List<Student> students) throws IOException {
        Path tmp = Files.createTempFile(file.getParent(), "students-", ".tmp");
        try {
            StringBuilder sb = new StringBuilder();
            for (Student s : students) {
                sb.append(s.toFileLine()).append('\n');
            }
            Files.writeString(tmp, sb.toString(), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            try {
                Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
        }
    }
}
