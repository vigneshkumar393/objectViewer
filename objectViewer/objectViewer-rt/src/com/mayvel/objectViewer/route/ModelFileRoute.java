package com.mayvel.objectViewer.route;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import javax.baja.naming.BOrd;
import javax.baja.file.BIFile;

import java.io.*;
import java.net.URLDecoder;

public class ModelFileRoute {

    public static void register(HttpServer server) {
        server.createContext("/getModel", new ModelHandler());
    }

    static class ModelHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {

            try {
                String query = exchange.getRequestURI().getQuery();

                if (query == null || !query.startsWith("path=")) {
                    exchange.sendResponseHeaders(400, -1);
                    return;
                }

                String relativePath = URLDecoder.decode(query.replace("path=", ""), "UTF-8");

                System.out.println("👉 Loading: " + relativePath);

                InputStream is = null;

                try {
                    // 🔹 Try Niagara File (BIFile)
                    BOrd ord = BOrd.make("file:^" + relativePath);
                    Object obj = ord.resolve().get();

                    if (obj instanceof BIFile) {
                        is = ((BIFile) obj).getInputStream();
                    }
                } catch (Exception ignore) {
                    // fallback to local file
                }

                // 🔥 Fallback → Local file system
                if (is == null) {
                    File baseDir = new File(javax.baja.sys.Sys.getStationHome(), "files");
                    File file = new File(baseDir, relativePath);

                    if (!file.exists()) {
                        System.out.println("❌ File not found: " + file.getAbsolutePath());
                        exchange.sendResponseHeaders(404, -1);
                        return;
                    }

                    is = new FileInputStream(file);
                }

                byte[] bytes = readStream(is);

                exchange.getResponseHeaders().add("Content-Type", getContentType(relativePath));
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

                exchange.sendResponseHeaders(200, bytes.length);

                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.close();

            } catch (Exception e) {
                e.printStackTrace();
                exchange.sendResponseHeaders(500, -1);
            }
        }

        private byte[] readStream(InputStream is) throws IOException {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;

            while ((read = is.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
            }

            is.close();
            return bos.toByteArray();
        }

        // 🔥 Content type fix (IMPORTANT)
        private String getContentType(String path) {
            if (path.endsWith(".glb")) return "model/gltf-binary";
            if (path.endsWith(".gltf")) return "model/gltf+json";
            if (path.endsWith(".js")) return "application/javascript";
            if (path.endsWith(".html")) return "text/html";
            if (path.endsWith(".json")) return "application/json";
            if (path.endsWith(".pdf")) return "application/pdf";
            return "application/octet-stream";
        }
    }
}