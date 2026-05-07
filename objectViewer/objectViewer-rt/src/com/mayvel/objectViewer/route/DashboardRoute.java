package com.mayvel.objectViewer.route;
import com.mayvel.objectViewer.controller.DashboardController;
import com.mayvel.objectViewer.utils.Logger;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.tridium.json.JSONObject;
import javax.baja.sys.BComponent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DashboardRoute {

    public static void registerRoutes(HttpServer server, BComponent parent) {
        DashboardController controller = new DashboardController();

        server.createContext("/getDashboard", exchange -> handle(exchange, () -> {
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

        server.createContext("/getHistories", exchange -> handle(exchange, () -> {
            if (!AuthRoute.isTokenValid(exchange)) {
                JSONObject error = new JSONObject();
                error.put("status", "error");
                error.put("message", "Unauthorized");
                return error;
            }

            Map<String, String> queryParams = parseQuery(exchange.getRequestURI().getQuery());
            String limit = queryParams.get("limit");
            String offset = queryParams.get("offset");
            boolean firstAndLastOnly = Boolean.parseBoolean(queryParams.getOrDefault("firstAndLastOnly", "false"));
            String filterValues = queryParams.get("filterValues");

            String requestBody = readBody(exchange);
            JSONObject jsonBody = new JSONObject(requestBody);

            String startTime = jsonBody.optString("startTime", "");
            String endTime = jsonBody.optString("endTime", "");
            String historySourcePath = jsonBody.optString("historySource", "");

            Map<String, Object> responseMap;
            if (startTime.equals("")) {

                responseMap = controller.GetAllHistoryFromDB(startTime, endTime, limit, offset,
                        historySourcePath, filterValues, firstAndLastOnly);
            } else {
                Logger.Log("11 GetAllHistory log");
                responseMap = controller.GetAllHistory(startTime, endTime, limit, offset,
                        historySourcePath, filterValues, firstAndLastOnly);
            }

            return new JSONObject(responseMap);
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

    private static String readBody(HttpExchange exchange) throws IOException {
        InputStream input = exchange.getRequestBody();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }
        return new String(baos.toByteArray(), StandardCharsets.UTF_8);
    }


    private static Map<String, String> parseQuery(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null || query.isEmpty()) return result;

        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            result.put(entry[0], entry.length > 1 ? entry[1] : "");
        }
        return result;
    }

}