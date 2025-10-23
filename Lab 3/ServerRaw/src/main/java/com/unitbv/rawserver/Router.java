package com.unitbv.rawserver;

import java.util.HashMap;
import java.util.Map;

public class Router {
    private Map<String, Handler> routes = new HashMap<>();

    public void get(String uri, Handler handler) {
        routes.put("GET_" + uri, handler);
    }

    public Handler find(String method, String uri) {
        return routes.getOrDefault(method + "_" + uri, null);
    }
}
