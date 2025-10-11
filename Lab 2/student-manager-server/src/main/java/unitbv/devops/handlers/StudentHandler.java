package unitbv.devops.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import unitbv.devops.models.Student;
import unitbv.devops.ports.StudentStore;
import unitbv.devops.util.HttpUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles /students
 * - GET  /students       -> list students with indices (text/plain)
 * - POST /students       -> add students (text/plain body; one per line: "FirstName LastName")
 * <p>
 * DELETE and other methods are intentionally left for students to implement later.
 */
public class StudentHandler implements HttpHandler {
    private final StudentStore store;

    public StudentHandler(StudentStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod().toUpperCase();
        switch (method) {
            case "GET" -> handleGet(ex);
            case "POST" -> handlePost(ex);
            default -> {
                ex.getResponseHeaders().add("Allow", "GET, POST");
                ex.sendResponseHeaders(405, -1); // Method Not Allowed
                ex.close();
            }
        }
        System.out.println("[/students " + method + "] on " + Thread.currentThread().getName());
    }

    private void handleGet(HttpExchange ex) throws IOException {
        List<Student> students = store.list();

        StringBuilder sb = new StringBuilder();
        sb.append("# Students (").append(students.size()).append(")\n");
        if (students.isEmpty()) {
            sb.append("(empty)\n");
        } else {
            for (int i = 0; i < students.size(); i++) {
                Student s = students.get(i);
                sb.append(i).append(": ")
                        .append(s.firstName()).append(" ").append(s.lastName())
                        .append("\n");
            }
        }

        byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(200, body.length);
        ex.getResponseBody().write(body);
        ex.close();
    }

    private void handlePost(HttpExchange ex) throws IOException {
        // Expecting text/plain, one student per line: "FirstName LastName"
        String raw = HttpUtils.readBody(ex);

        // Split into lines, trim, ignore blanks; validate at least first name.
        String[] lines = raw.replace("\r\n", "\n").split("\n");
        List<String> valid = new ArrayList<>();
        for (String line : lines) {
            if (line == null) continue;
            String t = line.trim();
            if (!t.isEmpty()) valid.add(t);
        }

        if (valid.isEmpty()) {
            HttpUtils.sendText(ex, 400, "ERROR: request body is empty or invalid\n");
            return;
        }

        store.addAll(valid);
        HttpUtils.sendText(ex, 200, "ADDED " + valid.size() + "\n");
    }
}
