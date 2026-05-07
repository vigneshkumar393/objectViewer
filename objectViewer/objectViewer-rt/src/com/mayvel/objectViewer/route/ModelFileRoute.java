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

            String query = exchange.getRequestURI().getQuery();

            if (query == null || !query.startsWith("name=")) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            String fileName = URLDecoder.decode(query.replace("name=", ""), "UTF-8");

            try {
                // ✅ Access Niagara File (NOT local disk)
                BOrd ord = BOrd.make("file:^3d_models/" + fileName);

                Object obj = ord.resolve().get();

                if (!(obj instanceof BIFile)) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }

                BIFile file = (BIFile) obj;
                InputStream is = file.getInputStream();

                byte[] bytes = readStream(is);

                // ✅ Headers
                exchange.getResponseHeaders().add("Content-Type", "model/gltf-binary");
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

                exchange.sendResponseHeaders(200, bytes.length);

                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.close();

            } catch (Exception e) {
                e.printStackTrace();
                exchange.sendResponseHeaders(404, -1);
            }
        }

        // ✅ Read InputStream
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
    }
}