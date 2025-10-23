package com.unitbv.rawserver;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Request {
    private String method;
    private String uri;
    private Map<String, String> headers;
    private Map<String, String> params;
    private String body;

    public Request(String method, String uri, Map<String, String> headers, Map<String, String> params, String body) {
        this.method = method;
        this.uri = uri;
        this.headers = headers;
        this.params = params;
        this.body = body;
    }

    //TODO https://codeshare.io/5R6jRB

    public static Request fromString(BufferedReader in) throws IOException {
            String line = in.readLine(); // first line = request

            String[] parts = line.split(" ");
            String method = parts[0];
            String uri = parts[1];

            Map<String, String> params = new HashMap<>();
            if (uri.contains("?")) {
                String query = uri.substring(uri.indexOf("?") + 1);
                for (String param : query.split("&")) {
                    String[] paramParts = param.split("=");
                    params.put(paramParts[0], paramParts[1]);
                }
                uri = uri.substring(0, uri.indexOf("?"));
            }

            Map<String, String> headers = new HashMap<>();
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                String[] headerParts = line.split(": ");
                headers.put(headerParts[0], headerParts[1]);
            }

            // If method is GET or HEAD, there's no body
            if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) {
                return new Request(method, uri, headers, params, "");
            }

            // Check if there is a Content-Length header
            String contentLength = headers.get("Content-Length");
            if (contentLength != null && Integer.parseInt(contentLength) > 0) {
                // Read the body if Content-Length is present and greater than 0
                StringBuilder sb = new StringBuilder();
                int bodyLength = Integer.parseInt(contentLength);
                char[] buffer = new char[1024];
                int bytesRead;
                while (bodyLength > 0 && (bytesRead = in.read(buffer, 0, Math.min(buffer.length, bodyLength))) != -1) {
                    sb.append(buffer, 0, bytesRead);
                    bodyLength -= bytesRead;
                }
                return new Request(method, uri, headers, params, sb.toString());
            }

            return null;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "Request{" +
                "method='" + method + '\'' +
                ", uri='" + uri + '\'' +
                ", headers=" + headers +
                ", params=" + params +
                ", body='" + body + '\'' +
                '}';
    }
}
