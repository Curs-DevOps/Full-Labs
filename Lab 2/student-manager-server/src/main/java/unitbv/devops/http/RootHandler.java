package unitbv.devops.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class RootHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.getResponseHeaders().add("Allow", "GET");
            ex.sendResponseHeaders(405, -1); // Method Not Allowed
            ex.close();
            return;
        }

        String body = """
                # Student Manager (Lab 2)
                This is a simple HTTP server (no REST yet).

                Available endpoints (planned):
                - GET  /students        -> list students with indices
                - POST /students        -> add students (text/plain, one per line: "First Last")
                - GET  /count           -> total number of students
                - DELETE /students      -> clear all, or ?i=<index> to delete one

                Try these in your browser or curl:
                - http://localhost:8080/students
                """;

        // Return as text/plain so it’s visible in terminal/curl
        ex.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(200, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();

        System.out.println("[/] GET on " + Thread.currentThread().getName());
    }
}
