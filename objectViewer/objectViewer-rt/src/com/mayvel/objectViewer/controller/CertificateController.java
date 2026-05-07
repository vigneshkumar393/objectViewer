package com.mayvel.objectViewer.controller;

import com.mayvel.objectViewer.BRestApiServerService;
import com.sun.net.httpserver.HttpExchange;
import com.tridium.json.JSONObject;
import javax.baja.sys.Sys;
import java.io.*;

public class CertificateController {

    private final BRestApiServerService service;

    public CertificateController(BRestApiServerService service) {
        this.service = service;
    }

    // --------------------------
    // UPLOAD P12 FILE + PASSWORD
    // --------------------------
    public JSONObject uploadCertificate(HttpExchange exchange) {

        try {

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                return error("Only POST allowed");
            }

            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.contains("multipart/form-data"))
                return error("Content-Type must be multipart/form-data");

            String boundary = "--" + contentType.split("boundary=")[1];

            InputStream is = exchange.getRequestBody();
            BufferedInputStream bis = new BufferedInputStream(is);

            ByteArrayOutputStream fileBuffer = null;
            String fileName = null;
            String password = null;
            boolean capturingFile = false;

            ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();
            int b;

            while ((b = bis.read()) != -1) {

                lineBuffer.write(b);

                if (b == '\n') {
                    String line = lineBuffer.toString("ISO-8859-1");

                    // -------------------------
                    // Boundary → stop file capture
                    // -------------------------
                    if (line.startsWith(boundary)) {
                        capturingFile = false;
                        lineBuffer.reset();
                        continue;
                    }

                    // -------------------------
                    // File header
                    // -------------------------
                    if (line.contains("filename=")) {
                        fileName = extractFilename(line);
                        fileBuffer = new ByteArrayOutputStream();
                        capturingFile = false;
                        lineBuffer.reset();
                        continue;
                    }

                    // -------------------------
                    // Content-Type of file
                    // -------------------------
                    if (line.contains("Content-Type:")) {
                        bis.read(); // \r
                        bis.read(); // \n
                        capturingFile = true;
                        lineBuffer.reset();
                        continue;
                    }

                    // -------------------------
                    // Password field
                    // -------------------------
                    if (line.contains("name=\"password\"")) {
                        bis.read(); // \r
                        bis.read(); // \n

                        StringBuilder sb = new StringBuilder();
                        int c;
                        while ((c = bis.read()) != '\r') {
                            sb.append((char) c);
                        }
                        bis.read(); // \n

                        password = sb.toString().trim();
                        lineBuffer.reset();
                        continue;
                    }

                    // -------------------------
                    // Capture raw binary file data
                    // -------------------------
                    if (capturingFile && fileBuffer != null) {
                        byte[] arr = lineBuffer.toByteArray();
                        fileBuffer.write(arr, 0, arr.length);
                    }

                    lineBuffer.reset();
                }
            }

            // -----------------------------
            // VALIDATION
            // -----------------------------
            if (fileName == null) return error("File missing");
            if (password == null) return error("Password missing");

            if (!fileName.endsWith(".p12") && !fileName.endsWith(".pfx"))
                return error("Only .p12 or .pfx allowed");

            // -----------------------------
            // Save certificate
            // -----------------------------
            File certDir = new File(Sys.getStationHome(), "niagara_certificate");
            certDir.mkdirs();

            File outFile = new File(certDir, fileName);

            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                fos.write(fileBuffer.toByteArray());
            } catch (Exception e) {
                return error("Failed to save certificate: " + e.getMessage());
            }

            // Save password
            try {
                service.setCertificatePassword(password);
                service.setCertificateFileName(fileName);
                // IMPORTANT → persist to station file
                Sys.getStation().save();
            } catch (Exception e) {
                return error("Failed to save password: " + e.getMessage());
            }

            // SUCCESS
            JSONObject ok = new JSONObject();
            ok.put("status", "success");
            ok.put("message", "Certificate uploaded successfully");
            ok.put("file", fileName);
            return ok;

        } catch (Exception e) {
            // ANY unexpected crash
            return error("Upload failed: " + e.getMessage());
        }
    }

    // ------------------------------
// GET CERTIFICATE DETAILS
// ------------------------------
    public JSONObject getCertificateDetails(HttpExchange exchange) {

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            return error("Only GET allowed");
        }

        JSONObject res = new JSONObject();

        String fileName = service.getCertificateFileName();
        String password = service.getCertificatePassword();

        // Folder path
        File certDir = new File(Sys.getStationHome(), "niagara_certificate");

        File certFile = null;
        boolean exists = false;

        if (fileName != null && !fileName.isEmpty()) {
            certFile = new File(certDir, fileName);
            exists = certFile.exists();
        }

        res.put("status", "success");
        res.put("fileName", fileName == null ? "" : fileName);
        res.put("fileExists", exists);

        return res;
    }

    public JSONObject validateCertificate(HttpExchange exchange) {
        try {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                return error("Only GET allowed");
            }

            String fileName = service.getCertificateFileName();
            String password = service.getCertificatePassword();

            if (fileName == null || fileName.isEmpty()) {
                return error("Certificate file not set");
            }
            if (password == null || password.isEmpty()) {
                return error("Certificate password not set");
            }

            // Call service method to validate
            String result = service.validateCertificate(fileName, password);

            JSONObject res = new JSONObject();
            res.put("status", result.startsWith("valid") ? "success" : "error");
            res.put("message", result);

            return res;

        } catch (Exception e) {
            return error("Validation failed: " + e.getMessage());
        }
    }

    // ------------------------------
// UPGRADE SERVER TO HTTPS
// ------------------------------
    public JSONObject upgradeToHttps(HttpExchange exchange) {
        try {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                return error("Only POST allowed");
            }

            // Validate certificate first
            String validationResult = service.validateCertificate(service.getCertificateFileName(),
                    service.getCertificatePassword());

            if (!"valid".equals(validationResult)) {
                return error("HTTPS upgrade failed: Certificate validation failed: " + validationResult);
            }

            // ✅ Respond to client before stopping server
            JSONObject res = new JSONObject();
            res.put("status", "success");
            res.put("message", "Certificate is valid. HTTPS upgrade will start shortly...");
            sendJsonResponse(exchange, res);  // immediate response

            // Restart server asynchronously
            new Thread(() -> {
                try {
                    System.out.println("Stopping HTTP server...");
                    service.stopServer();

                    System.out.println("Starting HTTPS server...");
                    service.startHttpsServer();

                    System.out.println("HTTPS enabled successfully.");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            return null; // already sent response

        } catch (Exception e) {
            return error("HTTPS upgrade failed: " + e.getMessage());
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




    private String extractFilename(String header) {
        try {
            int idx = header.indexOf("filename=\"");
            if (idx < 0) return null;
            String sub = header.substring(idx + 10);
            return sub.substring(0, sub.indexOf("\""));
        } catch (Exception e) {
            return null;
        }
    }

    private JSONObject error(String msg) {
        JSONObject o = new JSONObject();
        o.put("status", "error");
        o.put("message", msg);
        return o;
    }

}
