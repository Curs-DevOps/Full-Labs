package com.unitbv.cd.http;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class Rest {
    private static volatile boolean running = true; // flag to control loop
    private static ServerSocket server;
    private static ExecutorService pool;

    public static void main(String[] args) throws IOException {
        int port = 8080;
        server = new ServerSocket(port);
        pool = Executors.newFixedThreadPool(4);
        log("Server running on http://localhost:" + port);

        // --- Graceful Shutdown Hook ---
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log("Shutdown requested. Stopping server...");
            running = false;
            try {
                server.close(); // stop accepting new clients
            } catch (IOException ignored) {}
            pool.shutdown(); // stop thread pool gracefully
            try {
                if (!pool.awaitTermination(3, TimeUnit.SECONDS)) {
                    log("Forcing shutdown...");
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                pool.shutdownNow();
            }
            log("Server stopped.");
        }));

        // --- Main Loop ---
        while (running) {
            try {
                Socket client = server.accept();
                pool.execute(() -> handle(client));
            } catch (SocketException e) {
                if (running) log("Socket exception: " + e.getMessage());
                // Break loop only if shutting down
                break;
            }
        }
    }

    static void handle(Socket client) {
        try (client;
             BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintWriter out = new PrintWriter(client.getOutputStream())) {

            // --- Parse the Request ---
            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) return;

            String[] parts = requestLine.split(" ");
            if (parts.length < 3) {
                sendResponse(out, 400, "Bad Request", "Malformed HTTP request");
                log("400 Bad Request");
                return;
            }

            String method = parts[0];
            String path = parts[1];
            log("Incoming Request -> " + method + " " + path);

            // --- Basic Routing ---
            switch (method) {
                case "GET" -> handleGet(path, out);
                case "POST" -> handlePost(path, in, out);
                default -> {
                    sendResponse(out, 405, "Method Not Allowed", "Supported: GET, POST");
                    log("405 Method Not Allowed for " + method);
                }
            }

        } catch (IOException e) {
            log("Error: " + e.getMessage());
        }
    }

    static void handleGet(String path, PrintWriter out) {
        if ("/".equals(path)) {
            sendResponse(out, 200, "OK", "Welcome to the RESTful Java Server!");
        } else if ("/ping".equals(path)) {
            sendResponse(out, 200, "OK", "pong");
        } else {
            sendResponse(out, 404, "Not Found", "The resource " + path + " was not found.");
        }
    }

    static void handlePost(String path, BufferedReader in, PrintWriter out) throws IOException {
        // Read headers
        String line;
        int contentLength = 0;
        while (!(line = in.readLine()).isEmpty()) {
            if (line.startsWith("Content-Length:")) {
                contentLength = Integer.parseInt(line.split(":")[1].trim());
            }
        }

        char[] body = new char[contentLength];
        if (contentLength > 0) in.read(body);

        sendResponse(out, 200, "OK", "Received POST data: " + new String(body));
    }

    static void sendResponse(PrintWriter out, int statusCode, String statusText, String body) {
        out.println("HTTP/1.1 " + statusCode + " " + statusText);
        out.println("Content-Type: text/plain; charset=utf-8");
        out.println("Content-Length: " + body.length());
        out.println();
        out.print(body);
        out.flush();

        log("Response -> " + statusCode + " " + statusText);
    }

    static void log(String message) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        System.out.println("[" + timestamp + "] " + message);
    }
}
