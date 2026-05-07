package com.mayvel.objectViewer.route;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.tridium.json.JSONObject;
import com.mayvel.objectViewer.controller.BmsController;
import javax.baja.sys.BComponent;
import java.io.*;

public class BmsRoute {

    public static void registerRoutes(HttpServer server, BComponent parent) {
        BmsController controller = new BmsController();

        server.createContext("/getPoints", exchange -> handle(exchange, () -> {
            if (!AuthRoute.isTokenValid(exchange)) {
                JSONObject error = new JSONObject();
                error.put("status", "error");
                error.put("message", "Unauthorized");
                return error;
            }

            // Get query param
            String isSaved = getQueryParam(exchange, "isSaved"); // "true" or null
            return controller.getAllPoints(parent, isSaved);
        }));


        server.createContext("/updatePoint", exchange -> handle(exchange, () -> {
            if (!AuthRoute.isTokenValid(exchange)) {
                JSONObject error = new JSONObject();
                error.put("status", "error");
                error.put("message", "Unauthorized");
                return error;
            }
            return controller.updatePoint(exchange, parent);
        }));

        server.createContext("/api/start", exchange -> handle(exchange, () -> {
            if (!AuthRoute.isTokenValid(exchange)) {
                JSONObject error = new JSONObject();
                error.put("status", "error");
                error.put("message", "Unauthorized");
                return error;
            }
            return controller.startMeeting(exchange, parent);
        }));

        server.createContext("/api/stop", exchange -> handle(exchange, () -> {
            if (!AuthRoute.isTokenValid(exchange)) {
                JSONObject error = new JSONObject();
                error.put("status", "error");
                error.put("message", "Unauthorized");
                return error;
            }
            return controller.endMeeting(exchange, parent);
        }));

        server.createContext("/api/status", exchange -> handle(exchange, () -> {
            if (!AuthRoute.isTokenValid(exchange)) {
                JSONObject error = new JSONObject();
                error.put("status", "error");
                error.put("message", "Unauthorized");
                return error;
            }
            return controller.getRoomStatus(exchange, parent);
        }));

    }

    // common handler wrapper for error and response handling
    private static void handle(HttpExchange exchange, RouteAction action) throws IOException {
        try {
            JSONObject response = action.execute();
            sendJsonResponse(exchange, response);
        } catch (Exception e) {
            JSONObject error = new JSONObject();
            error.put("status", "error");
            error.put("message", e.getMessage());
            sendJsonResponse(exchange, error);
        }
    }

    private static void sendJsonResponse(HttpExchange exchange, JSONObject json) throws IOException {
        String response = json.toString();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    // functional interface for route logic
    @FunctionalInterface
    private interface RouteAction {
        JSONObject execute() throws Exception;
    }

    private static String getQueryParam(HttpExchange exchange, String key) {
        String query = exchange.getRequestURI().getQuery(); // e.g., "isSaved=true&other=val"
        if (query == null) return null;

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length == 2 && kv[0].equals(key)) {
                return kv[1];
            }
        }
        return null;
    }

}
