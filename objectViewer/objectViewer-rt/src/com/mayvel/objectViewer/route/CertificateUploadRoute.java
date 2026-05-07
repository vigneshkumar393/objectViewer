package com.mayvel.objectViewer.route;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.tridium.json.JSONObject;
import com.mayvel.objectViewer.controller.CertificateController;
import com.mayvel.objectViewer.BRestApiServerService;

import java.io.OutputStream;
import java.io.IOException;

public class CertificateUploadRoute {

    public static void registerRoutes(HttpServer server, BRestApiServerService service) {

        CertificateController controller = new CertificateController(service);

        server.createContext("/uploadCertificate", exchange -> handle(exchange, () -> {

            // ---------------------------
            // UPLOAD CERTIFICATE (POST)
            // ---------------------------

            // Optional token check
            if (!AuthRoute.isTokenValid(exchange)) {
                JSONObject error = new JSONObject();
                error.put("status", "error");
                error.put("message", "Unauthorized");
                return error;
            }

            // now works because controller returns JSONObject
            return controller.uploadCertificate(exchange);

        }));

        // ---------------------------
        // GET CERTIFICATE DETAILS (GET)
        // ---------------------------
        server.createContext("/getCertificateDetails", exchange -> handle(exchange, () -> {

            if (!AuthRoute.isTokenValid(exchange)) {
                JSONObject error = new JSONObject();
                error.put("status", "error");
                error.put("message", "Unauthorized");
                return error;
            }

            return controller.getCertificateDetails(exchange);

        }));

        // ---------------------------
        // VALIDATE CERTIFICATE (GET)
        // ---------------------------
        server.createContext("/validateCertificate", exchange -> handle(exchange, () -> {
            if (!AuthRoute.isTokenValid(exchange)) {
                JSONObject error = new JSONObject();
                error.put("status", "error");
                error.put("message", "Unauthorized");
                return error;
            }
            return controller.validateCertificate(exchange);
        }));

        // ---------------------------
        // UPGRADE TO HTTPS (POST)
        // ---------------------------
        server.createContext("/upgradeToHttps", exchange -> handle(exchange, () -> {

            if (!AuthRoute.isTokenValid(exchange)) {
                JSONObject error = new JSONObject();
                error.put("status", "error");
                error.put("message", "Unauthorized");
                return error;
            }

            // Call the controller method to upgrade
            return controller.upgradeToHttps(exchange);

        }));

    }

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
        byte[] data = json.toString().getBytes();

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, data.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    @FunctionalInterface
    private interface RouteAction {
        JSONObject execute() throws Exception;
    }
}
