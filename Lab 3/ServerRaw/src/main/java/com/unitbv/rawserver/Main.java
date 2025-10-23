package com.unitbv.rawserver;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class Main {
    public static Router router = new Router();

    public static void main(String[] args) throws Exception {
        router.get("/", (request) ->
                new Response(
                        200, "text/plain", "Hello from Java HTTP Server!"));

        ServerSocket server = new ServerSocket(8080);
        ExecutorService pool = Executors.newFixedThreadPool(4);
        System.out.println("Server running on http://localhost:8080");

        while (true) {
            Socket client = server.accept(); // waits for connection
            pool.execute(() -> {
                try {
                    handle(client);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }); // handle in thread pool
        }
    }

    static void handle(Socket client) throws IOException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintWriter out = new PrintWriter(client.getOutputStream())) {

            Request request = Request.fromString(in);
            if (request == null) {
                throw new IOException("Invalid request");
            }

            Handler handler = router.find(request.getMethod(), request.getUri());

            Response response;
            if (handler == null) {
                response = new Response(405, "text/plain", "Method Not Allowed");
            } else {
                response = handler.handle(request);
            }

            out.println("HTTP/1.1 " + response.getStatus());
            out.println("Content-Type: " + response.getContentType());
            out.println();

            if (response.getBody() != null) {
                out.println(response.getBody());
            }
            out.flush();
        }
    }
}
