package com.mayvel.objectViewer.route;

import com.mayvel.objectViewer.controller.ConfigurationController;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.tridium.json.JSONArray;
import com.tridium.json.JSONObject;

import javax.baja.sys.Sys;
import java.io.*;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class ObjectViewerRoute {

    public static void registerRoutes(HttpServer server) {
        server.createContext("/objectviewer", new ObjectViewerHandler());
        server.createContext("/uploadModel", new UploadHandler());
        server.createContext("/listFiles",  new ListFilesHandler());
        server.createContext("/createFolder", new CreateFolderHandler());
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
                    while ((read = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                    }
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
                                        if (f.isFile()) {
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

                // Add CORS headers so the page can read the JSON
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
    // MAIN PAGE HANDLER
    // ─────────────────────────────────────────────────────────────────────────
    static class ObjectViewerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {

            // NOTE: getModelUrls now sends  /getModel?folder=<folder>&name=<file>
            // Make sure ModelFileRoute.register() handles that query format.

            String html = "<!DOCTYPE html>\n"
                    + "<html lang=\"en\">\n"
                    + "<head>\n"
                    + "  <meta charset=\"UTF-8\">\n"
                    + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                    + "  <title>3D Model Viewer</title>\n"
                    + "  <style>\n"
                    + "    :root {\n"
                    + "      color-scheme: light;\n"
                    + "      --bg: #eef3f7;\n"
                    + "      --panel-bg: rgba(255,255,255,0.9);\n"
                    + "      --panel-border: rgba(20,42,61,0.14);\n"
                    + "      --text: #142a3d;\n"
                    + "      --text-light: #627384;\n"
                    + "      --muted: #627384;\n"
                    + "      --accent: #0c7c59;\n"
                    + "      --accent-dark: #095b42;\n"
                    + "      --bg-color: #eef3f7;\n"
                    + "      --radius: 16px;\n"
                    + "      --radius-sm: 8px;\n"
                    + "      --shadow: 0 18px 45px rgba(22,39,56,0.14);\n"
                    + "      --shadow-sm: 0 4px 12px rgba(22,39,56,0.08);\n"
                    + "    }\n"
                    + "    * { box-sizing: border-box; }\n"
                    + "    body, html { margin:0; padding:0; height:100%; font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",Roboto,Helvetica,Arial,sans-serif; background-color:var(--bg-color); color:var(--text); overflow:hidden; }\n"
                    + "\n"
                    + "    /* ── Top Bar ── */\n"
                    + "    .top-bar { width:100%; height:60px; background:#2f3640; color:white; display:flex; justify-content:space-between; align-items:center; padding:0 20px; position:fixed; top:0; left:0; z-index:1000; }\n"
                    + "    .top-bar button { padding:6px 12px; border:none; border-radius:5px; cursor:pointer; }\n"
                    + "    .top-bar button:first-child { background:#40739e; color:white; }\n"
                    + "    .top-bar button:last-child  { background:#e84118; color:white; }\n"
                    + "\n"
                    + "    /* ── Layout ── */\n"
                    + "    .app-container { display:flex; height:calc(100vh - 60px); margin-top:60px; background:radial-gradient(circle at center,#ffffff 0%,var(--bg-color) 100%); }\n"
                    + "\n"
                    + "    /* ── Sidebar ── */\n"
                    + "    .sidebar { width:320px; background:var(--panel-bg); backdrop-filter:blur(20px); -webkit-backdrop-filter:blur(20px); border-right:1px solid var(--panel-border); display:flex; flex-direction:column; box-shadow:2px 0 20px rgba(0,0,0,0.03); z-index:10; }\n"
                    + "    .sidebar-header { padding:24px; border-bottom:1px solid rgba(0,0,0,0.05); display:flex; justify-content:space-between; align-items:center; }\n"
                    + "    .sidebar-header h2 { margin:0; font-size:20px; font-weight:700; letter-spacing:-0.5px; background:linear-gradient(135deg,var(--text) 0%,#4a5568 100%); -webkit-background-clip:text; -webkit-text-fill-color:transparent; }\n"
                    + "    .file-browser { flex:1; overflow-y:auto; padding:16px; }\n"
                    + "    .folder-item { margin-bottom:12px; }\n"
                    + "    .folder-header { display:flex; align-items:center; padding:10px 14px; cursor:pointer; border-radius:var(--radius-sm); transition:all 0.2s ease; font-weight:600; color:var(--text); }\n"
                    + "    .folder-header:hover { background:rgba(0,0,0,0.03); }\n"
                    + "    .file-list { margin:4px 0 8px 16px; border-left:2px solid rgba(0,0,0,0.05); padding-left:8px; }\n"
                    + "    .file-item { display:flex; align-items:center; padding:8px 12px; cursor:pointer; border-radius:var(--radius-sm); transition:all 0.2s; font-size:14px; color:var(--text-light); margin-bottom:4px; }\n"
                    + "    .file-item:hover { background:rgba(37,99,235,0.05); color:var(--accent); transform:translateX(4px); }\n"
                    + "    .file-item.active { background:rgba(37,99,235,0.1); color:var(--accent); font-weight:600; }\n"
                    + "    .upload-btn { width:calc(100% - 24px); margin:8px 12px 16px 12px; padding:10px; border:1px dashed var(--accent); border-radius:var(--radius-sm); background:rgba(37,99,235,0.03); color:var(--accent); cursor:pointer; display:flex; align-items:center; justify-content:center; gap:8px; font-size:13px; font-weight:600; transition:all 0.2s; }\n"
                    + "    .upload-btn:hover { background:rgba(37,99,235,0.1); transform:translateY(-1px); }\n"
                    + "    .status-container { padding:16px 24px; border-top:1px solid rgba(0,0,0,0.05); margin-top:auto; background:rgba(255,255,255,0.5); }\n"
                    + "    .progress-bar { height:6px; width:0%; background:var(--accent); border-radius:3px; transition:width 0.2s ease-out; }\n"
                    + "    .status { font-size:12px; color:var(--text-light); margin:8px 0 0 0; }\n"
                    + "\n"
                    + "    /* ── Viewer ── */\n"
                    + "    .viewer-shell { flex:1; position:relative; border-radius:var(--radius); overflow:hidden; box-shadow:var(--shadow); margin:16px; background:#ffffff; }\n"
                    + "    #viewer { width:100%; height:100%; display:block; }\n"
                    + "    .viewer-badge { position:absolute; bottom:18px; right:24px; background:rgba(255,255,255,0.8); backdrop-filter:blur(10px); -webkit-backdrop-filter:blur(10px); padding:8px 16px; border-radius:20px; font-size:13px; color:var(--text-light); box-shadow:var(--shadow-sm); z-index:10; }\n"
                    + "\n"
                    + "    /* ── Camera Nav ── */\n"
                    + "    .camera-nav { position:absolute; top:24px; left:50%; transform:translateX(-50%); display:flex; flex-direction:column; align-items:center; gap:12px; z-index:10; background:rgba(240,244,248,0.45); backdrop-filter:blur(24px); -webkit-backdrop-filter:blur(24px); padding:14px 18px; border-radius:24px; box-shadow:0 12px 40px rgba(0,0,0,0.08),inset 0 1px 2px rgba(255,255,255,0.8); border:1px solid rgba(255,255,255,0.5); max-width:85%; }\n"
                    + "    .cam-btn { background:rgba(255,255,255,0.95); border:1px solid rgba(0,0,0,0.04); color:var(--text); padding:10px 18px; border-radius:24px; font-size:11px; font-weight:700; cursor:pointer; display:flex; align-items:center; gap:6px; transition:all 0.3s cubic-bezier(0.2,0.8,0.2,1); text-transform:uppercase; letter-spacing:0.5px; white-space:nowrap; flex-shrink:0; box-shadow:0 2px 8px rgba(0,0,0,0.04); }\n"
                    + "    .cam-btn:hover { background:var(--accent); color:white; transform:translateY(-2px) scale(1.02); box-shadow:0 8px 20px rgba(37,99,235,0.25); border-color:var(--accent); }\n"
                    + "\n"
                    + "    /* ── Loader Overlay ── */\n"
                    + "    #loaderOverlay { display:none; position:absolute; top:0; left:0; width:100%; height:100%; background:rgba(255,255,255,0.7); backdrop-filter:blur(4px); -webkit-backdrop-filter:blur(4px); z-index:9999; flex-direction:column; align-items:center; justify-content:center; }\n"
                    + "\n"
                    + "    @keyframes spin { to { transform:rotate(360deg); } }\n"
                    + "    @keyframes indeterminate { 0% { transform:translateX(-200%); } 100% { transform:translateX(200%); } }\n"
                    + "  </style>\n"
                    + "</head>\n"
                    + "<body>\n"
                    + "\n"
                    + "<!-- TOP BAR -->\n"
                    + "<div class=\"top-bar\">\n"
                    + "  <div>3D Object Viewer</div>\n"
                    + "  <div style=\"display:flex;gap:10px;\">\n"
                    + "    <button onclick=\"openSettings()\">⚙️</button>\n"
                    + "    <button onclick=\"logout()\">Logout</button>\n"
                    + "  </div>\n"
                    + "</div>\n"
                    + "\n"
                    + "<!-- LOADER OVERLAY (inside viewer-shell, appended via JS) -->\n"
                    + "<div id=\"loaderOverlay\">\n"
                    + "  <div style=\"width:40px;height:40px;border:4px solid rgba(37,99,235,0.2);border-top-color:var(--accent);border-radius:50%;animation:spin 1s linear infinite;margin-bottom:16px;\"></div>\n"
                    + "  <div id=\"loaderText\" style=\"font-size:16px;font-weight:600;color:var(--text);margin-bottom:12px;\">Loading...</div>\n"
                    + "  <div style=\"width:240px;height:6px;background:rgba(0,0,0,0.1);border-radius:3px;overflow:hidden;margin-bottom:8px;position:relative;\">\n"
                    + "    <div id=\"loaderProgressBar\" style=\"width:0%;height:100%;background:var(--accent);transition:width 0.2s;position:absolute;left:0;\"></div>\n"
                    + "  </div>\n"
                    + "  <div id=\"loaderPercent\" style=\"font-size:13px;color:var(--muted);font-weight:500;\"></div>\n"
                    + "</div>\n"
                    + "\n"
                    + "<main class=\"app-container\">\n"
                    + "\n"
                    + "  <!-- SIDEBAR -->\n"
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
                    + "\n"
                    + "  <!-- VIEWER -->\n"
                    + "  <section class=\"viewer-shell\">\n"
                    + "    <div class=\"viewer-badge\">Orbit: drag · Scroll: zoom</div>\n"
                    + "    <div id=\"cameraNav\" class=\"camera-nav\" style=\"display:none;\"></div>\n"
                    + "\n"
                    + "    <!-- Tag Creator UI -->\n"
                    + "    <div id=\"tagCreatorUI\" style=\"display:none;position:absolute;top:80px;right:24px;background:rgba(255,255,255,0.95);padding:16px;border-radius:8px;box-shadow:var(--shadow);z-index:1000;min-width:250px;font-size:14px;border:1px solid var(--panel-border);\">\n"
                    + "      <h4 style=\"margin:0 0 12px 0;color:var(--text);\">Create New Tag</h4>\n"
                    + "      <div style=\"margin-bottom:12px;\">\n"
                    + "        <label style=\"display:block;margin-bottom:4px;color:var(--muted);font-weight:500;\">Tag Name</label>\n"
                    + "        <input type=\"text\" id=\"tagNameInput\" placeholder=\"e.g. Filter Check\" style=\"width:100%;padding:8px;border:1px solid #ccc;border-radius:4px;box-sizing:border-box;\">\n"
                    + "      </div>\n"
                    + "      <div style=\"margin-bottom:16px;\">\n"
                    + "        <label style=\"display:block;margin-bottom:4px;color:var(--muted);font-weight:500;\">Focus Part</label>\n"
                    + "        <div id=\"tagTargetDisplay\" style=\"width:100%;padding:8px;background:#f0f4f8;border:1px solid #d9e4ec;border-radius:4px;box-sizing:border-box;color:var(--text);min-height:35px;word-break:break-all;\">Click a part on the 3D model...</div>\n"
                    + "      </div>\n"
                    + "      <div style=\"display:flex;gap:8px;justify-content:flex-end;\">\n"
                    + "        <button onclick=\"cancelCreateTag()\" style=\"padding:6px 12px;border:1px solid #ccc;background:white;border-radius:4px;cursor:pointer;\">Cancel</button>\n"
                    + "        <button onclick=\"saveNewTag()\" style=\"padding:6px 12px;border:none;background:var(--accent);color:white;border-radius:4px;cursor:pointer;font-weight:500;\">Save Tag</button>\n"
                    + "      </div>\n"
                    + "    </div>\n"
                    + "\n"
                    + "    <div id=\"viewer\"></div>\n"
                    + "  </section>\n"
                    + "</main>\n"
                    + "\n"
                    + "<!-- Three.js + GSAP -->\n"
                    + "<script src=\"https://cdn.jsdelivr.net/npm/three@0.128.0/build/three.min.js\"></script>\n"
                    + "<script src=\"https://cdn.jsdelivr.net/npm/three@0.128.0/examples/js/controls/OrbitControls.js\"></script>\n"
                    + "<script src=\"https://cdn.jsdelivr.net/npm/three@0.128.0/examples/js/loaders/GLTFLoader.js\"></script>\n"
                    + "<script src=\"https://cdn.jsdelivr.net/npm/gsap@3.12.2/dist/gsap.min.js\"></script>\n"
                    + "\n"
                    + "<script>\n"
                    + "// ─────────────────────────────────────────────\n"
                    + "// GLOBALS\n"
                    + "// ─────────────────────────────────────────────\n"
                    + "let virtualFS = [];          // ← FIX 1: was missing, caused ReferenceError\n"
                    + "let activeFolderId = null;\n"
                    + "let activeFileId   = null;\n"
                    + "\n"
                    + "const fileInput              = document.getElementById('fileInput');\n"
                    + "const status                 = document.getElementById('status');\n"
                    + "const viewer                 = document.getElementById('viewer');\n"
                    + "const uploadProgressContainer= document.getElementById('uploadProgressContainer');\n"
                    + "const progressBar            = document.getElementById('progressBar');\n"
                    + "const cameraNav              = document.getElementById('cameraNav');\n"
                    + "\n"
                    + "// Move loaderOverlay inside viewer-shell so position:absolute works\n"
                    + "document.querySelector('.viewer-shell').prepend(document.getElementById('loaderOverlay'));\n"
                    + "\n"
                    + "// ─────────────────────────────────────────────\n"
                    + "// UTILITY\n"
                    + "// ─────────────────────────────────────────────\n"
                    + "function setStatus(message) {\n"
                    + "  status.textContent = message;\n"
                    + "  const overlay      = document.getElementById('loaderOverlay');\n"
                    + "  const loaderText   = document.getElementById('loaderText');\n"
                    + "  const loaderBar    = document.getElementById('loaderProgressBar');\n"
                    + "  const loaderPct    = document.getElementById('loaderPercent');\n"
                    + "  if (overlay && loaderText) {\n"
                    + "    if (message.toLowerCase().includes('loading')) {\n"
                    + "      overlay.style.display = 'flex';\n"
                    + "      loaderText.textContent = message;\n"
                    + "      if (loaderBar) { loaderBar.style.width = '0%'; loaderBar.style.animation = 'none'; }\n"
                    + "      if (loaderPct) loaderPct.textContent = 'Connecting...';\n"
                    + "    } else {\n"
                    + "      overlay.style.display = 'none';\n"
                    + "    }\n"
                    + "  }\n"
                    + "}\n"
                    + "\n"
                    + "function logout()       { window.location.href = '/'; }\n"
                    + "function openSettings() { alert('Settings coming soon'); }\n"
                    + "\n"
                    + "// ─────────────────────────────────────────────\n"
                    + "// FIX 2: getModelUrls now includes folderName\n"
                    + "// URL format: /getModel?folder=<folder>&name=<file>\n"
                    + "// ─────────────────────────────────────────────\n"
                    + "function getModelUrls(model) {\n" +
                    "  return model.fileNames.map(function(fileName) {\n" +
                    "    return '/getModel?path=' + encodeURIComponent(model.fullPath + '/' + fileName);\n" +
                    "  });\n" +
                    "}\n"
                    + "\n"
                    + "// ─────────────────────────────────────────────\n"
                    + "// VIRTUAL FS RENDERER\n"
                    + "// ─────────────────────────────────────────────\n"
                    + "function renderVirtualFS() {\n"
                    + "  const browser = document.getElementById('fileBrowser');\n"
                    + "  browser.innerHTML = '';\n"
                    + "\n"
                    + "  if (!virtualFS || virtualFS.length === 0) {\n"
                    + "    browser.innerHTML = '<p style=\"color:var(--muted);font-size:13px;padding:12px;\">No folders yet. Click + to create one.</p>';\n"
                    + "    return;\n"
                    + "  }\n"
                    + "\n"
                    + "  virtualFS.forEach(function(folder) {\n"
                    + "    const folderEl = document.createElement('div');\n"
                    + "    folderEl.className = 'folder-item';\n"
                    + "\n"
                    + "    const folderHeader = document.createElement('div');\n"
                    + "    folderHeader.className = 'folder-header';\n"
                    + "    folderHeader.innerHTML = '📁 ' + folder.name;\n"
                    + "    folderHeader.onclick = function() {\n"
                    + "      folder.expanded = !folder.expanded;\n"
                    + "      renderVirtualFS();\n"
                    + "    };\n"
                    + "    folderEl.appendChild(folderHeader);\n"
                    + "\n"
                    + "    if (folder.expanded) {\n"
                    + "      const folderContent = document.createElement('div');\n"
                    + "      folderContent.className = 'folder-content';\n"
                    + "\n"
                    + "      // Upload button\n"
                    + "      const uploadBtn = document.createElement('button');\n"
                    + "      uploadBtn.className = 'upload-btn';\n"
                    + "      uploadBtn.innerHTML = '⬆ Upload .glb';\n"
                    + "      uploadBtn.onclick = function(e) {\n"
                    + "        e.stopPropagation();\n"
                    + "        activeFolderId = folder.id;\n"
                    + "        fileInput.click();\n"
                    + "      };\n"
                    + "      folderContent.appendChild(uploadBtn);\n"
                    + "\n"
                    + "      // File list\n"
                    + "      const fileList = document.createElement('div');\n"
                    + "      fileList.className = 'file-list';\n"
                    + "\n"
                    + "      if (!folder.files || folder.files.length === 0) {\n"
                    + "        const empty = document.createElement('div');\n"
                    + "        empty.style.cssText = 'font-size:12px;color:var(--muted);padding:6px 12px;';\n"
                    + "        empty.textContent = 'No files yet';\n"
                    + "        fileList.appendChild(empty);\n"
                    + "      } else {\n"
                    + "        folder.files.forEach(function(file) {\n"
                    + "          const fileEl = document.createElement('div');\n"
                    + "          fileEl.className = 'file-item' + (activeFileId === file.id ? ' active' : '');\n"
                    + "          fileEl.innerHTML = '🗂 ' + file.name;\n"
                    + "          fileEl.onclick = function(e) {\n"
                    + "            e.stopPropagation();\n"
                    + "            activeFileId = file.id;\n"
                    + "            renderVirtualFS();\n"
                    + "            if (file.type === 'server') {\n"
                    + "              loadModel({\n" +
                    "  label: file.name,\n" +
                    "  fileNames: file.fileNames,\n" +
                    "  fullPath: file.fullPath\n" +
                    "});\n"
                    + "            } else if (file.type === 'local') {\n"
                    + "              loadModelFile(file.file);\n"
                    + "            }\n"
                    + "          };\n"
                    + "          fileList.appendChild(fileEl);\n"
                    + "        });\n"
                    + "      }\n"
                    + "\n"
                    + "      folderContent.appendChild(fileList);\n"
                    + "      folderEl.appendChild(folderContent);\n"
                    + "    }\n"
                    + "\n"
                    + "    browser.appendChild(folderEl);\n"
                    + "  });\n"
                    + "}\n"
                    + "\n"
                    + "// ─────────────────────────────────────────────\n"
                    + "// FIX 3: loadFileSystem — single catch block\n"
                    + "// ─────────────────────────────────────────────\n"
                    + "virtualFS = [];\n" +
                    "\n" +
                    "function buildFS(data, parentPath = \"\") {\n" +
                    "  return data.map(folder => {\n" +
                    "    const currentPath = parentPath ? parentPath + \"/\" + folder.name : folder.name;\n" +
                    "\n" +
                    "    return {\n" +
                    "      id: 'folder-' + currentPath,\n" +
                    "      name: folder.name,\n" +
                    "      fullPath: currentPath,\n" +
                    "      expanded: true,\n" +
                    "      files: (folder.files || []).map(file => ({\n" +
                    "        id: 'file-' + currentPath + '-' + file,\n" +
                    "        name: file,\n" +
                    "        type: 'server',\n" +
                    "        fileNames: [file],\n" +
                    "        fullPath: currentPath\n" +
                    "      })),\n" +
                    "      children: buildFS(folder.folders || [], currentPath)\n" +
                    "    };\n" +
                    "  });\n" +
                    "}\n" +
                    "\n" +
                    "async function loadFileSystem() {\n" +
                    "  const res = await fetch('/listFiles');\n" +
                    "  const data = await res.json();\n" +
                    "\n" +
                    "  virtualFS = buildFS(data);\n" +
                    "  renderVirtualFS();\n" +
                    "}"
                    + "\n"
                    + "// ─────────────────────────────────────────────\n"
                    + "// NEW FOLDER\n"
                    + "// ─────────────────────────────────────────────\n"
                    + "document.getElementById('newFolderBtn').addEventListener('click', async function() {\n"
                    + "  const name = prompt('Enter folder name:');\n"
                    + "  if (!name || !name.trim()) return;\n"
                    + "  try {\n"
                    + "    const res = await fetch('/createFolder?name=' + encodeURIComponent(name.trim()), { method: 'POST' });\n"
                    + "    if (!res.ok) throw new Error('HTTP ' + res.status);\n"
                    + "    await loadFileSystem();\n"
                    + "    setStatus('Folder created: ' + name);\n"
                    + "  } catch (err) {\n"
                    + "    alert('Folder creation failed: ' + err.message);\n"
                    + "    console.error(err);\n"
                    + "  }\n"
                    + "});\n"
                    + "\n"
                    + "// ─────────────────────────────────────────────\n"
                    + "// FILE UPLOAD\n"
                    + "// ─────────────────────────────────────────────\n"
                    + "fileInput.addEventListener('change', function() {\n"
                    + "  const file = fileInput.files[0];\n"
                    + "  if (!file || !activeFolderId) return;\n"
                    + "\n"
                    + "  const folder = virtualFS.find(function(f) { return f.id === activeFolderId; });\n"
                    + "  if (!folder) return;\n"
                    + "\n"
                    + "  setStatus('Uploading...');\n"
                    + "  const url = '/uploadModel?folder=' + encodeURIComponent(folder.name) + '&name=' + encodeURIComponent(file.name);\n"
                    + "\n"
                    + "  uploadProgressContainer.style.display = 'block';\n"
                    + "  progressBar.style.width = '0%';\n"
                    + "\n"
                    + "  const xhr = new XMLHttpRequest();\n"
                    + "  xhr.open('PUT', url, true);\n"
                    + "\n"
                    + "  const csrfMatch = document.cookie.match(/(^| )niagara_csrf=([^;]+)/);\n"
                    + "  if (csrfMatch) xhr.setRequestHeader('X-Niagara-Csrf-Token', csrfMatch[2]);\n"
                    + "  xhr.setRequestHeader('Content-Type', file.type || 'application/octet-stream');\n"
                    + "\n"
                    + "  xhr.upload.onprogress = function(e) {\n"
                    + "    if (e.lengthComputable) {\n"
                    + "      progressBar.style.width = Math.round((e.loaded / e.total) * 100) + '%';\n"
                    + "    }\n"
                    + "  };\n"
                    + "\n"
                    + "  xhr.onload = function() {\n"
                    + "    setTimeout(function() { uploadProgressContainer.style.display = 'none'; }, 1500);\n"
                    + "    if (xhr.status >= 200 && xhr.status < 300) {\n"
                    + "      setStatus('Uploaded to ' + folder.name + '/' + file.name);\n"
                    + "      // Reload FS so the new file appears from server\n"
                    + "      loadFileSystem().then(function() {\n"
                    + "        // Also immediately preview the uploaded file\n"
                    + "        loadModelFile(file);\n"
                    + "      });\n"
                    + "    } else {\n"
                    + "      setStatus('Upload failed (HTTP ' + xhr.status + '). Previewing locally.');\n"
                    + "      // Still let user see the model locally even if server rejected\n"
                    + "      const newFile = { id:'file-'+Date.now(), name:file.name, type:'local', file:file };\n"
                    + "      folder.files.push(newFile);\n"
                    + "      renderVirtualFS();\n"
                    + "      loadModelFile(file);\n"
                    + "    }\n"
                    + "  };\n"
                    + "\n"
                    + "  xhr.onerror = function() {\n"
                    + "    uploadProgressContainer.style.display = 'none';\n"
                    + "    setStatus('Upload error. Previewing locally.');\n"
                    + "    const newFile = { id:'file-'+Date.now(), name:file.name, type:'local', file:file };\n"
                    + "    folder.files.push(newFile);\n"
                    + "    renderVirtualFS();\n"
                    + "    loadModelFile(file);\n"
                    + "  };\n"
                    + "\n"
                    + "  xhr.send(file);\n"
                    + "  fileInput.value = '';\n"
                    + "});\n"
                    + "\n"
                    + "// ─────────────────────────────────────────────\n"
                    + "// CAMERA / TAG VIEWS\n"
                    + "// ─────────────────────────────────────────────\n"
                    + "let allModelViews = JSON.parse(localStorage.getItem('niagara_3d_all_views')) || {};\n"
                    + "let currentModelName = '';\n"
                    + "let views = {};\n"
                    + "\n"
                    + "function updateCameraNavVisibility(modelName) {\n"
                    + "  currentModelName = modelName.replace('.glb', '').trim();\n"
                    + "  if (!allModelViews[currentModelName]) allModelViews[currentModelName] = {};\n"
                    + "  views = allModelViews[currentModelName];\n"
                    + "  if (cameraNav) { cameraNav.style.display = 'flex'; renderCameraNav(); }\n"
                    + "}\n"
                    + "\n"
                    + "function renderCameraNav() {\n"
                    + "  const nav = document.getElementById('cameraNav');\n"
                    + "  if (!nav) return;\n"
                    + "  nav.innerHTML = '';\n"
                    + "\n"
                    + "  const tagsWrapper = document.createElement('div');\n"
                    + "  tagsWrapper.style.cssText = 'display:flex;flex-wrap:wrap;justify-content:center;gap:10px;max-height:120px;overflow-y:auto;width:100%;padding-right:4px;';\n"
                    + "\n"
                    + "  for (const [id, view] of Object.entries(views)) {\n"
                    + "    const btnContainer = document.createElement('div');\n"
                    + "    btnContainer.className = 'cam-btn';\n"
                    + "    btnContainer.style.cssText = 'padding:6px 8px 6px 16px;';\n"
                    + "    btnContainer.onclick = function() { flyToView(id); };\n"
                    + "\n"
                    + "    const textSpan = document.createElement('span');\n"
                    + "    textSpan.textContent = view.name.toUpperCase();\n"
                    + "\n"
                    + "    const deleteBtn = document.createElement('button');\n"
                    + "    deleteBtn.innerHTML = '✕';\n"
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
                    + "      }\n"
                    + "    };\n"
                    + "\n"
                    + "    btnContainer.appendChild(textSpan);\n"
                    + "    btnContainer.appendChild(deleteBtn);\n"
                    + "    tagsWrapper.appendChild(btnContainer);\n"
                    + "  }\n"
                    + "  nav.appendChild(tagsWrapper);\n"
                    + "\n"
                    + "  const addBtn = document.createElement('button');\n"
                    + "  addBtn.className = 'cam-btn';\n"
                    + "  addBtn.style.cssText = 'background:rgba(12,124,89,0.1);color:var(--accent);border:1px solid rgba(12,124,89,0.3);';\n"
                    + "  addBtn.onclick = startCreateTag;\n"
                    + "  addBtn.textContent = '+ ADD TAG';\n"
                    + "  nav.appendChild(addBtn);\n"
                    + "}\n"
                    + "\n"
                    + "let isCreatingTag    = false;\n"
                    + "let pendingTargetMesh  = null;\n"
                    + "let pendingTargetPoint = null;\n"
                    + "\n"
                    + "function startCreateTag() {\n"
                    + "  document.getElementById('tagCreatorUI').style.display = 'block';\n"
                    + "  document.getElementById('tagNameInput').value = '';\n"
                    + "  document.getElementById('tagTargetDisplay').innerText = 'Click a part on the 3D model...';\n"
                    + "  pendingTargetMesh = null; pendingTargetPoint = null;\n"
                    + "  isCreatingTag = true;\n"
                    + "  setStatus('Tag Creation Mode: click a part on the model.');\n"
                    + "}\n"
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
                    + "    name:   tagName,\n"
                    + "    pos:    { x: camera.position.x, y: camera.position.y, z: camera.position.z },\n"
                    + "    target: { x: controls.target.x,  y: controls.target.y,  z: controls.target.z  }\n"
                    + "  };\n"
                    + "  localStorage.setItem('niagara_3d_all_views', JSON.stringify(allModelViews));\n"
                    + "  document.getElementById('tagCreatorUI').style.display = 'none';\n"
                    + "  isCreatingTag = false; pendingTargetMesh = null; pendingTargetPoint = null;\n"
                    + "  setStatus(\"Tag '\" + tagName + \"' saved!\");\n"
                    + "  renderCameraNav();\n"
                    + "}\n"
                    + "function flyToView(viewName) {\n"
                    + "  if (!camera || !controls) return;\n"
                    + "  const view = views[viewName];\n"
                    + "  if (!view) return;\n"
                    + "  if (!window.gsap) {\n"
                    + "    camera.position.set(view.pos.x, view.pos.y, view.pos.z);\n"
                    + "    controls.target.set(view.target.x, view.target.y, view.target.z);\n"
                    + "    controls.update(); return;\n"
                    + "  }\n"
                    + "  gsap.to(camera.position, { x:view.pos.x, y:view.pos.y, z:view.pos.z, duration:1.5, ease:'power2.inOut' });\n"
                    + "  gsap.to(controls.target,  { x:view.target.x, y:view.target.y, z:view.target.z, duration:1.5, ease:'power2.inOut', onUpdate:function(){ controls.update(); } });\n"
                    + "}\n"
                    + "\n"
                    + "// ─────────────────────────────────────────────\n"
                    + "// THREE.JS VIEWER\n"
                    + "// ─────────────────────────────────────────────\n"
                    + "let THREE, scene, camera, renderer, controls, loader;\n"
                    + "let activeModel = null, modelContainer = null;\n"
                    + "let raycaster, mouse;\n"
                    + "\n"
                    + "function loadThreeScripts() {\n"
                    + "  if (!window.THREE || !window.THREE.OrbitControls || !window.THREE.GLTFLoader)\n"
                    + "    throw new Error('Three.js library not available.');\n"
                    + "  return window.THREE;\n"
                    + "}\n"
                    + "\n"
                    + "function initViewer() {\n"
                    + "  THREE.Cache.enabled = true;\n"
                    + "  scene = new THREE.Scene();\n"
                    + "  scene.background = new THREE.Color(0xf0f4f8);\n"
                    + "  modelContainer = new THREE.Group();\n"
                    + "  scene.add(modelContainer);\n"
                    + "\n"
                    + "  camera = new THREE.PerspectiveCamera(45, viewer.clientWidth / viewer.clientHeight, 0.1, 1000);\n"
                    + "  camera.position.set(2.8, 2.2, 4.4);\n"
                    + "\n"
                    + "  renderer = new THREE.WebGLRenderer({ antialias:true, alpha:true });\n"
                    + "  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));\n"
                    + "  renderer.setSize(viewer.clientWidth, viewer.clientHeight);\n"
                    + "  renderer.outputEncoding = THREE.sRGBEncoding;\n"
                    + "  renderer.toneMapping = THREE.ACESFilmicToneMapping;\n"
                    + "  renderer.toneMappingExposure = 0.82;\n"
                    + "  viewer.appendChild(renderer.domElement);\n"
                    + "\n"
                    + "  controls = new THREE.OrbitControls(camera, renderer.domElement);\n"
                    + "  controls.enableDamping = true;\n"
                    + "  controls.target.set(0, 0.75, 0);\n"
                    + "\n"
                    + "  scene.add(new THREE.HemisphereLight(0xffffff, 0x8fa4b8, 0.65));\n"
                    + "  scene.add(new THREE.AmbientLight(0xffffff, 0.45));\n"
                    + "  const keyLight = new THREE.DirectionalLight(0xffffff, 1.15);\n"
                    + "  keyLight.position.set(5, 8, 6);\n"
                    + "  scene.add(keyLight);\n"
                    + "  const fillLight = new THREE.DirectionalLight(0xb9d6ff, 0.35);\n"
                    + "  fillLight.position.set(-4, 4, -5);\n"
                    + "  scene.add(fillLight);\n"
                    + "\n"
                    + "  const floor = new THREE.Mesh(\n"
                    + "    new THREE.CircleGeometry(8, 80),\n"
                    + "    new THREE.MeshStandardMaterial({ color:0xc8d7e4, transparent:true, opacity:0.75 })\n"
                    + "  );\n"
                    + "  floor.rotation.x = -Math.PI / 2;\n"
                    + "  floor.position.y = -1.1;\n"
                    + "  scene.add(floor);\n"
                    + "\n"
                    + "  loader = new THREE.GLTFLoader();\n"
                    + "  raycaster = new THREE.Raycaster();\n"
                    + "  mouse     = new THREE.Vector2();\n"
                    + "  viewer.addEventListener('pointerdown', onPointerDown, false);\n"
                    + "}\n"
                    + "\n"
                    + "function onPointerDown(event) {\n"
                    + "  if (!activeModel) return;\n"
                    + "  const rect = viewer.getBoundingClientRect();\n"
                    + "  mouse.x = ((event.clientX - rect.left) / rect.width)  *  2 - 1;\n"
                    + "  mouse.y = -((event.clientY - rect.top)  / rect.height) *  2 + 1;\n"
                    + "  raycaster.setFromCamera(mouse, camera);\n"
                    + "  const intersects = raycaster.intersectObject(activeModel, true);\n"
                    + "  if (intersects.length > 0) {\n"
                    + "    if (isCreatingTag) {\n"
                    + "      pendingTargetMesh  = intersects[0].object;\n"
                    + "      pendingTargetPoint = intersects[0].point;\n"
                    + "      document.getElementById('tagTargetDisplay').innerText = pendingTargetMesh.name || 'Unnamed Mesh';\n"
                    + "      setStatus('Selected: ' + (pendingTargetMesh.name || 'Unnamed') + '. Adjust camera then Save.');\n"
                    + "      return;\n"
                    + "    }\n"
                    + "    setStatus('Clicked: ' + (intersects[0].object.name || 'Unnamed Mesh'));\n"
                    + "  }\n"
                    + "}\n"
                    + "\n"
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
                    + "\n"
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
                    + "      setStatus(model.label + ' loaded ✔');\n"
                    + "    },\n"
                    + "    function(xhr) {\n"
                    + "      const bar = document.getElementById('loaderProgressBar');\n"
                    + "      const pct = document.getElementById('loaderPercent');\n"
                    + "      if (xhr.lengthComputable) {\n"
                    + "        const p = Math.round((xhr.loaded / xhr.total) * 100);\n"
                    + "        if (bar) { bar.style.width = p + '%'; bar.style.animation = 'none'; }\n"
                    + "        if (pct) pct.textContent = p + '%';\n"
                    + "      } else {\n"
                    + "        if (pct) pct.textContent = (xhr.loaded / 1048576).toFixed(2) + ' MB...';\n"
                    + "        if (bar) { bar.style.width = '50%'; bar.style.animation = 'indeterminate 1.5s infinite ease-in-out'; }\n"
                    + "      }\n"
                    + "    },\n"
                    + "    function(error) { console.error(error); setStatus('Model load failed ❌'); }\n"
                    + "  );\n"
                    + "}\n"
                    + "\n"
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
                    + "      if (bar) { bar.style.width = p + '%'; bar.style.animation = 'none'; }\n"
                    + "      if (pct) pct.textContent = p + '%';\n"
                    + "    }\n"
                    + "  };\n"
                    + "  reader.onload = function() {\n"
                    + "    loader.parse(reader.result, '', function(gltf) {\n"
                    + "      activeModel = gltf.scene;\n"
                    + "      prepareModelMaterials(activeModel);\n"
                    + "      modelContainer.add(activeModel);\n"
                    + "      frameModel(activeModel);\n"
                    + "      updateCameraNavVisibility(file.name);\n"
                    + "      setStatus(file.name + ' rendered ✔');\n"
                    + "    }, function(err) { console.error(err); setStatus('Could not parse model.'); });\n"
                    + "  };\n"
                    + "  reader.onerror = function() { setStatus('File could not be read.'); };\n"
                    + "  reader.readAsArrayBuffer(file);\n"
                    + "}\n"
                    + "\n"
                    + "window.addEventListener('resize', function() {\n"
                    + "  if (!renderer) return;\n"
                    + "  camera.aspect = viewer.clientWidth / viewer.clientHeight;\n"
                    + "  camera.updateProjectionMatrix();\n"
                    + "  renderer.setSize(viewer.clientWidth, viewer.clientHeight);\n"
                    + "});\n"
                    + "\n"
                    + "function animate() {\n"
                    + "  requestAnimationFrame(animate);\n"
                    + "  controls.update();\n"
                    + "  renderer.render(scene, camera);\n"
                    + "}\n"
                    + "\n"
                    + "// ─────────────────────────────────────────────\n"
                    + "// STARTUP — single entry point\n"
                    + "// ─────────────────────────────────────────────\n"
                    + "async function start() {\n"
                    + "  setStatus('Loading 3D library...');\n"
                    + "  THREE = loadThreeScripts();\n"
                    + "  initViewer();\n"
                    + "  animate();\n"
                    + "  setStatus('Viewer ready.');\n"
                    + "  await loadFileSystem();\n"
                    + "  // Auto-load first file if present\n"
                    + "  if (virtualFS.length > 0 && virtualFS[0].files.length > 0) {\n"
                    + "    const firstFile = virtualFS[0].files[0];\n"
                    + "    activeFileId = firstFile.id;\n"
                    + "    renderVirtualFS();\n"
                    + "    loadModel({ label: firstFile.name, fileNames: firstFile.fileNames, folderName: firstFile.fullPath });\n"
                    + "  }\n"
                    + "}\n"
                    + "\n"
                    + "start().catch(function(err) {\n"
                    + "  console.error(err);\n"
                    + "  setStatus('Startup error: ' + err.message);\n"
                    + "});\n"
                    + "</script>\n"
                    + "</body>\n"
                    + "</html>";

            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");

            byte[] bytes = html.getBytes("UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
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
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    @FunctionalInterface
    private interface RouteAction {
        JSONObject execute() throws Exception;
    }
}