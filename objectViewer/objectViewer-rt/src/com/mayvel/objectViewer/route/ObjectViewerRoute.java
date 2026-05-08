package com.mayvel.objectViewer.route;

import com.mayvel.objectViewer.controller.ConfigurationController;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.tridium.json.JSONArray;
import com.tridium.json.JSONObject;
import java.net.URLDecoder;
import javax.baja.naming.BOrd;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.Slot;
import javax.baja.sys.Sys;
import javax.baja.status.BStatusValue;
import java.io.*;
import java.net.URLDecoder;

public class ObjectViewerRoute {

    public static void registerRoutes(HttpServer server) {
        server.createContext("/objectviewer", new ObjectViewerHandler());
        server.createContext("/uploadModel", new UploadHandler());
        server.createContext("/listFiles",   new ListFilesHandler());
        server.createContext("/createFolder",new CreateFolderHandler());
        server.createContext("/listPoints",  new ListPointsHandler());
        server.createContext("/getPointValue", new GetPointValueHandler());
        ModelFileRoute.register(server);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPLOAD HANDLER
    // ─────────────────────────────────────────────────────────────────────────
    static class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }
                String query = exchange.getRequestURI().getQuery();
                if (query == null || !query.contains("folder=") || !query.contains("name=")) {
                    exchange.sendResponseHeaders(400, -1);
                    return;
                }
                String[] params = query.split("&");
                String folder   = URLDecoder.decode(params[0].split("=")[1], "UTF-8");
                String fileName = URLDecoder.decode(params[1].split("=")[1], "UTF-8");

                File stationHome = Sys.getStationHome();
                File baseDir     = new File(stationHome, "");
                if (!baseDir.exists()) baseDir.mkdirs();

                File targetFolder = new File(baseDir, folder);
                if (!targetFolder.exists()) targetFolder.mkdirs();

