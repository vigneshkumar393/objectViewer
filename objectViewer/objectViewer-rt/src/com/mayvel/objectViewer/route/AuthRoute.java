package com.mayvel.objectViewer.route;

import com.mayvel.objectViewer.controller.AuthController;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.tridium.json.JSONObject;
import javax.baja.sys.BComponent;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Scanner;

public class AuthRoute {

    private static final AuthController authController = new AuthController();

    public static void registerRoutes(HttpServer server, BComponent serviceComp) {
        // Login endpoint
        server.createContext("/login", exchange -> handle(exchange, () -> login(exchange, serviceComp)));
    }

    private static JSONObject login(HttpExchange exchange, BComponent serviceComp) throws IOException {
        Scanner s = new Scanner(exchange.getRequestBody()).useDelimiter("\\A");
        String body = s.hasNext() ? s.next() : "";
        JSONObject reqJson = new JSONObject(body);

        String username = reqJson.optString("username", null);
        String password = reqJson.optString("password", null);

        return authController.login(serviceComp, username, password);
    }

    // Common handler wrapper
    private static void handle(HttpExchange exchange, RouteAction action) throws IOException {
        JSONObject response;
        try {
            response = action.execute();
        } catch (Exception e) {
            response = new JSONObject();
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        String respText = response.toString();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, respText.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(respText.getBytes());
        }
    }

    @FunctionalInterface
    private interface RouteAction {
        JSONObject execute() throws Exception;
    }

    // Utility method to validate token from Bearer header
    public static boolean isTokenValid(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring("Bearer ".length());
            return authController.validateToken(token);
        }
        return false;
    }
}
