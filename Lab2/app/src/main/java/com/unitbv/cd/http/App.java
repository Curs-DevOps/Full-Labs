package com.unitbv.cd.http;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class App {
    public static void main(String[] args) throws Exception {
        try (ServerSocket server = new ServerSocket(8081)) {
            ExecutorService pool = Executors.newFixedThreadPool(4);
            System.out.println("Server running on http://localhost:8081");
            while (true) {
                Socket client = server.accept();
                pool.execute(() -> handle(client));
            }
        }
    }

    static void handle(Socket client) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()))) {
            PrintWriter out = new PrintWriter(client.getOutputStream());
            String line = in.readLine(); // first line = request
            System.out.println("Request: " + line);
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/plain");
            out.println();
            out.println("Hello from Java HTTP Server!");
            out.flush();
        } catch (IOException e) { e.printStackTrace(); }
    }
}