                File targetFile = new File(targetFolder, fileName);
                try (InputStream is = exchange.getRequestBody();
                     FileOutputStream fos = new FileOutputStream(targetFile)) {
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = is.read(buffer)) != -1) fos.write(buffer, 0, read);
                }
                System.out.println("✅ File uploaded: " + targetFile.getAbsolutePath());
                byte[] response = "UPLOAD_SUCCESS".getBytes();
                exchange.sendResponseHeaders(200, response.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(response); }
            } catch (Exception e) {
                e.printStackTrace();
                byte[] response = ("ERROR: " + e.getMessage()).getBytes();
                exchange.sendResponseHeaders(500, response.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(response); }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE FOLDER HANDLER
    // ─────────────────────────────────────────────────────────────────────────
    static class CreateFolderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }
                String query = exchange.getRequestURI().getQuery();
                if (query == null || !query.startsWith("name=")) {
                    exchange.sendResponseHeaders(400, -1);
                    return;
                }
                String name = URLDecoder.decode(query.replace("name=", ""), "UTF-8");
                File stationHome = Sys.getStationHome();
                File baseDir     = new File(stationHome, "");
                if (!baseDir.exists()) baseDir.mkdirs();
                File newFolder = new File(baseDir, name);
                if (!newFolder.exists()) newFolder.mkdirs();
                System.out.println("✅ Folder created: " + newFolder.getAbsolutePath());
                byte[] response = "FOLDER_CREATED".getBytes();
                exchange.sendResponseHeaders(200, response.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(response); }
            } catch (Exception e) {
                e.printStackTrace();
                byte[] response = ("ERROR: " + e.getMessage()).getBytes();
                exchange.sendResponseHeaders(500, response.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(response); }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LIST FILES HANDLER
    // ─────────────────────────────────────────────────────────────────────────
    static class ListFilesHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            try {
                File stationHome = Sys.getStationHome();
                File baseDir     = new File(stationHome, "");
                JSONArray result = new JSONArray();
                if (baseDir.exists()) {
                    File[] folders = baseDir.listFiles();
                    if (folders != null) {
                        for (File folder : folders) {
                            if (folder.isDirectory()) {
                                JSONObject folderObj = new JSONObject();
                                folderObj.put("name", folder.getName());
                                JSONArray filesArr = new JSONArray();
                                File[] files = folder.listFiles();
                                if (files != null) {
                                    for (File f : files) {
                                        if (f.isFile() && f.getName().toLowerCase().endsWith(".glb")) {
                                            filesArr.put(f.getName());
                                        }
                                    }
                                }
                                folderObj.put("files", filesArr);
                                result.put(folderObj);
                            }
                        }
                    }
                }
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                byte[] resp = result.toString().getBytes("UTF-8");
                exchange.sendResponseHeaders(200, resp.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(resp); }
            } catch (Exception e) {
                e.printStackTrace();
                exchange.sendResponseHeaders(500, -1);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LIST POINTS HANDLER — walks the station tree and collects BStatusValue points
    // ─────────────────────────────────────────────────────────────────────────
    static class ListPointsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                JSONArray points = new JSONArray();
                BComponent station = Sys.getStation();
                collectPoints(station, "", points, 0);

                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                byte[] resp = points.toString().getBytes("UTF-8");
                exchange.sendResponseHeaders(200, resp.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(resp); }
            } catch (Exception e) {
                e.printStackTrace();
                exchange.sendResponseHeaders(500, -1);
            }
        }

        private void collectPoints(BComponent comp, String path, JSONArray out, int depth) {
            if (depth > 10) return; // guard against deep recursion
            try {
                for (Slot slot : comp.getSlots()) {
                    try {
                        // BComponent.get() requires the slot name as a String
                        BObject child = comp.get(slot.getName());
                        if (child == null) continue;

                        String childPath = path.isEmpty() ? slot.getName() : path + "/" + slot.getName();

                        if (child instanceof BStatusValue) {
                            JSONObject pt = new JSONObject();
                            // Use slot name as short label, full childPath as display name for search
                            String cleanName = slot.getName()
                                    .replace("$20", " ")
                                    .replace("%20", " ")
                                    .replace("$2d", "-")
                                    .replace("%2d", "-");

                            pt.put("name", cleanName);
                            String cleanPath = childPath
                                    .replace("$20", " ")
                                    .replace("%20", " ")
                                    .replace("$2d", "-")
                                    .replace("%2d", "-");

                            pt.put("displayName", cleanPath);
                            pt.put("path", "station:|slot:/" + childPath);
                            try {
                                BStatusValue sv = (BStatusValue) child;
                                String val = sv.getValueValue() != null ? sv.getValueValue().toString() : "—";
                                pt.put("value", val);
                            } catch (Exception ve) {
                                pt.put("value", "—");
                            }
                            out.put(pt);
                        }

                        if (child instanceof BComponent) {
                            collectPoints((BComponent) child, childPath, out, depth + 1);
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET POINT VALUE HANDLER — returns live value for a given station path
    // Query: ?path=station:|slot:/Config/...
    // ─────────────────────────────────────────────────────────────────────────
    static class GetPointValueHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                String query = exchange.getRequestURI().getQuery();
                if (query == null || !query.startsWith("path=")) {
                    exchange.sendResponseHeaders(400, -1);
                    return;
                }
                String ordStr = URLDecoder.decode(query.substring(5), "UTF-8");
                BOrd ord = BOrd.make(ordStr);
                BObject obj = ord.resolve().get();

                JSONObject result = new JSONObject();
                if (obj instanceof BStatusValue) {
                    BStatusValue sv = (BStatusValue) obj;
                    String val = (sv.getValueValue() != null) ? sv.getValueValue().toString() : "—";
                    result.put("value", val);
                    result.put("status", "ok");
                } else {
                    result.put("value", obj != null ? obj.toString() : "—");
                    result.put("status", "ok");
                }

                byte[] resp = result.toString().getBytes("UTF-8");
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, resp.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(resp); }
            } catch (Exception e) {
                e.printStackTrace();
                JSONObject err = new JSONObject();
                try { err.put("value", "ERR"); err.put("status", "error"); } catch (Exception ignored) {}
                byte[] resp = err.toString().getBytes("UTF-8");
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, resp.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(resp); }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAIN PAGE HANDLER
    // ─────────────────────────────────────────────────────────────────────────
    static class ObjectViewerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {

            String html = "<!DOCTYPE html>\n"
                    + "<html lang=\"en\">\n"
                    + "<head>\n"
                    + "<meta charset=\"UTF-8\">\n"
                    + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                    + "<title>3D Model Viewer</title>\n"
                    + "<style>\n"
                    + ":root {\n"
                    + "  color-scheme: light;\n"
                    + "  --bg: #eef3f7;\n"
                    + "  --panel-bg: rgba(255,255,255,0.9);\n"
                    + "  --panel-border: rgba(20,42,61,0.14);\n"
                    + "  --text: #142a3d;\n"
                    + "  --text-light: #627384;\n"
                    + "  --muted: #627384;\n"
                    + "  --accent: #0c7c59;\n"
                    + "  --accent-dark: #095b42;\n"
                    + "  --bg-color: #eef3f7;\n"
                    + "  --radius: 16px;\n"
                    + "  --radius-sm: 8px;\n"
                    + "  --shadow: 0 18px 45px rgba(22,39,56,0.14);\n"
                    + "  --shadow-sm: 0 4px 12px rgba(22,39,56,0.08);\n"
                    + "}\n"
                    + "* { box-sizing: border-box; }\n"
                    + "body, html { margin:0; padding:0; height:100%; font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",Roboto,Helvetica,Arial,sans-serif; background-color:var(--bg-color); color:var(--text); overflow:hidden; }\n"
                    + ".top-bar { width:100%; height:60px; background:#2f3640; color:white; display:flex; justify-content:space-between; align-items:center; padding:0 20px; position:fixed; top:0; left:0; z-index:1000; }\n"
                    + ".top-bar button { padding:6px 12px; border:none; border-radius:5px; cursor:pointer; }\n"
                    + ".top-bar button:first-child { background:#40739e; color:white; }\n"
                    + ".top-bar button:last-child  { background:#e84118; color:white; }\n"
                    + ".app-container { display:flex; height:calc(100vh - 60px); margin-top:60px; background:radial-gradient(circle at center,#ffffff 0%,var(--bg-color) 100%); }\n"
                    + ".sidebar { width:320px; background:var(--panel-bg); backdrop-filter:blur(20px); -webkit-backdrop-filter:blur(20px); border-right:1px solid var(--panel-border); display:flex; flex-direction:column; box-shadow:2px 0 20px rgba(0,0,0,0.03); z-index:10; }\n"
                    + ".sidebar-header { padding:24px; border-bottom:1px solid rgba(0,0,0,0.05); display:flex; justify-content:space-between; align-items:center; }\n"
                    + ".sidebar-header h2 { margin:0; font-size:20px; font-weight:700; letter-spacing:-0.5px; background:linear-gradient(135deg,var(--text) 0%,#4a5568 100%); -webkit-background-clip:text; -webkit-text-fill-color:transparent; }\n"
                    + ".file-browser { flex:1; overflow-y:auto; padding:16px; }\n"
                    + ".folder-item { margin-bottom:12px; }\n"
                    + ".folder-header { display:flex; align-items:center; padding:10px 14px; cursor:pointer; border-radius:var(--radius-sm); transition:all 0.2s ease; font-weight:600; color:var(--text); }\n"
                    + ".folder-header:hover { background:rgba(0,0,0,0.03); }\n"
                    + ".file-list { margin:4px 0 8px 16px; border-left:2px solid rgba(0,0,0,0.05); padding-left:8px; }\n"
                    + ".file-item { display:flex; align-items:center; padding:8px 12px; cursor:pointer; border-radius:var(--radius-sm); transition:all 0.2s; font-size:14px; color:var(--text-light); margin-bottom:4px; }\n"
                    + ".file-item:hover { background:rgba(37,99,235,0.05); color:var(--accent); transform:translateX(4px); }\n"
                    + ".file-item.active { background:rgba(37,99,235,0.1); color:var(--accent); font-weight:600; }\n"
                    + ".upload-btn { width:calc(100% - 24px); margin:8px 12px 16px 12px; padding:10px; border:1px dashed var(--accent); border-radius:var(--radius-sm); background:rgba(37,99,235,0.03); color:var(--accent); cursor:pointer; display:flex; align-items:center; justify-content:center; gap:8px; font-size:13px; font-weight:600; transition:all 0.2s; }\n"
                    + ".upload-btn:hover { background:rgba(37,99,235,0.1); transform:translateY(-1px); }\n"
                    + ".status-container { padding:16px 24px; border-top:1px solid rgba(0,0,0,0.05); margin-top:auto; background:rgba(255,255,255,0.5); }\n"
                    + ".progress-bar { height:6px; width:0%; background:var(--accent); border-radius:3px; transition:width 0.2s ease-out; }\n"
                    + ".status { font-size:12px; color:var(--text-light); margin:8px 0 0 0; }\n"
                    + ".viewer-shell { flex:1; position:relative; border-radius:var(--radius); overflow:hidden; box-shadow:var(--shadow); margin:16px; background:#ffffff; }\n"
                    + "#viewer { width:100%; height:100%; display:block; }\n"
                    + ".viewer-badge { position:absolute; bottom:18px; right:24px; background:rgba(255,255,255,0.8); backdrop-filter:blur(10px); -webkit-backdrop-filter:blur(10px); padding:8px 16px; border-radius:20px; font-size:13px; color:var(--text-light); box-shadow:var(--shadow-sm); z-index:10; }\n"
                    + ".camera-nav { position:absolute; top:24px; left:50%; transform:translateX(-50%); display:flex; flex-direction:column; align-items:center; gap:12px; z-index:10; background:rgba(240,244,248,0.45); backdrop-filter:blur(24px); -webkit-backdrop-filter:blur(24px); padding:14px 18px; border-radius:24px; box-shadow:0 12px 40px rgba(0,0,0,0.08),inset 0 1px 2px rgba(255,255,255,0.8); border:1px solid rgba(255,255,255,0.5); max-width:85%; }\n"
                    + ".cam-btn { background:rgba(255,255,255,0.95); border:1px solid rgba(0,0,0,0.04); color:var(--text); padding:10px 18px; border-radius:24px; font-size:11px; font-weight:700; cursor:pointer; display:flex; align-items:center; gap:6px; transition:all 0.3s cubic-bezier(0.2,0.8,0.2,1); text-transform:uppercase; letter-spacing:0.5px; white-space:nowrap; flex-shrink:0; box-shadow:0 2px 8px rgba(0,0,0,0.04); }\n"
                    + ".cam-btn:hover { background:var(--accent); color:white; transform:translateY(-2px) scale(1.02); box-shadow:0 8px 20px rgba(37,99,235,0.25); border-color:var(--accent); }\n"
                    + "#loaderOverlay { display:none; position:absolute; top:0; left:0; width:100%; height:100%; background:rgba(255,255,255,0.7); backdrop-filter:blur(4px); -webkit-backdrop-filter:blur(4px); z-index:9999; flex-direction:column; align-items:center; justify-content:center; }\n"
                    + "@keyframes spin { to { transform:rotate(360deg); } }\n"
                    + "@keyframes indeterminate { 0% { transform:translateX(-200%); } 100% { transform:translateX(200%); } }\n"
// ── Point search dropdown styles ──
                    + ".point-search-wrap { position:relative; }\n"
                    + ".point-search-input { width:100%; padding:8px 10px; border:1px solid #ccc; border-radius:4px; font-size:13px; box-sizing:border-box; }\n"
                    + ".point-dropdown { position:fixed; max-height:320px; overflow-y:auto; background:#fff; border:1px solid #b0bec5; border-radius:6px; z-index:99999; box-shadow:0 8px 32px rgba(0,0,0,0.18); min-width:340px; }\n"
                    + ".point-dropdown-item { padding:10px 12px; cursor:pointer; font-size:13px; color:var(--text); border-bottom:1px solid #f0f0f0; }\n"
                    + ".point-dropdown-item:last-child { border-bottom:none; }\n"
                    + ".point-dropdown-item:hover { background:rgba(12,124,89,0.08); color:var(--accent); }\n"
                    + ".point-dropdown-item .pt-name { font-weight:600; margin-bottom:2px; }\n"
                    + ".point-dropdown-item .pt-path { font-size:10px; color:var(--muted); word-break:break-all; white-space:normal; line-height:1.4; }\n"
                    + ".point-search-refresh { float:right; background:none; border:none; color:var(--accent); cursor:pointer; font-size:12px; padding:0; margin-left:6px; text-decoration:underline; }\n"
// ── 3D label overlay ──
                    + ".model-label { position:absolute; pointer-events:all; cursor:pointer; z-index:500; transform:translate(-50%,-100%); }\n"
                    + ".model-label-inner { background:rgba(20,42,61,0.88); color:#fff; padding:6px 10px; border-radius:8px; font-size:12px; font-weight:600; white-space:nowrap; box-shadow:0 4px 16px rgba(0,0,0,0.25); border:1px solid rgba(255,255,255,0.15); display:flex; flex-direction:column; align-items:center; gap:2px; }\n"
                    + ".model-label-name { font-size:10px; font-weight:500; opacity:0.7; text-transform:uppercase; letter-spacing:0.5px; }\n"
                    + ".model-label-value { font-size:15px; font-weight:700; color:#4cffa0; }\n"
                    + ".model-label-stem { width:2px; height:16px; background:rgba(20,42,61,0.7); margin:0 auto; }\n"
                    + ".model-label-dot  { width:8px; height:8px; background:#4cffa0; border-radius:50%; margin:0 auto; box-shadow:0 0 6px #4cffa0; }\n"
                    + "</style>\n"
                    + "</head>\n"
                    + "<body>\n"
// TOP BAR
                    + "<div class=\"top-bar\">\n"
                    + "  <div>3D Object Viewer</div>\n"
                    + "  <div style=\"display:flex;gap:10px;\">\n"
                    + "    <button onclick=\"openSettings()\">⚙️</button>\n"
                    + "    <button onclick=\"logout()\">Logout</button>\n"
                    + "  </div>\n"
                    + "</div>\n"
// LOADER
                    + "<div id=\"loaderOverlay\">\n"
                    + "  <div style=\"width:40px;height:40px;border:4px solid rgba(37,99,235,0.2);border-top-color:var(--accent);border-radius:50%;animation:spin 1s linear infinite;margin-bottom:16px;\"></div>\n"
                    + "  <div id=\"loaderText\" style=\"font-size:16px;font-weight:600;color:var(--text);margin-bottom:12px;\">Loading...</div>\n"
                    + "  <div style=\"width:240px;height:6px;background:rgba(0,0,0,0.1);border-radius:3px;overflow:hidden;margin-bottom:8px;position:relative;\">\n"
                    + "    <div id=\"loaderProgressBar\" style=\"width:0%;height:100%;background:var(--accent);transition:width 0.2s;position:absolute;left:0;\"></div>\n"
                    + "  </div>\n"
                    + "  <div id=\"loaderPercent\" style=\"font-size:13px;color:var(--muted);font-weight:500;\"></div>\n"
                    + "</div>\n"
                    + "<main class=\"app-container\">\n"
// SIDEBAR
                    + "  <section class=\"sidebar\">\n"
                    + "    <div class=\"sidebar-header\">\n"
                    + "      <h2>Models</h2>\n"
                    + "      <button id=\"newFolderBtn\" class=\"cam-btn\" title=\"New Folder\" style=\"padding:4px 10px;font-size:16px;border:1px solid var(--panel-border);\">+</button>\n"
                    + "    </div>\n"
                    + "    <div id=\"fileBrowser\" class=\"file-browser\"></div>\n"
                    + "    <input id=\"fileInput\" type=\"file\" accept=\".glb,model/gltf-binary\" style=\"display:none;\">\n"
                    + "    <div class=\"status-container\">\n"
                    + "      <div id=\"uploadProgressContainer\" style=\"display:none;height:6px;background:rgba(0,0,0,0.05);border-radius:3px;margin-bottom:8px;\">\n"
                    + "        <div class=\"progress-bar\" id=\"progressBar\"></div>\n"
                    + "      </div>\n"
                    + "      <p id=\"status\" class=\"status\">Viewer ready.</p>\n"
                    + "    </div>\n"
                    + "  </section>\n"
// VIEWER
                    + "  <section class=\"viewer-shell\">\n"
                    + "    <div class=\"viewer-badge\">Orbit: drag · Scroll: zoom</div>\n"
                    + "    <div id=\"cameraNav\" class=\"camera-nav\" style=\"display:none;\"></div>\n"
// ── Tag Creator UI ──
                    + "    <div id=\"tagCreatorUI\" style=\"display:none;position:absolute;top:80px;right:24px;background:rgba(255,255,255,0.97);padding:18px;border-radius:10px;box-shadow:var(--shadow);z-index:1000;width:360px;font-size:14px;border:1px solid var(--panel-border);\">\n"
                    + "      <h4 style=\"margin:0 0 14px 0;color:var(--text);\">Create New Tag</h4>\n"
// Tag Name
                    + "      <div style=\"margin-bottom:12px;\">\n"
                    + "        <label style=\"display:block;margin-bottom:4px;color:var(--muted);font-weight:500;font-size:12px;\">TAG NAME</label>\n"
                    + "        <input type=\"text\" id=\"tagNameInput\" placeholder=\"e.g. Filter Check\" style=\"width:100%;padding:8px;border:1px solid #ccc;border-radius:4px;font-size:13px;box-sizing:border-box;\">\n"
                    + "      </div>\n"
// Focus Part
                    + "      <div style=\"margin-bottom:12px;\">\n"
                    + "        <label style=\"display:block;margin-bottom:4px;color:var(--muted);font-weight:500;font-size:12px;\">FOCUS PART (click model)</label>\n"
                    + "        <div id=\"tagTargetDisplay\" style=\"width:100%;padding:8px;background:#f0f4f8;border:1px solid #d9e4ec;border-radius:4px;font-size:13px;color:var(--text);min-height:35px;word-break:break-all;\">Click a part on the 3D model...</div>\n"
                    + "      </div>\n"
// Niagara Point Search
                    + "      <div style=\"margin-bottom:16px;\">\n"
                    + "        <label style=\"display:block;margin-bottom:4px;color:var(--muted);font-weight:500;font-size:12px;\">NIAGARA POINT (search &amp; select) <button class=\"point-search-refresh\" onclick=\"reloadPoints()\">↻ refresh</button></label>\n"
                    + "        <div class=\"point-search-wrap\" id=\"pointSearchWrap\">\n"
                    + "          <input type=\"text\" id=\"pointSearchInput\" class=\"point-search-input\" placeholder=\"Type to search points...\" autocomplete=\"off\">\n"
                    + "          <div class=\"point-dropdown\" id=\"pointDropdown\" style=\"display:none;\"></div>\n"
                    + "        </div>\n"
                    + "        <div id=\"selectedPointDisplay\" style=\"margin-top:6px;padding:6px 8px;background:#f0f4f8;border-radius:4px;font-size:11px;color:var(--muted);display:none;\"></div>\n"
                    + "      </div>\n"
                    + "      <div style=\"display:flex;gap:8px;justify-content:flex-end;\">\n"
                    + "        <button onclick=\"cancelCreateTag()\" style=\"padding:6px 12px;border:1px solid #ccc;background:white;border-radius:4px;cursor:pointer;font-size:13px;\">Cancel</button>\n"
                    + "        <button onclick=\"saveNewTag()\" style=\"padding:6px 12px;border:none;background:var(--accent);color:white;border-radius:4px;cursor:pointer;font-weight:500;font-size:13px;\">Save Tag</button>\n"
                    + "      </div>\n"
                    + "    </div>\n"
// Label overlay container
                    + "    <div id=\"labelContainer\" style=\"position:absolute;top:0;left:0;width:100%;height:100%;pointer-events:none;z-index:200;\"></div>\n"
                    + "    <div id=\"viewer\"></div>\n"
                    + "  </section>\n"
                    + "</main>\n"
// Scripts
                    + "<script src=\"https://cdn.jsdelivr.net/npm/three@0.128.0/build/three.min.js\"></script>\n"
                    + "<script src=\"https://cdn.jsdelivr.net/npm/three@0.128.0/examples/js/controls/OrbitControls.js\"></script>\n"
                    + "<script src=\"https://cdn.jsdelivr.net/npm/three@0.128.0/examples/js/loaders/GLTFLoader.js\"></script>\n"
                    + "<script src=\"https://cdn.jsdelivr.net/npm/gsap@3.12.2/dist/gsap.min.js\"></script>\n"
                    + "<script>\n"
// ─── GLOBALS ───
                    + "let virtualFS = [];\n"
                    + "let activeFolderId = null;\n"
                    + "let activeFileId   = null;\n"
                    + "const fileInput               = document.getElementById('fileInput');\n"
                    + "const status                  = document.getElementById('status');\n"
                    + "const viewer                  = document.getElementById('viewer');\n"
                    + "const uploadProgressContainer = document.getElementById('uploadProgressContainer');\n"
                    + "const progressBar             = document.getElementById('progressBar');\n"
                    + "const cameraNav               = document.getElementById('cameraNav');\n"
                    + "const labelContainer          = document.getElementById('labelContainer');\n"
                    + "document.querySelector('.viewer-shell').prepend(document.getElementById('loaderOverlay'));\n"
// ─── POINT CACHE ───
                    + "let allNiagaraPoints = [];  // {name, path, value}\n"
                    + "let selectedPointPath = null;\n"
                    + "let selectedPointName = null;\n"
// ─── UTILITY ───
                    + "function setStatus(message) {\n"
                    + "  status.textContent = message;\n"
                    + "  const overlay    = document.getElementById('loaderOverlay');\n"
                    + "  const loaderText = document.getElementById('loaderText');\n"
                    + "  const loaderBar  = document.getElementById('loaderProgressBar');\n"
                    + "  const loaderPct  = document.getElementById('loaderPercent');\n"
                    + "  if (overlay && loaderText) {\n"
                    + "    if (message.toLowerCase().includes('loading')) {\n"
                    + "      overlay.style.display = 'flex';\n"
                    + "      loaderText.textContent = message;\n"
                    + "      if (loaderBar) { loaderBar.style.width='0%'; loaderBar.style.animation='none'; }\n"
                    + "      if (loaderPct) loaderPct.textContent = 'Connecting...';\n"
                    + "    } else {\n"
                    + "      overlay.style.display = 'none';\n"
                    + "    }\n"
                    + "  }\n"
                    + "}\n"
                    + "function logout()       { window.location.href = '/'; }\n"
                    + "function openSettings() { alert('Settings coming soon'); }\n"
// ─── GET MODEL URL ───
                    + "function getModelUrls(model) {\n"
                    + "  return model.fileNames.map(function(fileName) {\n"
                    + "    return '/getModel?path=' + encodeURIComponent(model.fullPath + '/' + fileName);\n"
                    + "  });\n"
                    + "}\n"
// ─── POINT SEARCH ───
                    + "async function loadNiagaraPoints() {\n"
                    + "  try {\n"
                    + "    const res  = await fetch('/listPoints');\n"
                    + "    const data = await res.json();\n"
                    + "    allNiagaraPoints = data;\n"
                    + "    console.log('Loaded ' + data.length + ' Niagara points');\n"
                    + "  } catch(e) {\n"
                    + "    console.warn('Could not load Niagara points:', e);\n"
                    + "    allNiagaraPoints = [];\n"
                    + "  }\n"
                    + "}\n"
                    + "async function reloadPoints() {\n"
                    + "  const inp = document.getElementById('pointSearchInput');\n"
                    + "  inp.placeholder = 'Refreshing...';\n"
                    + "  await loadNiagaraPoints();\n"
                    + "  inp.placeholder = 'Type to search points...';\n"
                    + "  const results = filterPoints(inp.value);\n"
                    + "  positionAndShowDropdown(results);\n"
                    + "}\n"
                    + "function normalizeSearchText(text) {\n" +
                    "\n" +
                    "  return (text || '')\n" +
                    "    .toLowerCase()\n" +
                    "    .replace(/\\$20/g, ' ')\n" +
                    "    .replace(/%20/g, ' ')\n" +
                    "    .replace(/[_-]/g, ' ')\n" +
                    "    .replace(/\\s+/g, ' ')\n" +
                    "    .trim();\n" +
                    "\n" +
                    "}\n" +
                    "\n" +
                    "function filterPoints(query) {\n" +
                    "\n" +
                    "  if (!query || !query.trim()) {\n" +
                    "    return allNiagaraPoints.slice(0, 80);\n" +
                    "  }\n" +
                    "\n" +
                    "  const q = normalizeSearchText(query);\n" +
                    "\n" +
                    "  return allNiagaraPoints.filter(function(p) {\n" +
                    "\n" +
                    "    const name =\n" +
                    "      normalizeSearchText(p.name);\n" +
                    "\n" +
                    "    const displayName =\n" +
                    "      normalizeSearchText(p.displayName);\n" +
                    "\n" +
                    "    const path =\n" +
                    "      normalizeSearchText(p.path);\n" +
                    "\n" +
                    "    return (\n" +
                    "      name.includes(q) ||\n" +
                    "      displayName.includes(q) ||\n" +
                    "      path.includes(q)\n" +
                    "    );\n" +
                    "\n" +
                    "  }).slice(0, 80);\n" +
                    "\n" +
                    "}\n"
                    + "function positionAndShowDropdown(items) {\n"
                    + "  const inp = document.getElementById('pointSearchInput');\n"
                    + "  const dd  = document.getElementById('pointDropdown');\n"
                    + "  // Position fixed dropdown under the input\n"
                    + "  const rect = inp.getBoundingClientRect();\n"
                    + "  dd.style.top   = (rect.bottom + 2) + 'px';\n"
                    + "  dd.style.left  = rect.left + 'px';\n"
                    + "  dd.style.width = Math.max(rect.width, 340) + 'px';\n"
                    + "  renderPointDropdown(items);\n"
                    + "}\n"
                    + "function renderPointDropdown(items) {\n"
                    + "  const dd = document.getElementById('pointDropdown');\n"
                    + "  dd.innerHTML = '';\n"
                    + "  if (items.length === 0) {\n"
                    + "    dd.innerHTML = '<div class=\"point-dropdown-item\" style=\"color:var(--muted);\">No points found — try ↻ refresh if point is new</div>';\n"
                    + "    dd.style.display = 'block';\n"
                    + "    return;\n"
                    + "  }\n"
                    + "  items.forEach(function(pt) {\n"
                    + "    const div = document.createElement('div');\n"
                    + "    div.className = 'point-dropdown-item';\n"
                    + "    const label = pt.displayName || pt.name;\n"
                    + "    div.innerHTML =\n"
                    + "      '<div class=\"pt-name\">' + pt.name + '</div>'\n"
                    + "      + '<div class=\"pt-path\">' + label + '</div>';\n"
                    + "    div.onclick = function() { selectPoint(pt); };\n"
                    + "    dd.appendChild(div);\n"
                    + "  });\n"
                    + "  dd.style.display = 'block';\n"
                    + "}\n"
                    + "function selectPoint(pt) {\n"
                    + "  selectedPointPath = pt.path;\n"
                    + "  selectedPointName = pt.name;\n"
                    + "  document.getElementById('pointSearchInput').value = pt.displayName || pt.name;\n"
                    + "  document.getElementById('pointDropdown').style.display = 'none';\n"
                    + "  const disp = document.getElementById('selectedPointDisplay');\n"
                    + "  disp.style.display = 'block';\n"
                    + "  disp.innerHTML = '\\u2705 <strong>' + pt.name + '</strong><br><span style=\"word-break:break-all;\">' + pt.path + '</span>';\n"
                    + "}\n"
// ─── SETUP POINT SEARCH INPUT EVENTS ───
                    + "function initPointSearch() {\n"
                    + "  const inp = document.getElementById('pointSearchInput');\n"
                    + "  inp.addEventListener('input', function() {\n"
                    + "    const results = filterPoints(inp.value);\n"
                    + "    positionAndShowDropdown(results);\n"
                    + "  });\n"
                    + "  inp.addEventListener('focus', function() {\n" +
                    "\n" +
                    "  // only open if already typing\n" +
                    "  if (inp.value.trim().length > 0) {\n" +
                    "\n" +
                    "    const results = filterPoints(inp.value);\n" +
                    "\n" +
                    "    positionAndShowDropdown(results);\n" +
                    "  }\n" +
                    "});\n"
                    + "  document.addEventListener('click', function(e) {\n"
                    + "    const wrap = document.getElementById('pointSearchWrap');\n"
                    + "    const dd   = document.getElementById('pointDropdown');\n"
                    + "    if (wrap && !wrap.contains(e.target) && dd && !dd.contains(e.target)) {\n"
                    + "      dd.style.display = 'none';\n"
                    + "    }\n"
                    + "  });\n"
                    + "  // Re-position on scroll/resize so fixed dropdown stays aligned\n"
                    + "  window.addEventListener('resize', function() {\n"
                    + "    document.getElementById('pointDropdown').style.display = 'none';\n"
                    + "  });\n"
                    + "}\n"
// ─── VIRTUAL FS ───
                    + "function renderVirtualFS() {\n"
                    + "  const browser = document.getElementById('fileBrowser');\n"
                    + "  browser.innerHTML = '';\n"
                    + "  if (!virtualFS || virtualFS.length === 0) {\n"
                    + "    browser.innerHTML = '<p style=\"color:var(--muted);font-size:13px;padding:12px;\">No folders yet. Click + to create one.</p>';\n"
                    + "    return;\n"
                    + "  }\n"
                    + "  virtualFS.forEach(function(folder) {\n"
                    + "    const folderEl = document.createElement('div');\n"
                    + "    folderEl.className = 'folder-item';\n"
                    + "    const folderHeader = document.createElement('div');\n"
                    + "    folderHeader.className = 'folder-header';\n"
                    + "    folderHeader.innerHTML = '\\uD83D\\uDCC1 ' + folder.name;\n"
                    + "    folderHeader.onclick = function() { folder.expanded = !folder.expanded; renderVirtualFS(); };\n"
                    + "    folderEl.appendChild(folderHeader);\n"
                    + "    if (folder.expanded) {\n"
                    + "      const folderContent = document.createElement('div');\n"
                    + "      folderContent.className = 'folder-content';\n"
                    + "      const uploadBtn = document.createElement('button');\n"
                    + "      uploadBtn.className = 'upload-btn';\n"
                    + "      uploadBtn.innerHTML = '\\u2B06 Upload .glb';\n"
                    + "      uploadBtn.onclick = function(e) { e.stopPropagation(); activeFolderId = folder.id; fileInput.click(); };\n"
                    + "      folderContent.appendChild(uploadBtn);\n"
                    + "      const fileList = document.createElement('div');\n"
                    + "      fileList.className = 'file-list';\n"
                    + "      if (!folder.files || folder.files.length === 0) {\n"
                    + "        const empty = document.createElement('div');\n"
                    + "        empty.style.cssText = 'font-size:12px;color:var(--muted);padding:6px 12px;';\n"
                    + "        empty.textContent = 'No files yet';\n"
                    + "        fileList.appendChild(empty);\n"
                    + "      } else {\n"
                    + "        folder.files.forEach(function(file) {\n"
                    + "          const fileEl = document.createElement('div');\n"
                    + "          fileEl.className = 'file-item' + (activeFileId === file.id ? ' active' : '');\n"
                    + "          fileEl.innerHTML = '\\uD83D\\uDDC2 ' + file.name;\n"
                    + "          fileEl.onclick = function(e) {\n"
                    + "            e.stopPropagation();\n"
                    + "            activeFileId = file.id;\n"
                    + "            renderVirtualFS();\n"
                    + "            if (file.type === 'server') {\n"
                    + "              loadModel({ label: file.name, fileNames: file.fileNames, fullPath: file.fullPath });\n"
                    + "            } else if (file.type === 'local') {\n"
                    + "              loadModelFile(file.file);\n"
                    + "            }\n"
                    + "          };\n"
                    + "          fileList.appendChild(fileEl);\n"
                    + "        });\n"
                    + "      }\n"
                    + "      folderContent.appendChild(fileList);\n"
                    + "      folderEl.appendChild(folderContent);\n"
                    + "    }\n"
                    + "    browser.appendChild(folderEl);\n"
                    + "  });\n"
                    + "}\n"
// ─── BUILD FS ───
                    + "function buildFS(data, parentPath) {\n"
                    + "  parentPath = parentPath || '';\n"
                    + "  return data.map(function(folder) {\n"
                    + "    const currentPath = parentPath ? parentPath + '/' + folder.name : folder.name;\n"
                    + "    return {\n"
                    + "      id: 'folder-' + currentPath,\n"
                    + "      name: folder.name,\n"
                    + "      fullPath: currentPath,\n"
                    + "      expanded: true,\n"
                    + "      files: (folder.files || []).map(function(file) {\n"
                    + "        return { id: 'file-' + currentPath + '-' + file, name: file, type: 'server', fileNames: [file], fullPath: currentPath };\n"
                    + "      }),\n"
                    + "      children: buildFS(folder.folders || [], currentPath)\n"
                    + "    };\n"
                    + "  });\n"
                    + "}\n"
                    + "async function loadFileSystem() {\n"
                    + "  const res  = await fetch('/listFiles');\n"
                    + "  const data = await res.json();\n"
                    + "  virtualFS  = buildFS(data);\n"
                    + "  renderVirtualFS();\n"
                    + "}\n"
// ─── NEW FOLDER ───
                    + "document.getElementById('newFolderBtn').addEventListener('click', async function() {\n"
                    + "  const name = prompt('Enter folder name:');\n"
                    + "  if (!name || !name.trim()) return;\n"
                    + "  try {\n"
                    + "    const res = await fetch('/createFolder?name=' + encodeURIComponent(name.trim()), { method: 'POST' });\n"
                    + "    if (!res.ok) throw new Error('HTTP ' + res.status);\n"
                    + "    await loadFileSystem();\n"
                    + "    setStatus('Folder created: ' + name);\n"
                    + "  } catch (err) { alert('Folder creation failed: ' + err.message); }\n"
                    + "});\n"
// ─── FILE UPLOAD ───
                    + "fileInput.addEventListener('change', function() {\n"
                    + "  const file = fileInput.files[0];\n"
                    + "  if (!file || !activeFolderId) return;\n"
                    + "  const folder = virtualFS.find(function(f) { return f.id === activeFolderId; });\n"
                    + "  if (!folder) return;\n"
                    + "  setStatus('Uploading...');\n"
                    + "  const url = '/uploadModel?folder=' + encodeURIComponent(folder.name) + '&name=' + encodeURIComponent(file.name);\n"
                    + "  uploadProgressContainer.style.display = 'block';\n"
                    + "  progressBar.style.width = '0%';\n"
                    + "  const xhr = new XMLHttpRequest();\n"
                    + "  xhr.open('PUT', url, true);\n"
                    + "  const csrfMatch = document.cookie.match(/(^| )niagara_csrf=([^;]+)/);\n"
                    + "  if (csrfMatch) xhr.setRequestHeader('X-Niagara-Csrf-Token', csrfMatch[2]);\n"
                    + "  xhr.setRequestHeader('Content-Type', file.type || 'application/octet-stream');\n"
                    + "  xhr.upload.onprogress = function(e) {\n"
                    + "    if (e.lengthComputable) progressBar.style.width = Math.round((e.loaded / e.total) * 100) + '%';\n"
                    + "  };\n"
                    + "  xhr.onload = function() {\n"
                    + "    setTimeout(function() { uploadProgressContainer.style.display = 'none'; }, 1500);\n"
                    + "    if (xhr.status >= 200 && xhr.status < 300) {\n"
                    + "      setStatus('Uploaded to ' + folder.name + '/' + file.name);\n"
                    + "      loadFileSystem().then(function() { loadModelFile(file); });\n"
                    + "    } else {\n"
                    + "      setStatus('Upload failed (HTTP ' + xhr.status + '). Previewing locally.');\n"
                    + "      const newFile = { id:'file-'+Date.now(), name:file.name, type:'local', file:file };\n"
                    + "      folder.files.push(newFile); renderVirtualFS(); loadModelFile(file);\n"
                    + "    }\n"
                    + "  };\n"
                    + "  xhr.onerror = function() {\n"
                    + "    uploadProgressContainer.style.display = 'none';\n"
                    + "    setStatus('Upload error. Previewing locally.');\n"
                    + "    const newFile = { id:'file-'+Date.now(), name:file.name, type:'local', file:file };\n"
                    + "    folder.files.push(newFile); renderVirtualFS(); loadModelFile(file);\n"
                    + "  };\n"
                    + "  xhr.send(file);\n"
                    + "  fileInput.value = '';\n"
                    + "});\n"
// ─── CAMERA / TAG VIEWS ───
                    + "let allModelViews    = JSON.parse(localStorage.getItem('niagara_3d_all_views')) || {};\n"
                    + "let currentModelName = '';\n"
                    + "let views = {};\n"
                    + "function updateCameraNavVisibility(modelName) {\n"
                    + "  currentModelName = modelName.replace('.glb', '').trim();\n"
                    + "  if (!allModelViews[currentModelName]) allModelViews[currentModelName] = {};\n"
                    + "  views = allModelViews[currentModelName];\n"
                    + "  if (cameraNav) { cameraNav.style.display = 'flex'; renderCameraNav(); }\n"
                    + "  refreshAllLabels();\n"
                    + "}\n"
                    + "function renderCameraNav() {\n"
                    + "  const nav = document.getElementById('cameraNav');\n"
                    + "  if (!nav) return;\n"
                    + "  nav.innerHTML = '';\n"
                    + "  const tagsWrapper = document.createElement('div');\n"
                    + "  tagsWrapper.style.cssText = 'display:flex;flex-wrap:wrap;justify-content:center;gap:10px;max-height:120px;overflow-y:auto;width:100%;padding-right:4px;';\n"
                    + "  for (const id in views) {\n"
                    + "    const view = views[id];\n"
                    + "    const btnContainer = document.createElement('div');\n"
                    + "    btnContainer.className = 'cam-btn';\n"
                    + "    btnContainer.style.cssText = 'padding:6px 8px 6px 16px;';\n"
                    + "    btnContainer.onclick = function() { flyToView(id); };\n"
                    + "    const textSpan = document.createElement('span');\n"
                    + "    textSpan.textContent = view.name.toUpperCase();\n"
                    + "    const deleteBtn = document.createElement('button');\n"
                    + "    deleteBtn.innerHTML = '\\u2715';\n"
                    + "    deleteBtn.style.cssText = 'background:transparent;border:none;color:currentColor;opacity:0.5;cursor:pointer;font-size:10px;margin-left:4px;padding:4px 6px;border-radius:50%;transition:all 0.2s;';\n"
                    + "    deleteBtn.onmouseover = function() { deleteBtn.style.opacity='1'; deleteBtn.style.background='rgba(0,0,0,0.1)'; };\n"
                    + "    deleteBtn.onmouseout  = function() { deleteBtn.style.opacity='0.5'; deleteBtn.style.background='transparent'; };\n"
                    + "    deleteBtn.onclick = function(e) {\n"
                    + "      e.stopPropagation();\n"
                    + "      if (confirm('Delete tag \"' + view.name + '\"?')) {\n"
                    + "        delete views[id];\n"
                    + "        allModelViews[currentModelName] = views;\n"
                    + "        localStorage.setItem('niagara_3d_all_views', JSON.stringify(allModelViews));\n"
                    + "        renderCameraNav();\n"
                    + "        refreshAllLabels();\n"
                    + "      }\n"
                    + "    };\n"
                    + "    btnContainer.appendChild(textSpan);\n"
                    + "    btnContainer.appendChild(deleteBtn);\n"
                    + "    tagsWrapper.appendChild(btnContainer);\n"
                    + "  }\n"
                    + "  nav.appendChild(tagsWrapper);\n"
                    + "  const addBtn = document.createElement('button');\n"
                    + "  addBtn.className = 'cam-btn';\n"
                    + "  addBtn.style.cssText = 'background:rgba(12,124,89,0.1);color:var(--accent);border:1px solid rgba(12,124,89,0.3);';\n"
                    + "  addBtn.onclick = startCreateTag;\n"
                    + "  addBtn.textContent = '+ ADD TAG';\n"
                    + "  nav.appendChild(addBtn);\n"
                    + "}\n"
// ─── TAG CREATION ───
                    + "let isCreatingTag     = false;\n"
                    + "let pendingTargetMesh  = null;\n"
                    + "let pendingTargetPoint = null;\n"
                    + "function startCreateTag() {\n" +
                    "  document.getElementById('tagCreatorUI').style.display = 'block';\n" +
                    "\n" +
                    "  document.getElementById('tagNameInput').value = '';\n" +
                    "\n" +
                    "  document.getElementById('tagTargetDisplay').innerText =\n" +
                    "    'Click a part on the 3D model...';\n" +
                    "\n" +
                    "  document.getElementById('pointSearchInput').value = '';\n" +
                    "\n" +
                    "  document.getElementById('pointDropdown').style.display = 'none';\n" +
                    "\n" +
                    "  document.getElementById('selectedPointDisplay').style.display = 'none';\n" +
                    "\n" +
                    "  selectedPointPath = null;\n" +
                    "  selectedPointName = null;\n" +
                    "\n" +
                    "  pendingTargetMesh = null;\n" +
                    "  pendingTargetPoint = null;\n" +
                    "\n" +
                    "  isCreatingTag = true;\n" +
                    "\n" +
                    "  setStatus(\n" +
                    "    'Tag Creation Mode: click a part on the model.'\n" +
                    "  );\n" +
                    "\n" +
                    "  // ✅ ONLY LOAD CACHE\n" +
                    "  loadNiagaraPoints();\n" +
                    "}\n"
                    + "function cancelCreateTag() {\n"
                    + "  document.getElementById('tagCreatorUI').style.display = 'none';\n"
                    + "  isCreatingTag = false; pendingTargetMesh = null; pendingTargetPoint = null;\n"
                    + "  setStatus('Tag creation cancelled.');\n"
                    + "}\n"
                    + "function saveNewTag() {\n"
                    + "  const tagName = document.getElementById('tagNameInput').value.trim();\n"
                    + "  if (!tagName) { alert('Please enter a tag name.'); return; }\n"
                    + "  if (!pendingTargetMesh || !pendingTargetPoint) { alert('Please click a part of the 3D model first.'); return; }\n"
                    + "  const viewId = 'view_' + Date.now();\n"
                    + "  views[viewId] = {\n"
                    + "    name:        tagName,\n"
                    + "    pos:         { x: camera.position.x, y: camera.position.y, z: camera.position.z },\n"
                    + "    target:      { x: controls.target.x,  y: controls.target.y,  z: controls.target.z  },\n"
                    + "    worldPoint:  { x: pendingTargetPoint.x, y: pendingTargetPoint.y, z: pendingTargetPoint.z },\n"
                    + "    pointPath:   selectedPointPath  || null,\n"
                    + "    pointName:   selectedPointName  || null\n"
                    + "  };\n"
                    + "  allModelViews[currentModelName] = views;\n"
                    + "  localStorage.setItem('niagara_3d_all_views', JSON.stringify(allModelViews));\n"
                    + "  document.getElementById('tagCreatorUI').style.display = 'none';\n"
                    + "  isCreatingTag = false; pendingTargetMesh = null; pendingTargetPoint = null;\n"
                    + "  setStatus(\"Tag '\" + tagName + \"' saved!\");\n"
                    + "  renderCameraNav();\n"
                    + "  refreshAllLabels();\n"
                    + "}\n"
                    + "function flyToView(viewId) {\n"
                    + "  if (!camera || !controls) return;\n"
                    + "  const view = views[viewId];\n"
                    + "  if (!view) return;\n"
                    + "  if (!window.gsap) {\n"
                    + "    camera.position.set(view.pos.x, view.pos.y, view.pos.z);\n"
                    + "    controls.target.set(view.target.x, view.target.y, view.target.z);\n"
                    + "    controls.update(); return;\n"
                    + "  }\n"
                    + "  gsap.to(camera.position, { x:view.pos.x, y:view.pos.y, z:view.pos.z, duration:1.5, ease:'power2.inOut' });\n"
                    + "  gsap.to(controls.target,  { x:view.target.x, y:view.target.y, z:view.target.z, duration:1.5, ease:'power2.inOut', onUpdate:function(){ controls.update(); } });\n"
                    + "}\n"
// ─── LABEL OVERLAY SYSTEM ───
                    + "var labelObjects = [];  // [{el, worldPos, viewId}]\n"
                    + "var labelValueCache = {};  // viewId -> last fetched value\n"
                    + "var labelFetchInterval = null;\n"
                    + "\n"
                    + "function clearAllLabels() {\n"
                    + "  labelObjects = [];\n"
                    + "  labelContainer.innerHTML = '';\n"
                    + "  if (labelFetchInterval) { clearInterval(labelFetchInterval); labelFetchInterval = null; }\n"
                    + "}\n"
                    + "\n"
                    + "function refreshAllLabels() {\n"
                    + "  clearAllLabels();\n"
                    + "  for (var id in views) {\n"
                    + "    var view = views[id];\n"
                    + "    if (!view.worldPoint) continue;  // old tags without worldPoint skip\n"
                    + "    createLabel(id, view);\n"
                    + "  }\n"
                    + "  // Start polling for live values every 5s\n"
                    + "  if (Object.keys(views).some(function(id){ return !!views[id].pointPath; })) {\n"
                    + "    fetchAllLabelValues();\n"
                    + "    labelFetchInterval = setInterval(fetchAllLabelValues, 5000);\n"
                    + "  }\n"
                    + "}\n"
                    + "\n"
                    + "function createLabel(viewId, view) {\n"
                    + "  var el = document.createElement('div');\n"
                    + "  el.className = 'model-label';\n"
                    + "  el.id = 'label-' + viewId;\n"
                    + "  el.style.pointerEvents = 'all';\n"
                    + "  el.innerHTML =\n"
                    + "    '<div class=\"model-label-inner\">'\n"
                    + "    + '<span class=\"model-label-name\">' + view.name + '</span>'\n"
                    + "    + '<span class=\"model-label-value\" id=\"lv-' + viewId + '\">' + (view.pointPath ? '...' : '') + '</span>'\n"
                    + "    + '</div>'\n"
                    + "    + '<div class=\"model-label-stem\"></div>'\n"
                    + "    + '<div class=\"model-label-dot\"></div>';\n"
                    + "  el.onclick = function() { flyToView(viewId); };\n"
                    + "  labelContainer.appendChild(el);\n"
                    + "  labelObjects.push({\n"
                    + "    el: el,\n"
                    + "    worldPos: new THREE.Vector3(view.worldPoint.x, view.worldPoint.y, view.worldPoint.z),\n"
                    + "    viewId: viewId\n"
                    + "  });\n"
                    + "}\n"
                    + "\n"
                    + "function updateLabelPositions() {\n"
                    + "  if (!camera || !renderer) return;\n"
                    + "  var width  = renderer.domElement.clientWidth;\n"
                    + "  var height = renderer.domElement.clientHeight;\n"
                    + "  labelObjects.forEach(function(lbl) {\n"
                    + "    var pos = lbl.worldPos.clone().project(camera);\n"
                    + "    var x   = ( pos.x * 0.5 + 0.5) * width;\n"
                    + "    var y   = (-pos.y * 0.5 + 0.5) * height;\n"
                    + "    // Hide if behind camera\n"
                    + "    if (pos.z > 1) { lbl.el.style.display = 'none'; return; }\n"
                    + "    lbl.el.style.display = '';\n"
                    + "    lbl.el.style.left = x + 'px';\n"
                    + "    lbl.el.style.top  = y + 'px';\n"
                    + "  });\n"
                    + "}\n"
                    + "\n"
                    + "async function fetchAllLabelValues() {\n"
                    + "  for (var id in views) {\n"
                    + "    var view = views[id];\n"
                    + "    if (!view.pointPath) continue;\n"
                    + "    try {\n"
                    + "      var res  = await fetch('/getPointValue?path=' + encodeURIComponent(view.pointPath));\n"
                    + "      var data = await res.json();\n"
                    + "      var valEl = document.getElementById('lv-' + id);\n"
                    + "      if (valEl) valEl.textContent = data.value;\n"
                    + "    } catch(e) {\n"
                    + "      var valEl = document.getElementById('lv-' + id);\n"
                    + "      if (valEl) valEl.textContent = 'ERR';\n"
                    + "    }\n"
                    + "  }\n"
                    + "}\n"
// ─── THREE.JS ───
                    + "let THREE, scene, camera, renderer, controls, loader;\n"
                    + "let activeModel = null, modelContainer = null;\n"
                    + "let raycaster, mouse;\n"
                    + "function loadThreeScripts() {\n"
                    + "  if (!window.THREE || !window.THREE.OrbitControls || !window.THREE.GLTFLoader)\n"
                    + "    throw new Error('Three.js library not available.');\n"
                    + "  return window.THREE;\n"
                    + "}\n"
                    + "function initViewer() {\n"
                    + "  THREE.Cache.enabled = true;\n"
                    + "  scene = new THREE.Scene();\n"
                    + "  scene.background = new THREE.Color(0xf0f4f8);\n"
                    + "  modelContainer = new THREE.Group();\n"
                    + "  scene.add(modelContainer);\n"
                    + "  camera = new THREE.PerspectiveCamera(45, viewer.clientWidth / viewer.clientHeight, 0.1, 1000);\n"
                    + "  camera.position.set(2.8, 2.2, 4.4);\n"
                    + "  renderer = new THREE.WebGLRenderer({ antialias:true, alpha:true });\n"
                    + "  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));\n"
                    + "  renderer.setSize(viewer.clientWidth, viewer.clientHeight);\n"
                    + "  renderer.outputEncoding = THREE.sRGBEncoding;\n"
                    + "  renderer.toneMapping = THREE.ACESFilmicToneMapping;\n"
                    + "  renderer.toneMappingExposure = 0.82;\n"
                    + "  viewer.appendChild(renderer.domElement);\n"
                    + "  controls = new THREE.OrbitControls(camera, renderer.domElement);\n"
                    + "  controls.enableDamping = true;\n"
                    + "  controls.target.set(0, 0.75, 0);\n"
                    + "  scene.add(new THREE.HemisphereLight(0xffffff, 0x8fa4b8, 0.65));\n"
                    + "  scene.add(new THREE.AmbientLight(0xffffff, 0.45));\n"
                    + "  const keyLight = new THREE.DirectionalLight(0xffffff, 1.15);\n"
                    + "  keyLight.position.set(5, 8, 6); scene.add(keyLight);\n"
                    + "  const fillLight = new THREE.DirectionalLight(0xb9d6ff, 0.35);\n"
                    + "  fillLight.position.set(-4, 4, -5); scene.add(fillLight);\n"
                    + "  const floor = new THREE.Mesh(\n"
                    + "    new THREE.CircleGeometry(8, 80),\n"
                    + "    new THREE.MeshStandardMaterial({ color:0xc8d7e4, transparent:true, opacity:0.75 })\n"
                    + "  );\n"
                    + "  floor.rotation.x = -Math.PI / 2;\n"
                    + "  floor.position.y = -1.1;\n"
                    + "  scene.add(floor);\n"
                    + "  loader     = new THREE.GLTFLoader();\n"
                    + "  raycaster  = new THREE.Raycaster();\n"
                    + "  mouse      = new THREE.Vector2();\n"
                    + "  viewer.addEventListener('pointerdown', onPointerDown, false);\n"
                    + "}\n"
                    + "function onPointerDown(event) {\n"
                    + "  if (!activeModel) return;\n"
                    + "  const rect = viewer.getBoundingClientRect();\n"
                    + "  mouse.x =  ((event.clientX - rect.left) / rect.width)  * 2 - 1;\n"
                    + "  mouse.y = -((event.clientY - rect.top)  / rect.height) * 2 + 1;\n"
                    + "  raycaster.setFromCamera(mouse, camera);\n"
                    + "  const intersects = raycaster.intersectObject(activeModel, true);\n"
                    + "  if (intersects.length > 0) {\n"
                    + "    if (isCreatingTag) {\n"
                    + "      pendingTargetMesh  = intersects[0].object;\n"
                    + "      pendingTargetPoint = intersects[0].point;\n"
                    + "      document.getElementById('tagTargetDisplay').innerText = pendingTargetMesh.name || 'Unnamed Mesh';\n"
                    + "      setStatus('Selected: ' + (pendingTargetMesh.name || 'Unnamed') + '. Choose a point & Save.');\n"
                    + "      return;\n"
                    + "    }\n"
                    + "    setStatus('Clicked: ' + (intersects[0].object.name || 'Unnamed Mesh'));\n"
                    + "  }\n"
                    + "}\n"
                    + "function disposeMaterial(material) {\n"
                    + "  for (const key of Object.keys(material)) {\n"
                    + "    const value = material[key];\n"
                    + "    if (value && typeof value === 'object' && 'minFilter' in value) value.dispose();\n"
                    + "  }\n"
                    + "  material.dispose();\n"
                    + "}\n"
                    + "function clearCurrentModel() {\n"
                    + "  if (modelContainer) {\n"
                    + "    while (modelContainer.children.length > 0) {\n"
                    + "      const child = modelContainer.children[0];\n"
                    + "      modelContainer.remove(child);\n"
                    + "      child.traverse(function(node) {\n"
                    + "        if (node.geometry) node.geometry.dispose();\n"
                    + "        if (node.material) {\n"
                    + "          if (Array.isArray(node.material)) node.material.forEach(disposeMaterial);\n"
                    + "          else disposeMaterial(node.material);\n"
                    + "        }\n"
                    + "      });\n"
                    + "    }\n"
                    + "  }\n"
                    + "  activeModel = null;\n"
                    + "  clearAllLabels();\n"
                    + "}\n"
                    + "function frameModel(object) {\n"
                    + "  const box = new THREE.Box3().setFromObject(object);\n"
                    + "  const size   = box.getSize(new THREE.Vector3());\n"
                    + "  const center = box.getCenter(new THREE.Vector3());\n"
                    + "  const maxSize     = Math.max(size.x, size.y, size.z);\n"
                    + "  const fitDistance = maxSize / (2 * Math.tan((Math.PI * camera.fov) / 360));\n"
                    + "  const distance    = fitDistance * 1.65;\n"
                    + "  camera.near = Math.max(0.1, maxSize / 100);\n"
                    + "  camera.far  = Math.max(1000, distance * 10);\n"
                    + "  camera.updateProjectionMatrix();\n"
                    + "  camera.position.set(center.x + distance * 0.8, center.y + distance * 0.55, center.z + distance);\n"
                    + "  controls.target.copy(center);\n"
                    + "  controls.update();\n"
                    + "}\n"
                    + "function prepareModelMaterials(object) {\n"
                    + "  object.traverse(function(child) {\n"
                    + "    if (!child.isMesh || !child.material) return;\n"
                    + "    const materials = Array.isArray(child.material) ? child.material : [child.material];\n"
                    + "    materials.forEach(function(material) {\n"
                    + "      ['map','emissiveMap','sheenColorMap'].forEach(function(key) {\n"
                    + "        if (material[key]) { material[key].encoding = THREE.sRGBEncoding; material[key].needsUpdate = true; }\n"
                    + "      });\n"
                    + "      material.needsUpdate = true;\n"
                    + "    });\n"
                    + "  });\n"
                    + "}\n"
                    + "function loadModel(model) {\n"
                    + "  setStatus('Loading model...');\n"
                    + "  clearCurrentModel();\n"
                    + "  const modelUrl = getModelUrls(model)[0];\n"
                    + "  loader.load(\n"
                    + "    modelUrl,\n"
                    + "    function(gltf) {\n"
                    + "      activeModel = gltf.scene;\n"
                    + "      prepareModelMaterials(activeModel);\n"
                    + "      modelContainer.add(activeModel);\n"
                    + "      frameModel(activeModel);\n"
                    + "      updateCameraNavVisibility(model.label);\n"
                    + "      setStatus(model.label + ' loaded \\u2714');\n"
                    + "    },\n"
                    + "    function(xhr) {\n"
                    + "      const bar = document.getElementById('loaderProgressBar');\n"
                    + "      const pct = document.getElementById('loaderPercent');\n"
                    + "      if (xhr.lengthComputable) {\n"
                    + "        const p = Math.round((xhr.loaded / xhr.total) * 100);\n"
                    + "        if (bar) { bar.style.width = p+'%'; bar.style.animation='none'; }\n"
                    + "        if (pct) pct.textContent = p+'%';\n"
                    + "      } else {\n"
                    + "        if (pct) pct.textContent = (xhr.loaded/1048576).toFixed(2)+' MB...';\n"
                    + "        if (bar) { bar.style.width='50%'; bar.style.animation='indeterminate 1.5s infinite ease-in-out'; }\n"
                    + "      }\n"
                    + "    },\n"
                    + "    function(error) { console.error(error); setStatus('Model load failed \\u274C'); }\n"
                    + "  );\n"
                    + "}\n"
                    + "function loadModelFile(file) {\n"
                    + "  if (!file) return;\n"
                    + "  setStatus('Loading ' + file.name + '...');\n"
                    + "  clearCurrentModel();\n"
                    + "  const reader = new FileReader();\n"
                    + "  reader.onprogress = function(e) {\n"
                    + "    const bar = document.getElementById('loaderProgressBar');\n"
                    + "    const pct = document.getElementById('loaderPercent');\n"
                    + "    if (e.lengthComputable) {\n"
                    + "      const p = Math.round((e.loaded / e.total) * 100);\n"
                    + "      if (bar) { bar.style.width = p+'%'; bar.style.animation='none'; }\n"
                    + "      if (pct) pct.textContent = p+'%';\n"
                    + "    }\n"
                    + "  };\n"
                    + "  reader.onload = function() {\n"
                    + "    loader.parse(reader.result, '', function(gltf) {\n"
                    + "      activeModel = gltf.scene;\n"
                    + "      prepareModelMaterials(activeModel);\n"
                    + "      modelContainer.add(activeModel);\n"
                    + "      frameModel(activeModel);\n"
                    + "      updateCameraNavVisibility(file.name);\n"
                    + "      setStatus(file.name + ' rendered \\u2714');\n"
                    + "    }, function(err) { console.error(err); setStatus('Could not parse model.'); });\n"
                    + "  };\n"
                    + "  reader.onerror = function() { setStatus('File could not be read.'); };\n"
                    + "  reader.readAsArrayBuffer(file);\n"
                    + "}\n"
                    + "window.addEventListener('resize', function() {\n"
                    + "  if (!renderer) return;\n"
                    + "  camera.aspect = viewer.clientWidth / viewer.clientHeight;\n"
                    + "  camera.updateProjectionMatrix();\n"
                    + "  renderer.setSize(viewer.clientWidth, viewer.clientHeight);\n"
                    + "});\n"
                    + "function animate() {\n"
                    + "  requestAnimationFrame(animate);\n"
                    + "  controls.update();\n"
                    + "  renderer.render(scene, camera);\n"
                    + "  updateLabelPositions();\n"  // ← project labels every frame
                    + "}\n"
// ─── STARTUP ───
                    + "async function start() {\n"
                    + "  setStatus('Loading 3D library...');\n"
                    + "  THREE = loadThreeScripts();\n"
                    + "  initViewer();\n"
                    + "  animate();\n"
                    + "  setStatus('Viewer ready.');\n"
                    + "  initPointSearch();\n"
                    + "  loadNiagaraPoints();\n"  // preload points in background
                    + "  await loadFileSystem();\n"
                    + "  let firstModel = null;\n"
                    + "  for (const folder of virtualFS) {\n"
                    + "    if (folder.files && folder.files.length > 0) { firstModel = folder.files[0]; break; }\n"
                    + "  }\n"
                    + "  if (firstModel) {\n"
                    + "    activeFileId = firstModel.id;\n"
                    + "    renderVirtualFS();\n"
                    + "    loadModel({ label: firstModel.name, fileNames: firstModel.fileNames, fullPath: firstModel.fullPath });\n"
                    + "  } else {\n"
                    + "    setStatus('No model found to load.');\n"
                    + "  }\n"
                    + "}\n"
                    + "start().catch(function(err) { console.error(err); setStatus('Startup error: ' + err.message); });\n"
                    + "</script>\n"
                    + "</body>\n"
                    + "</html>";

            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");
            byte[] bytes = html.getBytes("UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COMMON HELPERS
    // ─────────────────────────────────────────────────────────────────────────
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
        try (OutputStream os = exchange.getResponseBody()) { os.write(response.getBytes()); }
    }

    @FunctionalInterface
    private interface RouteAction {
        JSONObject execute() throws Exception;
    }
}