package com.mayvel.objectViewer.route;

import com.mayvel.objectViewer.controller.ConfigurationController;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.tridium.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ObjectViewerRoute {

    public static void registerRoutes(HttpServer server) {
        server.createContext("/objectviewer", new ObjectViewerHandler());
        ModelFileRoute.register(server);

    }

    static class  ObjectViewerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {

            String html =
                    "<!DOCTYPE html>\n" +
                            "<html lang=\"en\">\n" +
                            "\n" +
                            "<head>\n" +
                            "    <meta charset=\"UTF-8\">\n" +
                            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                            "    <title>3D Model Viewer</title>\n" +
                            "    <style>\n" +
                            "        :root {\n" +
                            "            color-scheme: light;\n" +
                            "            --bg: #eef3f7;\n" +
                            "            --panel: rgba(255, 255, 255, 0.9);\n" +
                            "            --panel-border: rgba(20, 42, 61, 0.14);\n" +
                            "            --text: #142a3d;\n" +
                            "            --muted: #627384;\n" +
                            "            --accent: #0c7c59;\n" +
                            "            --accent-dark: #095b42;\n" +
                            "            --canvas: #d9e4ec;\n" +
                            "            --shadow: 0 18px 45px rgba(22, 39, 56, 0.14);\n" +
                            "        }\n" +
                            "\n" +
                            "        \n" +
                            "        * {\n" +
                            "            box-sizing: border-box;\n" +
                            "        }\n" +
                            "\n" +
                            "        body,\n" +
                            "        html {\n" +
                            "            margin: 0;\n" +
                            "            padding: 0;\n" +
                            "            height: 100%;\n" +
                            "            font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif;\n" +
                            "            background-color: var(--bg-color);\n" +
                            "            color: var(--text);\n" +
                            "            overflow: hidden;\n" +
                            "        }\n" +
                            "\n" +
                            "        .app-container {\n" +
                            "            display: flex;\n" +
                            "            height: 100vh;\n" +
                            "            background: radial-gradient(circle at center, #ffffff 0%, var(--bg-color) 100%);\n" +
                            "        }\n" +
                            "\n" +
                            "        .sidebar {\n" +
                            "            width: 320px;\n" +
                            "            background: var(--panel-bg);\n" +
                            "            backdrop-filter: blur(20px);\n" +
                            "            -webkit-backdrop-filter: blur(20px);\n" +
                            "            border-right: 1px solid var(--panel-border);\n" +
                            "            display: flex;\n" +
                            "            flex-direction: column;\n" +
                            "            box-shadow: 2px 0 20px rgba(0,0,0,0.03);\n" +
                            "            z-index: 10;\n" +
                            "        }\n" +
                            "\n" +
                            "        .sidebar-header {\n" +
                            "            padding: 24px;\n" +
                            "            border-bottom: 1px solid rgba(0,0,0,0.05);\n" +
                            "            display: flex;\n" +
                            "            justify-content: space-between;\n" +
                            "            align-items: center;\n" +
                            "        }\n" +
                            "\n" +
                            "        .sidebar-header h2 {\n" +
                            "            margin: 0;\n" +
                            "            font-size: 20px;\n" +
                            "            font-weight: 700;\n" +
                            "            letter-spacing: -0.5px;\n" +
                            "            background: linear-gradient(135deg, var(--text) 0%, #4a5568 100%);\n" +
                            "            -webkit-background-clip: text;\n" +
                            "            -webkit-text-fill-color: transparent;\n" +
                            "        }\n" +
                            "\n" +
                            "        .file-browser {\n" +
                            "            flex: 1;\n" +
                            "            overflow-y: auto;\n" +
                            "            padding: 16px;\n" +
                            "        }\n" +
                            "\n" +
                            "        .folder-item { margin-bottom: 12px; }\n" +
                            "        \n" +
                            "        .folder-header {\n" +
                            "            display: flex;\n" +
                            "            align-items: center;\n" +
                            "            padding: 10px 14px;\n" +
                            "            cursor: pointer;\n" +
                            "            border-radius: var(--radius-sm);\n" +
                            "            transition: all 0.2s ease;\n" +
                            "            font-weight: 600;\n" +
                            "            color: var(--text);\n" +
                            "        }\n" +
                            "        \n" +
                            "        .folder-header:hover {\n" +
                            "            background: rgba(0,0,0,0.03);\n" +
                            "        }\n" +
                            "\n" +
                            "        .file-list {\n" +
                            "            margin: 4px 0 8px 16px;\n" +
                            "            border-left: 2px solid rgba(0,0,0,0.05);\n" +
                            "            padding-left: 8px;\n" +
                            "        }\n" +
                            "\n" +
                            "        .file-item {\n" +
                            "            display: flex;\n" +
                            "            align-items: center;\n" +
                            "            padding: 8px 12px;\n" +
                            "            cursor: pointer;\n" +
                            "            border-radius: var(--radius-sm);\n" +
                            "            transition: all 0.2s;\n" +
                            "            font-size: 14px;\n" +
                            "            color: var(--text-light);\n" +
                            "            margin-bottom: 4px;\n" +
                            "        }\n" +
                            "\n" +
                            "        .file-item:hover {\n" +
                            "            background: rgba(37, 99, 235, 0.05);\n" +
                            "            color: var(--accent);\n" +
                            "            transform: translateX(4px);\n" +
                            "        }\n" +
                            "\n" +
                            "        .file-item.active {\n" +
                            "            background: rgba(37, 99, 235, 0.1);\n" +
                            "            color: var(--accent);\n" +
                            "            font-weight: 600;\n" +
                            "        }\n" +
                            "\n" +
                            "        .upload-btn {\n" +
                            "            width: calc(100% - 24px);\n" +
                            "            margin: 8px 12px 16px 12px;\n" +
                            "            padding: 10px;\n" +
                            "            border: 1px dashed var(--accent);\n" +
                            "            border-radius: var(--radius-sm);\n" +
                            "            background: rgba(37, 99, 235, 0.03);\n" +
                            "            color: var(--accent);\n" +
                            "            cursor: pointer;\n" +
                            "            display: flex;\n" +
                            "            align-items: center;\n" +
                            "            justify-content: center;\n" +
                            "            gap: 8px;\n" +
                            "            font-size: 13px;\n" +
                            "            font-weight: 600;\n" +
                            "            transition: all 0.2s;\n" +
                            "        }\n" +
                            "\n" +
                            "        .upload-btn:hover {\n" +
                            "            background: rgba(37, 99, 235, 0.1);\n" +
                            "            transform: translateY(-1px);\n" +
                            "        }\n" +
                            "\n" +
                            "        .viewer-shell {\n" +
                            "            flex: 1;\n" +
                            "            position: relative;\n" +
                            "            border-radius: var(--radius);\n" +
                            "            overflow: hidden;\n" +
                            "            box-shadow: var(--shadow);\n" +
                            "            margin: 16px;\n" +
                            "            background: #ffffff;\n" +
                            "        }\n" +
                            "\n" +
                            "        #viewer { width: 100%; height: 100%; display: block; }\n" +
                            "\n" +
                            "        .camera-nav {\n" +
                            "            position: absolute;\n" +
                            "            top: 24px;\n" +
                            "            left: 50%;\n" +
                            "            transform: translateX(-50%);\n" +
                            "            display: flex;\n" +
                            "            flex-direction: column;\n" +
                            "            align-items: center;\n" +
                            "            gap: 12px;\n" +
                            "            z-index: 10;\n" +
                            "            background: rgba(240, 244, 248, 0.45);\n" +
                            "            backdrop-filter: blur(24px);\n" +
                            "            -webkit-backdrop-filter: blur(24px);\n" +
                            "            padding: 14px 18px;\n" +
                            "            border-radius: 24px;\n" +
                            "            box-shadow: 0 12px 40px rgba(0, 0, 0, 0.08), inset 0 1px 2px rgba(255, 255, 255, 0.8);\n" +
                            "            border: 1px solid rgba(255, 255, 255, 0.5);\n" +
                            "            max-width: 85%;\n" +
                            "        }\n" +
                            "\n" +
                            "        .cam-btn {\n" +
                            "            background: rgba(255, 255, 255, 0.95);\n" +
                            "            border: 1px solid rgba(0, 0, 0, 0.04);\n" +
                            "            color: var(--text);\n" +
                            "            padding: 10px 18px;\n" +
                            "            border-radius: 24px;\n" +
                            "            font-size: 11px;\n" +
                            "            font-weight: 700;\n" +
                            "            cursor: pointer;\n" +
                            "            display: flex;\n" +
                            "            align-items: center;\n" +
                            "            gap: 6px;\n" +
                            "            transition: all 0.3s cubic-bezier(0.2, 0.8, 0.2, 1);\n" +
                            "            text-transform: uppercase;\n" +
                            "            letter-spacing: 0.5px;\n" +
                            "            white-space: nowrap;\n" +
                            "            flex-shrink: 0;\n" +
                            "            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);\n" +
                            "        }\n" +
                            "\n" +
                            "        .cam-btn:hover {\n" +
                            "            background: var(--accent);\n" +
                            "            color: white;\n" +
                            "            transform: translateY(-2px) scale(1.02);\n" +
                            "            box-shadow: 0 8px 20px rgba(37, 99, 235, 0.25);\n" +
                            "            border-color: var(--accent);\n" +
                            "        }\n" +
                            "        \n" +
                            "        .viewer-badge {\n" +
                            "            position: absolute;\n" +
                            "            top: auto;\n" +
                            "            bottom: 24px;\n" +
                            "            right: 24px;\n" +
                            "            background: rgba(255, 255, 255, 0.8);\n" +
                            "            backdrop-filter: blur(10px);\n" +
                            "            -webkit-backdrop-filter: blur(10px);\n" +
                            "            padding: 8px 16px;\n" +
                            "            border-radius: 20px;\n" +
                            "            font-size: 13px;\n" +
                            "            color: var(--text-light);\n" +
                            "            box-shadow: var(--shadow-sm);\n" +
                            "            z-index: 10;\n" +
                            "        }\n" +
                            "\n" +
                            "        .status-container {\n" +
                            "            padding: 16px 24px;\n" +
                            "            border-top: 1px solid rgba(0,0,0,0.05);\n" +
                            "            margin-top: auto;\n" +
                            "            background: rgba(255,255,255,0.5);\n" +
                            "        }\n" +
                            "\n" +
                            "        .progress-bar {\n" +
                            "            height: 6px;\n" +
                            "            width: 0%;\n" +
                            "            background: var(--accent);\n" +
                            "            border-radius: 3px;\n" +
                            "            transition: width 0.2s ease-out;\n" +
                            "        }\n" +
                            "\n" +
                            "        .status { font-size: 12px; color: var(--text-light); margin: 8px 0 0 0; }\n" +
                            "\n" +
                            "        @keyframes spin {\n" +
                            "            to { transform: rotate(360deg); }\n" +
                            "        }\n" +
                            "        @keyframes indeterminate {\n" +
                            "            0% { transform: translateX(-200%); }\n" +
                            "            100% { transform: translateX(200%); }\n" +
                            "        }\n" +
                            "    </style>\n" +
                            "</head>\n" +
                            "\n" +
                            "<body>\n" +
                            "    <div id=\"loaderOverlay\" style=\"display: none; position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: rgba(255,255,255,0.7); backdrop-filter: blur(4px); -webkit-backdrop-filter: blur(4px); z-index: 9999; flex-direction: column; align-items: center; justify-content: center;\">\n" +
                            "        <div style=\"width: 40px; height: 40px; border: 4px solid rgba(37, 99, 235, 0.2); border-top-color: var(--accent); border-radius: 50%; animation: spin 1s linear infinite; margin-bottom: 16px;\"></div>\n" +
                            "        <div id=\"loaderText\" style=\"font-size: 16px; font-weight: 600; color: var(--text); margin-bottom: 12px;\">Loading...</div>\n" +
                            "        <div style=\"width: 240px; height: 6px; background: rgba(0,0,0,0.1); border-radius: 3px; overflow: hidden; margin-bottom: 8px; position: relative;\">\n" +
                            "            <div id=\"loaderProgressBar\" style=\"width: 0%; height: 100%; background: var(--accent); transition: width 0.2s; position: absolute; left: 0;\"></div>\n" +
                            "        </div>\n" +
                            "        <div id=\"loaderPercent\" style=\"font-size: 13px; color: var(--muted); font-weight: 500;\"></div>\n" +
                            "    </div>\n" +
                            "    <main class=\"app-container\">\n" +
                            "        <section class=\"sidebar\">\n" +
                            "            <div class=\"sidebar-header\">\n" +
                            "                <h2>Models</h2>\n" +
                            "                <button id=\"newFolderBtn\" class=\"cam-btn\" title=\"New Folder\" style=\"padding: 4px 10px; font-size: 16px; border: 1px solid var(--panel-border);\">+</button>\n" +
                            "            </div>\n" +
                            "\n" +
                            "            <div id=\"fileBrowser\" class=\"file-browser\">\n" +
                            "                <!-- virtual fs rendered here -->\n" +
                            "            </div>\n" +
                            "\n" +
                            "            <input id=\"fileInput\" type=\"file\" accept=\".glb,model/gltf-binary\" style=\"display: none;\">\n" +
                            "\n" +
                            "            <div class=\"status-container\">\n" +
                            "                <div id=\"uploadProgressContainer\" style=\"display: none; height: 6px; background: rgba(0,0,0,0.05); border-radius: 3px; margin-bottom: 8px;\">\n" +
                            "                    <div class=\"progress-bar\" id=\"progressBar\"></div>\n" +
                            "                </div>\n" +
                            "                <p id=\"status\" class=\"status\">Viewer ready.</p>\n" +
                            "            </div>\n" +
                            "        </section>\n" +
                            "\n" +
                            "        <section class=\"viewer-shell\">\n" +
                            "            <div class=\"viewer-badge\">Orbit: drag to rotate, scroll to zoom</div>\n" +
                            "            <div id=\"cameraNav\" class=\"camera-nav\" style=\"display: none;\">\n" +
                            "                <!-- Buttons will be injected here dynamically -->\n" +
                            "            </div>\n" +
                            "            <div id=\"tagCreatorUI\"\n" +
                            "                style=\"display: none; position: absolute; top: 80px; right: 24px; background: rgba(255,255,255,0.95); padding: 16px; border-radius: 8px; box-shadow: 0 18px 45px rgba(22, 39, 56, 0.14); z-index: 1000; min-width: 250px; font-size: 14px; border: 1px solid rgba(20, 42, 61, 0.14);\">\n" +
                            "                <h4 style=\"margin: 0 0 12px 0; color: var(--text);\">Create New Tag</h4>\n" +
                            "                <div style=\"margin-bottom: 12px;\">\n" +
                            "                    <label style=\"display: block; margin-bottom: 4px; color: var(--muted); font-weight: 500;\">Tag\n" +
                            "                        Name</label>\n" +
                            "                    <input type=\"text\" id=\"tagNameInput\" placeholder=\"e.g. Filter Check\"\n" +
                            "                        style=\"width: 100%; padding: 8px; border: 1px solid #ccc; border-radius: 4px; box-sizing: border-box;\">\n" +
                            "                </div>\n" +
                            "                <div style=\"margin-bottom: 16px;\">\n" +
                            "                    <label style=\"display: block; margin-bottom: 4px; color: var(--muted); font-weight: 500;\">Focus\n" +
                            "                        Part</label>\n" +
                            "                    <div id=\"tagTargetDisplay\"\n" +
                            "                        style=\"width: 100%; padding: 8px; background: #f0f4f8; border: 1px solid #d9e4ec; border-radius: 4px; box-sizing: border-box; color: var(--text); min-height: 35px; word-break: break-all;\">\n" +
                            "                        Click a part on the 3D model...</div>\n" +
                            "                </div>\n" +
                            "                <div style=\"display: flex; gap: 8px; justify-content: flex-end;\">\n" +
                            "                    <button onclick=\"cancelCreateTag()\"\n" +
                            "                        style=\"padding: 6px 12px; border: 1px solid #ccc; background: white; border-radius: 4px; cursor: pointer;\">Cancel</button>\n" +
                            "                    <button onclick=\"saveNewTag()\"\n" +
                            "                        style=\"padding: 6px 12px; border: none; background: var(--accent); color: white; border-radius: 4px; cursor: pointer; font-weight: 500;\">Save\n" +
                            "                        Tag</button>\n" +
                            "                </div>\n" +
                            "            </div>\n" +
                            "            <div class=\"viewer-badge\" style=\"top: auto; bottom: 18px;\">Orbit: drag to rotate, scroll to zoom</div>\n" +
                            "            <div id=\"viewer\"></div>\n" +
                            "        </section>\n" +
                            "    </main>\n" +
                            "    <script src=\"https://cdn.jsdelivr.net/npm/three@0.128.0/build/three.min.js\"></script>\n" +
                            "    <script src=\"https://cdn.jsdelivr.net/npm/three@0.128.0/examples/js/controls/OrbitControls.js\"></script>\n" +
                            "    <script src=\"https://cdn.jsdelivr.net/npm/three@0.128.0/examples/js/loaders/GLTFLoader.js\"></script>\n" +
                            "    <script src=\"https://cdn.jsdelivr.net/npm/gsap@3.12.2/dist/gsap.min.js\"></script>\n" +
                            "    <script>\n" +
                            "        let virtualFS = [\n" +
                            "            {\n" +
                            "                id: 'folder-1',\n" +
                            "                name: '3d_models',\n" +
                            "                expanded: true,\n" +
                            "                files: [\n" +
                            "                    { id: 'file-1', name: 'Damper', type: 'server', fileNames: [\"damper.glb\", \"damper.glb\"], folderName: '3d_models' },\n" +
                            "                    { id: 'file-2', name: 'Filter', type: 'server', fileNames: [\"filter.glb\", \"filter.glb\"], folderName: '3d_models' },\n" +
                            "                    { id: 'file-3', name: 'Server Rack', type: 'server', fileNames: [\"ServerRack.glb\", \"ServerRack.glb\"], folderName: '3d_models' },\n" +
                            "                    { id: 'file-4', name: 'AHU5', type: 'server', fileNames: [\"AHU5.glb\", \"AHU5.glb\"], folderName: '3d_models' }\n" +
                            "                ]\n" +
                            "            }\n" +
                            "        ];\n" +
                            "\n" +
                            "        let activeFolderId = null;\n" +
                            "        let activeFileId = null;\n" +
                            "\n" +
                            "        const fileInput = document.getElementById(\"fileInput\");\n" +
                            "        const status = document.getElementById(\"status\");\n" +
                            "        const viewer = document.getElementById(\"viewer\");\n" +
                            "\n" +
                            "        const uploadProgressContainer = document.getElementById(\"uploadProgressContainer\");\n" +
                            "        const progressText = document.getElementById(\"progressText\");\n" +
                            "        const progressPercent = document.getElementById(\"progressPercent\");\n" +
                            "        const progressBar = document.getElementById(\"progressBar\");\n" +
                            "        const cameraNav = document.getElementById(\"cameraNav\");\n" +
                            "\n" +
                            "        function updateCameraNavVisibility(modelName) {\n" +
                            "            if (cameraNav) {\n" +
                            "                if (modelName && modelName.toLowerCase().includes('ahu5')) {\n" +
                            "                    cameraNav.style.display = 'flex';\n" +
                            "                    renderCameraNav();\n" +
                            "                } else {\n" +
                            "                    cameraNav.style.display = 'none';\n" +
                            "                }\n" +
                            "            }\n" +
                            "        }\n" +
                            "\n" +
                            "        function renderVirtualFS() {\n" +
                            "            const browser = document.getElementById(\"fileBrowser\");\n" +
                            "            browser.innerHTML = \"\";\n" +
                            "\n" +
                            "            virtualFS.forEach(folder => {\n" +
                            "                const folderEl = document.createElement(\"div\");\n" +
                            "                folderEl.className = \"folder-item\";\n" +
                            "\n" +
                            "                const folderHeader = document.createElement(\"div\");\n" +
                            "                folderHeader.className = \"folder-header\";\n" +
                            "                folderHeader.innerHTML = folder.name;\n" +
                            "                folderHeader.onclick = () => {\n" +
                            "                    folder.expanded = !folder.expanded;\n" +
                            "                    renderVirtualFS();\n" +
                            "                };\n" +
                            "\n" +
                            "                folderEl.appendChild(folderHeader);\n" +
                            "\n" +
                            "                if (folder.expanded) {\n" +
                            "                    const folderContent = document.createElement(\"div\");\n" +
                            "                    folderContent.className = \"folder-content\";\n" +
                            "\n" +
                            "                    const uploadBtn = document.createElement(\"button\");\n" +
                            "                    uploadBtn.className = \"upload-btn\";\n" +
                            "                    uploadBtn.innerHTML = `Upload .glb`;\n" +
                            "                    uploadBtn.onclick = (e) => {\n" +
                            "                        e.stopPropagation();\n" +
                            "                        activeFolderId = folder.id;\n" +
                            "                        fileInput.click();\n" +
                            "                    };\n" +
                            "                    folderContent.appendChild(uploadBtn);\n" +
                            "\n" +
                            "                    const fileList = document.createElement(\"div\");\n" +
                            "                    fileList.className = \"file-list\";\n" +
                            "\n" +
                            "                    folder.files.forEach(file => {\n" +
                            "                        const fileEl = document.createElement(\"div\");\n" +
                            "                        fileEl.className = \"file-item\" + (activeFileId === file.id ? \" active\" : \"\");\n" +
                            "                        fileEl.innerHTML = file.name;\n" +
                            "                        fileEl.onclick = (e) => {\n" +
                            "                            e.stopPropagation();\n" +
                            "                            activeFileId = file.id;\n" +
                            "                            renderVirtualFS(); // Update active state\n" +
                            "                            if (file.type === 'server') {\n" +
                            "                                loadModel({ label: file.name, fileNames: file.fileNames, folderName: folder.name });\n" +
                            "                            } else if (file.type === 'local') {\n" +
                            "                                loadModelFile(file.file);\n" +
                            "                            }\n" +
                            "                        };\n" +
                            "                        fileList.appendChild(fileEl);\n" +
                            "                    });\n" +
                            "\n" +
                            "                    folderContent.appendChild(fileList);\n" +
                            "                    folderEl.appendChild(folderContent);\n" +
                            "                }\n" +
                            "\n" +
                            "                browser.appendChild(folderEl);\n" +
                            "            });\n" +
                            "        }\n" +
                            "\n" +
                            "        document.getElementById(\"newFolderBtn\").addEventListener(\"click\", () => {\n" +
                            "            const name = prompt(\"Enter folder name:\");\n" +
                            "            if (name && name.trim() !== \"\") {\n" +
                            "                virtualFS.push({\n" +
                            "                    id: 'folder-' + Date.now(),\n" +
                            "                    name: name.trim(),\n" +
                            "                    expanded: true,\n" +
                            "                    files: []\n" +
                            "                });\n" +
                            "                renderVirtualFS();\n" +
                            "            }\n" +
                            "        });\n" +
                            "\n" +
                            "        fileInput.addEventListener(\"change\", () => {\n" +
                            "            const file = fileInput.files[0];\n" +
                            "            if (file && activeFolderId) {\n" +
                            "                const folder = virtualFS.find(f => f.id === activeFolderId);\n" +
                            "                if (folder) {\n" +
                            "                    setStatus(\"Preparing upload...\");\n" +
                            "\n" +
                            "                    const slash = String.fromCharCode(47);\n" +
                            "                    const fileRoot = \"/getModel?name=\";" +
                            "                    const folderPath = folder.name;\n" +
                            "                    const url = fileRoot + encodeURIComponent(file.name);\n" +
                            "\n" +
                            "                    uploadProgressContainer.style.display = \"block\";\n" +
                            "                    progressBar.style.width = \"0%\";\n" +
                            "\n" +
                            "                    const xhr = new XMLHttpRequest();\n" +
                            "                    xhr.open(\"PUT\", url, true);\n" +
                            "\n" +
                            "                    const csrfMatch = document.cookie.match(new RegExp('(^| )niagara_csrf=([^;]+)'));\n" +
                            "                    if (csrfMatch) {\n" +
                            "                        xhr.setRequestHeader('X-Niagara-Csrf-Token', csrfMatch[2]);\n" +
                            "                    }\n" +
                            "                    xhr.setRequestHeader('Content-Type', file.type || 'application/octet-stream');\n" +
                            "\n" +
                            "                    xhr.upload.onprogress = (e) => {\n" +
                            "                        if (e.lengthComputable) {\n" +
                            "                            const percentComplete = Math.round((e.loaded / e.total) * 100);\n" +
                            "                            progressBar.style.width = percentComplete + \"%\";\n" +
                            "                        }\n" +
                            "                    };\n" +
                            "\n" +
                            "                    xhr.onload = () => {\n" +
                            "                        setTimeout(() => { uploadProgressContainer.style.display = \"none\"; }, 1500);\n" +
                            "\n" +
                            "                        if (xhr.status >= 200 && xhr.status < 300) {\n" +
                            "                            setStatus(\"Successfully uploaded to \" + folderPath);\n" +
                            "                        } else {\n" +
                            "                            setStatus(\"Upload failed (Status \" + xhr.status + \"). Model rendering locally.\");\n" +
                            "                            console.warn(\"Niagara upload failed:\", xhr.responseText);\n" +
                            "                            alert(\"Niagara upload failed: HTTP \" + xhr.status + \"\\n\" + xhr.statusText + \"\\n\\nNiagara might require specific CSRF tokens, baja.js integration, or a different upload endpoint.\");\n" +
                            "                        }\n" +
                            "\n" +
                            "                        const newFile = {\n" +
                            "                            id: 'file-' + Date.now(),\n" +
                            "                            name: file.name,\n" +
                            "                            type: 'server',\n" +
                            "                            fileNames: [file.name],\n" +
                            "                            folderName: folder.name\n" +
                            "                        };\n" +
                            "                        folder.files.push(newFile);\n" +
                            "                        activeFileId = newFile.id;\n" +
                            "                        renderVirtualFS();\n" +
                            "                        loadModelFile(file);\n" +
                            "                    };\n" +
                            "\n" +
                            "                    xhr.onerror = () => {\n" +
                            "                        uploadProgressContainer.style.display = \"none\";\n" +
                            "                        console.error(\"Upload XHR error\");\n" +
                            "                        setStatus(\"Upload failed. Check Niagara permissions.\");\n" +
                            "\n" +
                            "                        const newFile = {\n" +
                            "                            id: 'file-' + Date.now(),\n" +
                            "                            name: file.name,\n" +
                            "                            type: 'local',\n" +
                            "                            file: file\n" +
                            "                        };\n" +
                            "                        folder.files.push(newFile);\n" +
                            "                        activeFileId = newFile.id;\n" +
                            "                        renderVirtualFS();\n" +
                            "                        loadModelFile(file);\n" +
                            "                    };\n" +
                            "                    xhr.send(file);\n" +
                            "                }\n" +
                            "            }\n" +
                            "            fileInput.value = \"\"; // Reset for next upload\n" +
                            "        });\n" +
                            "\n" +
                            "        function setStatus(message) {\n" +
                            "            status.textContent = message;\n" +
                            "            \n" +
                            "            const overlay = document.getElementById(\"loaderOverlay\");\n" +
                            "            const loaderText = document.getElementById(\"loaderText\");\n" +
                            "            const loaderProgressBar = document.getElementById(\"loaderProgressBar\");\n" +
                            "            const loaderPercent = document.getElementById(\"loaderPercent\");\n" +
                            "            \n" +
                            "            if (overlay && loaderText) {\n" +
                            "                if (message.toLowerCase().includes(\"loading\")) {\n" +
                            "                    overlay.style.display = \"flex\";\n" +
                            "                    loaderText.textContent = message;\n" +
                            "                    if (loaderProgressBar) {\n" +
                            "                        loaderProgressBar.style.width = \"0%\";\n" +
                            "                        loaderProgressBar.style.animation = \"none\";\n" +
                            "                    }\n" +
                            "                    if (loaderPercent) loaderPercent.textContent = \"Connecting...\";\n" +
                            "                } else {\n" +
                            "                    overlay.style.display = \"none\";\n" +
                            "                }\n" +
                            "            }\n" +
                            "        }\n" +
                            "\n" +
                            "        function getModelUrls(model) {\n" +
                            "        const fileRoot = \"/getModel?name=\";\n" +
                            "return model.fileNames.map((fileName) => \n" +
                            "    fileRoot + encodeURIComponent(fileName)\n" +
                            ");}\n" +
                            "\n" +
                            "        async function loadThreeScripts() {\n" +
                            "            if (!window.THREE || !window.THREE.OrbitControls || !window.THREE.GLTFLoader) {\n" +
                            "                throw new Error(\"Three.js library is not available from cdn.jsdelivr.net.\");\n" +
                            "            }\n" +
                            "\n" +
                            "            return window.THREE;\n" +
                            "        }\n" +
                            "\n" +
                            "        let THREE;\n" +
                            "        let scene;\n" +
                            "        let camera;\n" +
                            "        let renderer;\n" +
                            "        let controls;\n" +
                            "        let loader;\n" +
                            "        let activeModel = null;\n" +
                            "        let modelContainer = null;\n" +
                            "        let raycaster;\n" +
                            "        let mouse;\n" +
                            "\n" +
                            "        let isCreatingTag = false;\n" +
                            "        let pendingTagName = \"\";\n" +
                            "\n" +
                            "        let allModelViews = JSON.parse(localStorage.getItem('niagara_3d_all_views')) || {\n" +
                            "            \"AHU5\": {\n" +
                            "                damper: { name: \"DAMPER\", pos: { x: -2.1, y: 0.8, z: 2.8 }, target: { x: -2.1, y: 0.5, z: 0 } },\n" +
                            "                filter: { name: \"FILTER\", pos: { x: -0.4, y: 1.0, z: 3.5 }, target: { x: -0.4, y: 0.5, z: 0 } },\n" +
                            "                cooling: { name: \"COOLING COIL\", pos: { x: 1.6, y: 1.0, z: 3.5 }, target: { x: 1.6, y: 0.5, z: 0 } },\n" +
                            "                heating: { name: \"HEATING COIL\", pos: { x: 2.4, y: 1.0, z: 3.5 }, target: { x: 2.4, y: 0.5, z: 0 } },\n" +
                            "                fan: { name: \"FAN\", pos: { x: 3.3, y: 1.0, z: 3.5 }, target: { x: 3.3, y: 0.5, z: 0 } }\n" +
                            "            }\n" +
                            "        };\n" +
                            "\n" +
                            "        let legacyViews = JSON.parse(localStorage.getItem('niagara_3d_views'));\n" +
                            "        if (legacyViews && !localStorage.getItem('niagara_3d_all_views_migrated')) {\n" +
                            "            allModelViews[\"AHU5\"] = legacyViews;\n" +
                            "            localStorage.setItem('niagara_3d_all_views_migrated', \"true\");\n" +
                            "            localStorage.setItem('niagara_3d_all_views', JSON.stringify(allModelViews));\n" +
                            "        }\n" +
                            "\n" +
                            "        let currentModelName = \"AHU5\";\n" +
                            "        let views = allModelViews[currentModelName] || {};\n" +
                            "\n" +
                            "        function updateCameraNavVisibility(modelName) {\n" +
                            "            currentModelName = modelName.replace('.glb', '').trim();\n" +
                            "            if (!allModelViews[currentModelName]) {\n" +
                            "                allModelViews[currentModelName] = {};\n" +
                            "            }\n" +
                            "            views = allModelViews[currentModelName];\n" +
                            "            \n" +
                            "            if (cameraNav) {\n" +
                            "                cameraNav.style.display = 'flex';\n" +
                            "                renderCameraNav();\n" +
                            "            }\n" +
                            "        }\n" +
                            "\n" +
                            "        function renderCameraNav() {\n" +
                            "            const nav = document.getElementById(\"cameraNav\");\n" +
                            "            if (!nav) return;\n" +
                            "            nav.innerHTML = \"\";\n" +
                            "\n" +
                            "            const tagsWrapper = document.createElement(\"div\");\n" +
                            "            tagsWrapper.style.display = \"flex\";\n" +
                            "            tagsWrapper.style.flexWrap = \"wrap\";\n" +
                            "            tagsWrapper.style.justifyContent = \"center\";\n" +
                            "            tagsWrapper.style.gap = \"10px\";\n" +
                            "            tagsWrapper.style.maxHeight = \"120px\";\n" +
                            "            tagsWrapper.style.overflowY = \"auto\";\n" +
                            "            tagsWrapper.style.width = \"100%\";\n" +
                            "            tagsWrapper.style.paddingRight = \"4px\";\n" +
                            "\n" +
                            "            for (const [id, view] of Object.entries(views)) {\n" +
                            "                const btnContainer = document.createElement(\"div\");\n" +
                            "                btnContainer.className = \"cam-btn\";\n" +
                            "                btnContainer.style.padding = \"6px 8px 6px 16px\";\n" +
                            "                btnContainer.onclick = () => flyToView(id);\n" +
                            "\n" +
                            "                const textSpan = document.createElement(\"span\");\n" +
                            "                textSpan.innerHTML = `<span class=\"dot\"></span> ${view.name.toUpperCase()}`;\n" +
                            "                \n" +
                            "                const deleteBtn = document.createElement(\"button\");\n" +
                            "                deleteBtn.innerHTML = \"✕\";\n" +
                            "                deleteBtn.style.background = \"transparent\";\n" +
                            "                deleteBtn.style.border = \"none\";\n" +
                            "                deleteBtn.style.color = \"currentColor\";\n" +
                            "                deleteBtn.style.opacity = \"0.5\";\n" +
                            "                deleteBtn.style.cursor = \"pointer\";\n" +
                            "                deleteBtn.style.fontSize = \"10px\";\n" +
                            "                deleteBtn.style.marginLeft = \"4px\";\n" +
                            "                deleteBtn.style.padding = \"4px 6px\";\n" +
                            "                deleteBtn.style.borderRadius = \"50%\";\n" +
                            "                deleteBtn.style.transition = \"all 0.2s\";\n" +
                            "                deleteBtn.onmouseover = () => { deleteBtn.style.opacity = \"1\"; deleteBtn.style.background = \"rgba(0,0,0,0.1)\"; };\n" +
                            "                deleteBtn.onmouseout = () => { deleteBtn.style.opacity = \"0.5\"; deleteBtn.style.background = \"transparent\"; };\n" +
                            "                deleteBtn.onclick = (e) => {\n" +
                            "                    e.stopPropagation();\n" +
                            "                    if (confirm(`Delete tag \"${view.name}\"?`)) {\n" +
                            "                        delete views[id];\n" +
                            "                        allModelViews[currentModelName] = views;\n" +
                            "                        localStorage.setItem('niagara_3d_all_views', JSON.stringify(allModelViews));\n" +
                            "                        renderCameraNav();\n" +
                            "                    }\n" +
                            "                };\n" +
                            "\n" +
                            "                btnContainer.appendChild(textSpan);\n" +
                            "                btnContainer.appendChild(deleteBtn);\n" +
                            "                tagsWrapper.appendChild(btnContainer);\n" +
                            "            }\n" +
                            "            nav.appendChild(tagsWrapper);\n" +
                            "\n" +
                            "            const addBtn = document.createElement(\"button\");\n" +
                            "            addBtn.className = \"cam-btn\";\n" +
                            "            addBtn.style.background = \"rgba(12, 124, 89, 0.1)\";\n" +
                            "            addBtn.style.color = \"var(--accent)\";\n" +
                            "            addBtn.style.border = \"1px solid rgba(12, 124, 89, 0.3)\";\n" +
                            "            addBtn.onclick = startCreateTag;\n" +
                            "            addBtn.innerHTML = `+ ADD TAG`;\n" +
                            "            nav.appendChild(addBtn);\n" +
                            "        }\n" +
                            "\n" +
                            "        let pendingTargetMesh = null;\n" +
                            "        let pendingTargetPoint = null;\n" +
                            "\n" +
                            "        function startCreateTag() {\n" +
                            "            document.getElementById('tagCreatorUI').style.display = 'block';\n" +
                            "            document.getElementById('tagNameInput').value = '';\n" +
                            "            document.getElementById('tagTargetDisplay').innerText = 'Click a part on the 3D model...';\n" +
                            "            pendingTargetMesh = null;\n" +
                            "            pendingTargetPoint = null;\n" +
                            "            isCreatingTag = true;\n" +
                            "            setStatus(\"Tag Creation Mode: Adjust camera and click a part on the 3D model.\");\n" +
                            "        }\n" +
                            "\n" +
                            "        function cancelCreateTag() {\n" +
                            "            document.getElementById('tagCreatorUI').style.display = 'none';\n" +
                            "            isCreatingTag = false;\n" +
                            "            pendingTargetMesh = null;\n" +
                            "            pendingTargetPoint = null;\n" +
                            "            setStatus(\"Tag creation cancelled.\");\n" +
                            "        }\n" +
                            "\n" +
                            "        function saveNewTag() {\n" +
                            "            const tagName = document.getElementById('tagNameInput').value.trim();\n" +
                            "            if (!tagName) {\n" +
                            "                alert(\"Please enter a tag name.\");\n" +
                            "                return;\n" +
                            "            }\n" +
                            "            if (!pendingTargetMesh || !pendingTargetPoint) {\n" +
                            "                alert(\"Please click on a part of the 3D model to select a focus point.\");\n" +
                            "                return;\n" +
                            "            }\n" +
                            "\n" +
                            "            // Capture EXACT camera position and pivot target at the moment of saving\n" +
                            "            const posPoint = camera.position.clone();\n" +
                            "            const targetPoint = controls.target.clone();\n" +
                            "\n" +
                            "            const viewId = 'view_' + Date.now();\n" +
                            "            views[viewId] = {\n" +
                            "                name: tagName,\n" +
                            "                pos: { x: posPoint.x, y: posPoint.y, z: posPoint.z },\n" +
                            "                target: { x: targetPoint.x, y: targetPoint.y, z: targetPoint.z }\n" +
                            "            };\n" +
                            "\n" +
                            "            localStorage.setItem('niagara_3d_all_views', JSON.stringify(allModelViews));\n" +
                            "\n" +
                            "            document.getElementById('tagCreatorUI').style.display = 'none';\n" +
                            "            isCreatingTag = false;\n" +
                            "            pendingTargetMesh = null;\n" +
                            "            pendingTargetPoint = null;\n" +
                            "            setStatus(\"Tag '\" + tagName + \"' exact view saved!\");\n" +
                            "            renderCameraNav();\n" +
                            "        }\n" +
                            "\n" +
                            "        function flyToView(viewName) {\n" +
                            "            if (!camera || !controls) return;\n" +
                            "            const view = views[viewName];\n" +
                            "            if (!view) return;\n" +
                            "\n" +
                            "            if (!window.gsap) {\n" +
                            "                // Fallback if GSAP is blocked by CSP\n" +
                            "                camera.position.set(view.pos.x, view.pos.y, view.pos.z);\n" +
                            "                controls.target.set(view.target.x, view.target.y, view.target.z);\n" +
                            "                controls.update();\n" +
                            "                return;\n" +
                            "            }\n" +
                            "\n" +
                            "            gsap.to(camera.position, {\n" +
                            "                x: view.pos.x,\n" +
                            "                y: view.pos.y,\n" +
                            "                z: view.pos.z,\n" +
                            "                duration: 1.5,\n" +
                            "                ease: \"power2.inOut\"\n" +
                            "            });\n" +
                            "\n" +
                            "            gsap.to(controls.target, {\n" +
                            "                x: view.target.x,\n" +
                            "                y: view.target.y,\n" +
                            "                z: view.target.z,\n" +
                            "                duration: 1.5,\n" +
                            "                ease: \"power2.inOut\",\n" +
                            "                onUpdate: () => controls.update()\n" +
                            "            });\n" +
                            "        }\n" +
                            "\n" +
                            "        function disposeMaterial(material) {\n" +
                            "            for (const key of Object.keys(material)) {\n" +
                            "                const value = material[key];\n" +
                            "                if (value && typeof value === \"object\" && \"minFilter\" in value) {\n" +
                            "                    value.dispose();\n" +
                            "                }\n" +
                            "            }\n" +
                            "            material.dispose();\n" +
                            "        }\n" +
                            "\n" +
                            "        function clearCurrentModel() {\n" +
                            "            if (modelContainer) {\n" +
                            "                while(modelContainer.children.length > 0){ \n" +
                            "                    const child = modelContainer.children[0];\n" +
                            "                    modelContainer.remove(child); \n" +
                            "                    \n" +
                            "                    child.traverse((node) => {\n" +
                            "                      if (node.geometry) node.geometry.dispose();\n" +
                            "                      if (node.material) {\n" +
                            "                        if (Array.isArray(node.material)) {\n" +
                            "                          node.material.forEach(disposeMaterial);\n" +
                            "                        } else {\n" +
                            "                          disposeMaterial(node.material);\n" +
                            "                        }\n" +
                            "                      }\n" +
                            "                    });\n" +
                            "                }\n" +
                            "            }\n" +
                            "            activeModel = null;\n" +
                            "        }\n" +
                            "\n" +
                            "        function frameModel(object) {\n" +
                            "            const box = new THREE.Box3().setFromObject(object);\n" +
                            "            const size = box.getSize(new THREE.Vector3());\n" +
                            "            const center = box.getCenter(new THREE.Vector3());\n" +
                            "            const maxSize = Math.max(size.x, size.y, size.z);\n" +
                            "            const fitDistance = maxSize / (2 * Math.tan((Math.PI * camera.fov) / 360));\n" +
                            "            const distance = fitDistance * 1.65;\n" +
                            "\n" +
                            "            camera.near = Math.max(0.1, maxSize / 100);\n" +
                            "            camera.far = Math.max(1000, distance * 10);\n" +
                            "            camera.updateProjectionMatrix();\n" +
                            "\n" +
                            "            camera.position.set(center.x + distance * 0.8, center.y + distance * 0.55, center.z + distance);\n" +
                            "            controls.target.copy(center);\n" +
                            "            controls.update();\n" +
                            "        }\n" +
                            "\n" +
                            "        function initViewer() {\n" +
                            "            THREE.Cache.enabled = true;\n" +
                            "            \n" +
                            "            scene = new THREE.Scene();\n" +
                            "            scene.background = new THREE.Color(0xf0f4f8);\n" +
                            "            \n" +
                            "            modelContainer = new THREE.Group();\n" +
                            "            scene.add(modelContainer);\n" +
                            "\n" +
                            "            camera = new THREE.PerspectiveCamera(45, viewer.clientWidth / viewer.clientHeight, 0.1, 1000);\n" +
                            "            camera.position.set(2.8, 2.2, 4.4);\n" +
                            "\n" +
                            "            renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });\n" +
                            "            renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));\n" +
                            "            renderer.setSize(viewer.clientWidth, viewer.clientHeight);\n" +
                            "            renderer.outputEncoding = THREE.sRGBEncoding;\n" +
                            "            renderer.toneMapping = THREE.ACESFilmicToneMapping;\n" +
                            "            renderer.toneMappingExposure = 0.82;\n" +
                            "            viewer.appendChild(renderer.domElement);\n" +
                            "\n" +
                            "            controls = new THREE.OrbitControls(camera, renderer.domElement);\n" +
                            "            controls.enableDamping = true;\n" +
                            "            controls.target.set(0, 0.75, 0);\n" +
                            "\n" +
                            "            scene.add(new THREE.HemisphereLight(0xffffff, 0x8fa4b8, 0.65));\n" +
                            "            scene.add(new THREE.AmbientLight(0xffffff, 0.45));\n" +
                            "\n" +
                            "            const keyLight = new THREE.DirectionalLight(0xffffff, 1.15);\n" +
                            "            keyLight.position.set(5, 8, 6);\n" +
                            "            scene.add(keyLight);\n" +
                            "\n" +
                            "            const fillLight = new THREE.DirectionalLight(0xb9d6ff, 0.35);\n" +
                            "            fillLight.position.set(-4, 4, -5);\n" +
                            "            scene.add(fillLight);\n" +
                            "\n" +
                            "            const floor = new THREE.Mesh(\n" +
                            "                new THREE.CircleGeometry(8, 80),\n" +
                            "                new THREE.MeshStandardMaterial({\n" +
                            "                    color: 0xc8d7e4,\n" +
                            "                    transparent: true,\n" +
                            "                    opacity: 0.75\n" +
                            "                })\n" +
                            "            );\n" +
                            "            floor.rotation.x = -Math.PI / 2;\n" +
                            "            floor.position.y = -1.1;\n" +
                            "            scene.add(floor);\n" +
                            "\n" +
                            "            loader = new THREE.GLTFLoader();\n"  +
                            "\n" +
                            "            if (loader.createImageBitmap !== undefined) {\n" +
                            "                loader.createImageBitmap = false;\n" +
                            "            }\n" +
                            "\n" +
                            "            raycaster = new THREE.Raycaster();\n" +
                            "            mouse = new THREE.Vector2();\n" +
                            "\n" +
                            "            viewer.addEventListener('pointerdown', onPointerDown, false);\n" +
                            "        }\n" +
                            "\n" +
                            "        function onPointerDown(event) {\n" +
                            "            if (!activeModel) return;\n" +
                            "\n" +
                            "            const rect = viewer.getBoundingClientRect();\n" +
                            "            mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1;\n" +
                            "            mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1;\n" +
                            "\n" +
                            "            raycaster.setFromCamera(mouse, camera);\n" +
                            "            const intersects = raycaster.intersectObject(activeModel, true);\n" +
                            "\n" +
                            "            if (intersects.length > 0) {\n" +
                            "                if (isCreatingTag) {\n" +
                            "                    pendingTargetMesh = intersects[0].object;\n" +
                            "                    pendingTargetPoint = intersects[0].point;\n" +
                            "                    const meshName = pendingTargetMesh.name || \"Unnamed Mesh\";\n" +
                            "                    document.getElementById('tagTargetDisplay').innerText = meshName;\n" +
                            "                    setStatus(\"Selected: \" + meshName + \". Adjust camera if needed, then click 'Save Tag'.\");\n" +
                            "                    return;\n" +
                            "                }\n" +
                            "\n" +
                            "                const clickedMesh = intersects[0].object;\n" +
                            "                console.log(\"Clicked Mesh Name:\", clickedMesh.name);\n" +
                            "                setStatus(\"Clicked: \" + (clickedMesh.name || \"Unnamed Mesh\"));\n" +
                            "            }\n" +
                            "        }\n" +
                            "\n" +
                            "        function prepareModelMaterials(object) {\n" +
                            "            object.traverse((child) => {\n" +
                            "                if (!child.isMesh || !child.material) {\n" +
                            "                    return;\n" +
                            "                }\n" +
                            "\n" +
                            "                const materials = Array.isArray(child.material) ? child.material : [child.material];\n" +
                            "                for (const material of materials) {\n" +
                            "                    for (const key of [\"map\", \"emissiveMap\", \"sheenColorMap\"]) {\n" +
                            "                        if (material[key]) {\n" +
                            "                            material[key].encoding = THREE.sRGBEncoding;\n" +
                            "                            material[key].needsUpdate = true;\n" +
                            "                        }\n" +
                            "                    }\n" +
                            "                    material.needsUpdate = true;\n" +
                            "                }\n" +
                            "            });\n" +
                            "        }\n" +
                            "\n" +
                            "        function arrayBufferToBase64(buffer) {\n" +
                            "            const bytes = new Uint8Array(buffer);\n" +
                            "            const chunkSize = 0x8000;\n" +
                            "            let binary = \"\";\n" +
                            "\n" +
                            "            for (let index = 0; index < bytes.length; index += chunkSize) {\n" +
                            "                binary += String.fromCharCode.apply(null, bytes.subarray(index, index + chunkSize));\n" +
                            "            }\n" +
                            "\n" +
                            "            return btoa(binary);\n" +
                            "        }\n" +
                            "\n" +
                            "        function patchGlbEmbeddedImages(arrayBuffer) {\n" +
                            "            const dataView = new DataView(arrayBuffer);\n" +
                            "            const magic = dataView.getUint32(0, true);\n" +
                            "            const version = dataView.getUint32(4, true);\n" +
                            "\n" +
                            "            if (magic !== 0x46546c67 || version !== 2) {\n" +
                            "                return arrayBuffer;\n" +
                            "            }\n" +
                            "\n" +
                            "            const jsonChunkLength = dataView.getUint32(12, true);\n" +
                            "            const jsonChunkType = dataView.getUint32(16, true);\n" +
                            "\n" +
                            "            if (jsonChunkType !== 0x4e4f534a) {\n" +
                            "                return arrayBuffer;\n" +
                            "            }\n" +
                            "\n" +
                            "            const jsonBytes = new Uint8Array(arrayBuffer, 20, jsonChunkLength);\n" +
                            "            const jsonText = new TextDecoder().decode(jsonBytes).replace(/\\0+$/g, \"\").trim();\n" +
                            "            const gltf = JSON.parse(jsonText);\n" +
                            "            const firstBinaryChunkOffset = 20 + jsonChunkLength;\n" +
                            "\n" +
                            "            if (!gltf.images || !gltf.bufferViews || firstBinaryChunkOffset + 8 > arrayBuffer.byteLength) {\n" +
                            "                return arrayBuffer;\n" +
                            "            }\n" +
                            "\n" +
                            "            const binaryChunkLength = dataView.getUint32(firstBinaryChunkOffset, true);\n" +
                            "            const binaryChunkType = dataView.getUint32(firstBinaryChunkOffset + 4, true);\n" +
                            "\n" +
                            "            if (binaryChunkType !== 0x004e4942) {\n" +
                            "                return arrayBuffer;\n" +
                            "            }\n" +
                            "\n" +
                            "            const binaryOffset = firstBinaryChunkOffset + 8;\n" +
                            "            let changed = false;\n" +
                            "\n" +
                            "            for (const image of gltf.images) {\n" +
                            "                if (image.bufferView === undefined || !image.mimeType) {\n" +
                            "                    continue;\n" +
                            "                }\n" +
                            "\n" +
                            "                const bufferView = gltf.bufferViews[image.bufferView];\n" +
                            "                const byteOffset = binaryOffset + (bufferView.byteOffset || 0);\n" +
                            "                const byteLength = bufferView.byteLength;\n" +
                            "                const imageBytes = arrayBuffer.slice(byteOffset, byteOffset + byteLength);\n" +
                            "                image.uri = \"data:\" + image.mimeType + \";base64,\" + arrayBufferToBase64(imageBytes);\n" +
                            "                delete image.bufferView;\n" +
                            "                delete image.mimeType;\n" +
                            "                changed = true;\n" +
                            "            }\n" +
                            "\n" +
                            "            if (!changed) {\n" +
                            "                return arrayBuffer;\n" +
                            "            }\n" +
                            "\n" +
                            "            const encoder = new TextEncoder();\n" +
                            "            let patchedJson = JSON.stringify(gltf);\n" +
                            "            const paddedJsonLength = Math.ceil(patchedJson.length / 4) * 4;\n" +
                            "            patchedJson = patchedJson.padEnd(paddedJsonLength, \" \");\n" +
                            "            const patchedJsonBytes = encoder.encode(patchedJson);\n" +
                            "            const totalLength = 12 + 8 + patchedJsonBytes.length + 8 + binaryChunkLength;\n" +
                            "            const output = new ArrayBuffer(totalLength);\n" +
                            "            const outputView = new DataView(output);\n" +
                            "            let offset = 0;\n" +
                            "\n" +
                            "            outputView.setUint32(offset, magic, true);\n" +
                            "            outputView.setUint32(offset + 4, version, true);\n" +
                            "            outputView.setUint32(offset + 8, totalLength, true);\n" +
                            "            offset += 12;\n" +
                            "            outputView.setUint32(offset, patchedJsonBytes.length, true);\n" +
                            "            outputView.setUint32(offset + 4, jsonChunkType, true);\n" +
                            "            offset += 8;\n" +
                            "            new Uint8Array(output, offset, patchedJsonBytes.length).set(patchedJsonBytes);\n" +
                            "            offset += patchedJsonBytes.length;\n" +
                            "            outputView.setUint32(offset, binaryChunkLength, true);\n" +
                            "            outputView.setUint32(offset + 4, binaryChunkType, true);\n" +
                            "            offset += 8;\n" +
                            "            new Uint8Array(output, offset, binaryChunkLength).set(new Uint8Array(arrayBuffer, binaryOffset, binaryChunkLength));\n" +
                            "\n" +
                            "            return output;\n" +
                            "        }\n" +
                            "\n" +
                            "        function parseModelArrayBuffer(arrayBuffer, modelLabel) {\n" +
                            "            const patchedBuffer = arrayBuffer;\n" +
                            "\n" +
                            "            loader.parse(\n" +
                            "                patchedBuffer,\n" +
                            "                \"\",\n" +
                            "                (gltf) => {\n" +
                            "                    activeModel = gltf.scene;\n" +
                            "                    prepareModelMaterials(activeModel);\n" +
                            "                    modelContainer.add(activeModel);\n" +
                            "                    frameModel(activeModel);\n" +
                            "                    updateCameraNavVisibility(modelLabel);\n" +
                            "                    setStatus(modelLabel + \" rendered successfully.\");\n" +
                            "                },\n" +
                            "                (error) => {\n" +
                            "                    console.error(error);\n" +
                            "                    setStatus(\"Model could not be loaded.\");\n" +
                            "                }\n" +
                            "            );\n" +
                            "        }\n" +
                            "\n" +
                            "        function loadModel(model) {\n" +
                            "            setStatus(\"Loading model...\");\n" +
                            "            clearCurrentModel();\n" +
                            "\n" +
                            "            const modelUrls = getModelUrls(model);\n" +
                            "            let attemptIndex = 0;\n" +
                            "\n" +
                            "            function tryLoadNext() {\n" +
                            "                const modelUrl = modelUrls[attemptIndex];\n" +
                            "\n" +
                            "                loader.load(\n" +
                            "                    modelUrl,\n" +
                            "                    (gltf) => {\n" +
                            "\n" +
                            "\n" +
                            "                        activeModel = gltf.scene;\n" +
                            "                        prepareModelMaterials(activeModel);\n" +
                            "                        modelContainer.add(activeModel);\n" +
                            "                        frameModel(activeModel);\n" +
                            "                        updateCameraNavVisibility(model.label);\n" +
                            "\n" +
                            "                        setStatus(model.label + \" rendered successfully ✔ (no textures)\");\n" +
                            "                    },\n" +
                            "                    (xhr) => {\n" +
                            "                        const progressBar = document.getElementById(\"loaderProgressBar\");\n" +
                            "                        const percentText = document.getElementById(\"loaderPercent\");\n" +
                            "                        \n" +
                            "                        if (xhr.lengthComputable) {\n" +
                            "                            const percentComplete = Math.round((xhr.loaded / xhr.total) * 100);\n" +
                            "                            if (progressBar) {\n" +
                            "                                progressBar.style.width = percentComplete + \"%\";\n" +
                            "                                progressBar.style.animation = \"none\";\n" +
                            "                            }\n" +
                            "                            if (percentText) percentText.textContent = percentComplete + \"%\";\n" +
                            "                        } else {\n" +
                            "                            const mbLoaded = (xhr.loaded / (1024 * 1024)).toFixed(2);\n" +
                            "                            if (percentText) percentText.textContent = `Downloaded ${mbLoaded} MB...`;\n" +
                            "                            if (progressBar) {\n" +
                            "                                progressBar.style.width = \"50%\";\n" +
                            "                                progressBar.style.animation = \"indeterminate 1.5s infinite ease-in-out\";\n" +
                            "                            }\n" +
                            "                        }\n" +
                            "                    },\n" +
                            "                    (error) => {\n" +
                            "                        console.error(error);\n" +
                            "                        setStatus(\"Model load failed ❌\");\n" +
                            "                    }\n" +
                            "                );\n" +
                            "            }\n" +
                            "\n" +
                            "            tryLoadNext();\n" +
                            "        }\n" +
                            "\n" +
                            "        function loadModelFile(file) {\n" +
                            "            if (!file) {\n" +
                            "                return;\n" +
                            "            }\n" +
                            "\n" +
                            "            setStatus(\"Loading \" + file.name + \"...\");\n" +
                            "            clearCurrentModel();\n" +
                            "\n" +
                            "            const reader = new FileReader();\n" +
                            "            reader.onprogress = (e) => {\n" +
                            "                const progressBar = document.getElementById(\"loaderProgressBar\");\n" +
                            "                const percentText = document.getElementById(\"loaderPercent\");\n" +
                            "                if (e.lengthComputable) {\n" +
                            "                    const percentComplete = Math.round((e.loaded / e.total) * 100);\n" +
                            "                    if (progressBar) {\n" +
                            "                        progressBar.style.width = percentComplete + \"%\";\n" +
                            "                        progressBar.style.animation = \"none\";\n" +
                            "                    }\n" +
                            "                    if (percentText) percentText.textContent = percentComplete + \"%\";\n" +
                            "                } else {\n" +
                            "                    const mbLoaded = (e.loaded / (1024 * 1024)).toFixed(2);\n" +
                            "                    if (percentText) percentText.textContent = `Processed ${mbLoaded} MB...`;\n" +
                            "                    if (progressBar) {\n" +
                            "                        progressBar.style.width = \"50%\";\n" +
                            "                        progressBar.style.animation = \"indeterminate 1.5s infinite ease-in-out\";\n" +
                            "                    }\n" +
                            "                }\n" +
                            "            };\n" +
                            "            reader.onload = () => {\n" +
                            "                parseModelArrayBuffer(reader.result, file.name);\n" +
                            "            };\n" +
                            "            reader.onerror = () => {\n" +
                            "                setStatus(\"Selected file could not be read.\");\n" +
                            "            };\n" +
                            "            reader.readAsArrayBuffer(file);\n" +
                            "        }\n" +
                            "\n" +
                            "        window.addEventListener(\"resize\", () => {\n" +
                            "            if (!renderer) {\n" +
                            "                return;\n" +
                            "            }\n" +
                            "\n" +
                            "            const width = viewer.clientWidth;\n" +
                            "            const height = viewer.clientHeight;\n" +
                            "            camera.aspect = width / height;\n" +
                            "            camera.updateProjectionMatrix();\n" +
                            "            renderer.setSize(width, height);\n" +
                            "        });\n" +
                            "\n" +
                            "        function animate() {\n" +
                            "            requestAnimationFrame(animate);\n" +
                            "            controls.update();\n" +
                            "            renderer.render(scene, camera);\n" +
                            "        }\n" +
                            "\n" +
                            "        async function start() {\n" +
                            "            setStatus(\"Loading 3D library...\");\n" +
                            "            THREE = await loadThreeScripts();\n" +
                            "            initViewer();\n" +
                            "            animate();\n" +
                            "            setStatus(\"Viewer ready.\");\n" +
                            "\n" +
                            "            renderVirtualFS();\n" +
                            "\n" +
                            "            if (virtualFS[0] && virtualFS[0].files.length > 0) {\n" +
                            "                const firstFile = virtualFS[0].files[0];\n" +
                            "                activeFileId = firstFile.id;\n" +
                            "                renderVirtualFS();\n" +
                            "                loadModel({ label: firstFile.name, fileNames: firstFile.fileNames, folderName: virtualFS[0].name });\n" +
                            "            }\n" +
                            "        }\n" +
                            "\n" +
                            "        start().catch((error) => {\n" +
                            "            console.error(error);\n" +
                            "            setStatus(\"3D library could not load from cdn.jsdelivr.net.\");\n" +
                            "        });\n" +
                            "    </script>\n" +
                            "</body>\n" +
                            "\n" +
                            "</html>";


            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "http://localhost:8080"); // or your Niagara URL
            exchange.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");
            exchange.sendResponseHeaders(200, html.getBytes().length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(html.getBytes());
            }
        }
    }



    // common handler wrapper for error and response handling
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
}
