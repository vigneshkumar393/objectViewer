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

public class ConfigurationRoute {

    public static void registerRoutes(HttpServer server) {
        ConfigurationController controller = new ConfigurationController();
        server.createContext("/configuration", new ConfigurationHandler());

        server.createContext("/savePoints", exchange -> handle(exchange, () -> {

            if (!AuthRoute.isTokenValid(exchange)) {
                JSONObject error = new JSONObject();
                error.put("status", "error");
                error.put("message", "Unauthorized");
                return error;
            }

            // Read JSON body (Java 8 compatible)
            String body = readRequestBody(exchange);
            JSONObject json = new JSONObject(body);

            // Extract points array
            com.tridium.json.JSONArray arr = json.getJSONArray("points");

            String[] points = new String[arr.length()];
            for (int i = 0; i < arr.length(); i++) {
                points[i] = arr.getString(i);
            }

            return controller.savePoints(points);
        }));


    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString("UTF-8");
    }


    static class ConfigurationHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {

            String html =
                    "<!DOCTYPE html>\n" +
                            "<html lang=\"en\">\n" +
                            "<head>\n" +
                            "    <meta charset=\"UTF-8\">\n" +
                            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                            "    <title>Configuration</title>\n" +
                            "\n" +
                            "    <style>\n" +
                            "        html, body {\n" +
                            "            height: 100%;\n" +
                            "            margin: 0;\n" +
                            "            overflow: hidden;  /* REMOVE ENTIRE SCREEN SCROLL */\n" +
                            "            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n" +
                            "            background: #f5f6fa;\n" +
                            "        }\n" +
                            "\n" +
                            "        /* ─────────────── TOP BAR ─────────────── */\n" +
                            "        .top-bar {\n" +
                            "            width: 100%;\n" +
                            "            background: #2f3640;\n" +
                            "            color: white;\n" +
                            "            display: flex;\n" +
                            "            justify-content: space-between;\n" +
                            "            align-items: center;\n" +
                            "            padding: 15px;\n" +
                            "            position: fixed;\n" +
                            "            top: 0;\n" +
                            "            left: 0;\n" +
                            "            z-index: 100;\n" +
                            "            box-shadow: 0 4px 8px rgba(0,0,0,0.1);\n" +
                            "        }\n" +
                            "\n" +
                            "        .top-bar h1 {\n" +
                            "            margin-left: 15px;\n" +
                            "            font-size: 20px;\n" +
                            "        }\n" +
                            ".modal-overlay.show {\n" +
                            "    display: flex;\n" +
                            "}\n"+
                            ".top-actions {\n" +
                            "    display: flex;\n" +
                            "    gap: 10px;                         /* space between settings + logout */\n" +
                            "    align-items: center;\n" +
                            "}"+
                            "\n" +
                            "        .logout-btn {\n" +
                            "            margin-right: 40px;\n" +
                            "            padding: 8px 16px;\n" +
                            "            background: #e84118;\n" +
                            "            color: white;\n" +
                            "            border: none;\n" +
                            "            border-radius: 5px;\n" +
                            "            cursor: pointer;\n" +
                            "            font-weight: bold;\n" +
                            "        }\n" +
                            "\n" +
                            "        /* ─────────────── MAIN LAYOUT FIXED HEIGHT ─────────────── */\n" +
                            "        .container {\n" +
                            "            position: absolute;\n" +
                            "            top: 60px;        /* below top bar */\n" +
                            "            bottom: 60px;     /* above save btn */\n" +
                            "            left: 0;\n" +
                            "            right: 0;\n" +
                            "            display: flex;\n" +
                            "            gap: 20px;\n" +
                            "            padding: 20px;\n" +
                            "            overflow: hidden; /* no screen scroll */\n" +
                            "        }\n" +
                            "\n" +
                            "        .left-column, .right-column {\n" +
                            "            flex: 1;\n" +
                            "            display: flex;\n" +
                            "            flex-direction: column;\n" +
                            "            overflow: hidden;\n" +
                            "        }\n" +
                            "\n" +
                            "        #searchBox, #searchSelected {\n" +
                            "            padding: 10px;\n" +
                            "            border-radius: 6px;\n" +
                            "            border: 1px solid #dcdde1;\n" +
                            "            margin-bottom: 10px;\n" +
                            "            font-size: 16px;\n" +
                            "        }\n" +
                            "\n" +
                            "        /* ─────────────── SCROLL ONLY INSIDE LISTS ─────────────── */\n" +
                            "        #pointsDropdown, #selectedPoints {\n" +
                            "            flex: 1;\n" +
                            "            padding: 5px;\n" +
                            "            border-radius: 6px;\n" +
                            "            border: 1px solid #dcdde1;\n" +
                            "            font-size: 16px;\n" +
                            "            overflow-y: auto; /* LEFT + RIGHT ONLY SCROLL */\n" +
                            "        }\n" +
                            "\n" +
                            "        /* HIDING SCROLL BAR */\n" +
                            "        #pointsDropdown, #selectedPoints {\n" +
                            "            scrollbar-width: none;\n" +
                            "            -ms-overflow-style: none;\n" +
                            "        }\n" +
                            "        #pointsDropdown::-webkit-scrollbar, #selectedPoints::-webkit-scrollbar {\n" +
                            "            display: none;\n" +
                            "        }\n" +
                            "\n" +
                            "        /* Left list items */\n" +
                            "        .dropdown-option {\n" +
                            "            padding: 8px;\n" +
                            "            margin: 3px 0;\n" +
                            "            border-radius: 16px;\n" +
                            "            cursor: pointer;\n" +
                            "            transition: background 0.2s;\n" +
                            "        }\n" +
                            "\n" +
                            "        .dropdown-option:hover {\n" +
                            "            background-color: #d9f7e8;\n" +
                            "        }\n" +
                            "\n" +
                            "        .dropdown-option.selected {\n" +
                            "            background-color: #7bed9f;\n" +
                            "            color: #2f3640;\n" +
                            "        }\n" +
                            "\n" +
                            "        /* Selected points right side */\n" +
                            "        .selected-points {\n" +
                            "            display: flex;\n" +
                            "            flex-direction: column;\n" +
                            "            gap: 10px;\n" +
                            "        }\n" +
                            "\n" +
                            "        .point-chip {\n" +
                            "            background: #4cd137;\n" +
                            "            color: white;\n" +
                            "            padding: 8px 12px;\n" +
                            "            border-radius: 20px;\n" +
                            "            display: flex;\n" +
                            "            justify-content: space-between;\n" +
                            "            align-items: center;\n" +
                            "        }\n" +
                            "\n" +
                            "        .point-chip button {\n" +
                            "            background: none;\n" +
                            "            border: none;\n" +
                            "            color: white;\n" +
                            "            cursor: pointer;\n" +
                            "            font-weight: bold;\n" +
                            "        }\n" +
                            "\n" +
                            "        /* Save button */\n" +
                            "        .save-btn-container {\n" +
                            "            width: 100%;\n" +
                            "            text-align: center;\n" +
                            "            padding: 15px 0;\n" +
                            "            position: fixed;\n" +
                            "            bottom: 0;\n" +
                            "            left: 0;\n" +
                            "            background: #f5f6fa;\n" +
                            "        }\n" +
                            "\n" +
                            "        .save-btn {\n" +
                            "            padding: 12px 30px;\n" +
                            "            background: #0097e6;\n" +
                            "            color: white;\n" +
                            "            font-size: 16px;\n" +
                            "            border: none;\n" +
                            "            border-radius: 8px;\n" +
                            "            cursor: pointer;\n" +
                            "            font-weight: bold;\n" +
                            "        }\n" +
                            ".settings-btn {\n" +
                            "    margin-right: 10px;\n" +
                            "    padding: 8px 16px;\n" +
                            "    background: #40739e;\n" +
                            "    color: white;\n" +
                            "    border: none;\n" +
                            "    border-radius: 5px;\n" +
                            "    cursor: pointer;\n" +
                            "    font-weight: bold;\n" +
                            "}\n" +
                            ".cert-box {\n" +
                            "    display: flex;\n" +
                            "    justify-content: space-between;\n" +
                            "    align-items: center;\n" +
                            "    background: #f2f2f2;\n" +
                            "    padding: 10px 14px;\n" +
                            "    border-radius: 6px;\n" +
                            "    margin-bottom: 15px;\n" +
                            "    border: 1px solid #ccc;\n" +
                            "}\n" +
                            "\n" +
                            "#certFileName {\n" +
                            "    font-size: 14px;\n" +
                            "    color: #333;\n" +
                            "}\n" +
                            "\n" +
                            ".close-icon {\n" +
                            "    cursor: pointer;\n" +
                            "    font-size: 16px;\n" +
                            "    color: #d60000;\n" +
                            "    font-weight: bold;\n" +
                            "    padding: 2px 6px;\n" +
                            "}\n" +
                            "\n" +
                            ".close-icon:hover {\n" +
                            "    color: #ff0000;\n" +
                            "}\n" +
                            "\n" +
                            ".hidden {\n" +
                            "    display: none;\n" +
                            "}\n"+
                            "\n" +
                            ".settings-btn:hover {\n" +
                            "    background: #487eb0;\n" +
                            "}\n" +
                            "\n" +
                            "/* ───────────── Modal ───────────── */\n" +
                            ".modal-overlay {\n" +
                            "    position: fixed;\n" +
                            "    top: 0; left: 0; right: 0; bottom: 0;\n" +
                            "    background: rgba(0,0,0,0.6);\n" +
                            "    z-index: 1000;\n" +
                            "    display: none; /* hidden by default */\n" +
                            "    justify-content: center;\n" +
                            "    align-items: center;\n" +
                            "}\n" +
                            "\n" +
                            ".modal-box {\n" +
                            "    max-width: 450px;       /* limit width */\n" +
                            "    width: 90%;             /* responsive */\n" +
                            "    background: #fff;\n" +
                            "    padding: 20px;\n" +
                            "    border-radius: 10px;\n" +
                            "    box-shadow: 0 4px 20px rgba(0,0,0,0.3);\n" +
                            "    box-sizing: border-box; /* include padding in width */\n" +
                            "    position: relative;\n" +
                            "}\n" +
                            ".modal-overlay.show .modal-box {\n" +
                            "    animation: scaleIn 0.2s ease-out;\n" +
                            "}"+
                            "@keyframes scaleIn {\n" +
                            "    0% { transform: scale(0.8); opacity: 0; }\n" +
                            "    100% { transform: scale(1); opacity: 1; }\n" +
                            "}\n"+
                            "\n" +
                            ".modal-box h2 {\n" +
                            "    margin-top: 0;\n" +
                            "}\n" +
                            "\n" +
                            ".modal-box input {\n" +
                            "    width: 90%;\n" +
                            "    padding: 10px;\n" +
                            "    margin: 10px 0;\n" +
                            "    border-radius: 6px;\n" +
                            "    border: 1px solid #ccc;\n" +
                            "}\n" +
                            "\n" +
                            ".modal-actions {\n" +
                            "    text-align: right;\n" +
                            "}\n" +
                            "\n" +
                            ".modal-btn {\n" +
                            "    padding: 10px 16px;\n" +
                            "    border: none;\n" +
                            "    border-radius: 6px;\n" +
                            "    cursor: pointer;\n" +
                            "    font-weight: bold;\n" +
                            "}\n" +
                            "\n" +
                            ".modal-btn.save {\n" +
                            "    background: #44bd32;\n" +
                            "    color: white;\n" +
                            "}\n" +
                            "\n" +
                            ".modal-btn.cancel {\n" +
                            "    background: #e84118;\n" +
                            "    color: white;\n" +
                            "}\n"+
                            "    </style>\n" +
                            "</head>\n" +
                            "\n" +
                            "<body>\n" +
                            "\n" +
                            "<div class=\"top-bar\">\n" +
                            "    <div class=\"title\">Http Server Driver</div>\n" +
                            "\n" +
                            "    <div class=\"top-actions\">\n" +
                            "        <button class=\"settings-btn\" onclick=\"openSettings()\">⚙️</button>\n" +
                            "        <button class=\"logout-btn\" onclick=\"logout()\">Logout</button>\n" +
                            "    </div>\n" +
                            "</div>\n" +
                            "\n" +
                            "<div class=\"container\">\n" +
                            "    <div class=\"left-column\">\n" +
                            "        <h2>Configuration</h2>\n" +
                            "        <input type=\"text\" id=\"searchBox\" placeholder=\"Search points...\">\n" +
                            "        <div id=\"pointsDropdown\"></div>\n" +
                            "    </div>\n" +
                            "\n" +
                            "    <div class=\"right-column\">\n" +
                            "        <h2>Selected Points</h2>\n" +
                            "        <input type=\"text\" id=\"searchSelected\" placeholder=\"Search selected points...\">\n" +
                            "        <div class=\"selected-points\" id=\"selectedPoints\"></div>\n" +
                            "    </div>\n" +
                            "</div>\n" +
                            "\n" +
                            "<div class=\"save-btn-container\">\n" +
                            "    <button class=\"save-btn\" onclick=\"saveChanges()\">Save Changes</button>\n" +
                            "</div>\n" +
                            "\n" +
                            "<script>\n" +
                            "let originalSavedPoints = [];\n"+
                            "function logout() {\n" +
                            "    if (hasUnsavedChanges()) {\n" +
                            "        const confirmLeave = confirm(\n" +
                            "            \"You have unsaved changes!\\nIf you logout, they will be lost.\\n\\nDo you want to continue?\"\n" +
                            "        );\n" +
                            "\n" +
                            "        if (!confirmLeave) return;  // cancel logout\n" +
                            "    }\n" +
                            "\n" +
                            "    window.location.href = \"/\";\n" +
                            "}\n" +
                            "\n" +
                            "const selectedPointsMap = new Map();\n" +
                            "const dropdown = document.getElementById('pointsDropdown');\n" +
                            "const selectedContainer = document.getElementById('selectedPoints');\n" +
                            "const searchBox = document.getElementById('searchBox');\n" +
                            "const searchSelected = document.getElementById('searchSelected');\n" +
                            "\n" +
                            "function populateDropdown(points) {\n" +
                            "    dropdown.innerHTML = '';\n" +
                            "\n" +
                            "    points.forEach(point => {\n" +
                            "        const div = document.createElement('div');\n" +
                            "        div.className = 'dropdown-option';\n" +
                            "        div.textContent = point.pointName + ' (' + point.type + ')';\n" +
                            "        div.dataset.value = point.pointName;\n" +
                            "\n" +
                            "        div.onclick = () => toggleSelect(point.pointName, div);\n" +
                            "        dropdown.appendChild(div);\n" +
                            "\n" +
                            "        // ⭐ AUTO-SELECT IF isSaved == true\n" +
                            "        if (point.isSaved === true) {\n" +
                            "            selectedPointsMap.set(point.pointName, div.textContent);\n" +
                            "        }\n" +
                            "    });\n" +
                            "\n" +
                            "    // Render already saved items to right side\n" +
                            "    renderSelectedPoints();\n" +
                            "\n" +
                            "    // Apply highlight style on left dropdown\n" +
                            "    highlightDropdown();\n" +
                            "}\n\n" +
                            "\n" +
                            "function toggleSelect(value, div) {\n" +
                            "    if (selectedPointsMap.has(value)) selectedPointsMap.delete(value);\n" +
                            "    else selectedPointsMap.set(value, div.textContent);\n" +
                            "\n" +
                            "    renderSelectedPoints();\n" +
                            "    highlightDropdown();\n" +
                            "}\n" +
                            "\n" +
                            "function renderSelectedPoints() {\n" +
                            "    selectedContainer.innerHTML = '';\n" +
                            "    selectedPointsMap.forEach((label, value) => {\n" +
                            "        const chip = document.createElement('div');\n" +
                            "        chip.className = 'point-chip';\n" +
                            "        chip.textContent = label;\n" +
                            "\n" +
                            "        const btn = document.createElement('button');\n" +
                            "        btn.textContent = '×';\n" +
                            "        btn.onclick = () => {\n" +
                            "            selectedPointsMap.delete(value);\n" +
                            "            renderSelectedPoints();\n" +
                            "            highlightDropdown();\n" +
                            "        };\n" +
                            "\n" +
                            "        chip.appendChild(btn);\n" +
                            "        selectedContainer.appendChild(chip);\n" +
                            "    });\n" +
                            "}\n" +
                            "\n" +
                            "function hasUnsavedChanges() {\n" +
                            "    const currentlySelected = Array.from(selectedPointsMap.keys());\n" +
                            "\n" +
                            "    if (currentlySelected.length !== originalSavedPoints.length) return true;\n" +
                            "\n" +
                            "    const a = [...currentlySelected].sort();\n" +
                            "    const b = [...originalSavedPoints].sort();\n" +
                            "\n" +
                            "    return JSON.stringify(a) !== JSON.stringify(b);\n" +
                            "}"+
                            "function highlightDropdown() {\n" +
                            "    Array.from(dropdown.children).forEach(div => {\n" +
                            "        if (selectedPointsMap.has(div.dataset.value))\n" +
                            "            div.classList.add('selected');\n" +
                            "        else\n" +
                            "            div.classList.remove('selected');\n" +
                            "    });\n" +
                            "}\n" +
                            "\n" +
                            "searchBox.addEventListener('input', e => {\n" +
                            "    const filter = e.target.value.toLowerCase();\n" +
                            "    Array.from(dropdown.children).forEach(div => {\n" +
                            "        div.style.display = div.textContent.toLowerCase().includes(filter) ? 'block' : 'none';\n" +
                            "    });\n" +
                            "});\n" +
                            "\n" +
                            "searchSelected.addEventListener('input', e => {\n" +
                            "    const filter = e.target.value.toLowerCase();\n" +
                            "    Array.from(selectedContainer.children).forEach(div => {\n" +
                            "        div.style.display = div.textContent.toLowerCase().includes(filter) ? 'flex' : 'none';\n" +
                            "    });\n" +
                            "});\n" +
                            "\n" +
                            "async function saveChanges() {\n" +
                            "    const selected = Array.from(selectedPointsMap.keys());\n" +
                            "    const token = localStorage.getItem(\"token\");\n" +
                            "\n" +
                            "    try {\n" +
                            "        const res = await fetch(\"/savePoints\", {\n" +
                            "            method: \"POST\",\n" +
                            "            headers: {\n" +
                            "                \"Content-Type\": \"application/json\",\n" +
                            "                \"Authorization\": \"Bearer \" + token\n" +
                            "            },\n" +
                            "            body: JSON.stringify({ points: selected })\n" +
                            "        });\n" +
                            "\n" +
                            "        const data = await res.json();\n" +
                            "\n" +
                            "        // ⭐ SESSION EXPIRED\n" +
                            "        if (data.message === \"Unauthorized\") {\n" +
                            "            alert(\"Session expired! Please login again.\");\n" +
                            "            localStorage.removeItem(\"token\");\n" +
                            "            window.location.href = \"/\";\n" +
                            "            return;\n" +
                            "        }\n" +
                            "\n" +
                            "        if (data.status === \"success\") {\n" +
                            "            alert(\"Points saved successfully!\");\n" +
                            "        } else {\n" +
                            "            alert(\"Failed to save points: \" + data.message);\n" +
                            "        }\n" +
                            "\n" +
                            "    } catch (err) {\n" +
                            "        console.error(err);\n" +
                            "        alert(\"Server error while saving points\");\n" +
                            "    }\n" +
                            "}\n" +
                            "\n" +
                            "async function fetchPoints() {\n" +
                            "    try {\n" +
                            "        const token = localStorage.getItem('token');\n" +
                            "        const res = await fetch('/getPoints', {\n" +
                            "            headers: { 'Authorization': 'Bearer ' + token }\n" +
                            "        });\n" +
                            "\n" +
                            "        const data = await res.json();\n" +
                            "\n" +
                            "        if (data.message === \"Unauthorized\") {\n" +
                            "            alert(\"Session expired! Login again.\");\n" +
                            "            localStorage.removeItem(\"token\");\n" +
                            "            window.location.href = \"/\";\n" +
                            "            return;\n" +
                            "        }\n" +
                            "\n" +
                            "        if (data.status === 'success') {\n" +
                            "            originalSavedPoints = data.points\n" +
                            "                .filter(p => p.isSaved === true)\n" +
                            "                .map(p => p.pointName);\n" +
                            "\n" +
                            "            populateDropdown(data.points);\n" +
                            "\n" +
                            "        } else {\n" +
                            "            alert('Failed to fetch points');\n" +
                            "        }\n" +
                            "\n" +
                            "    } catch (err) {\n" +
                            "        console.error(err);\n" +
                            "        alert('Server error');\n" +
                            "    }\n" +
                            "}\n\n" +
                            "\n" +
                            "function openSettings() {\n" +
                            "    const overlay = document.getElementById(\"settingsModal\");\n" +
                            "    overlay.classList.add(\"show\");  // instead of display:block\n" +
                            "    loadCertificateDetails();\n" +
                            "}\n" +
                            "\n" +
                            "function closeSettings() {\n" +
                            "    const overlay = document.getElementById(\"settingsModal\");\n" +
                            "    overlay.classList.remove(\"show\");\n" +
                            "}" +
                            "async function uploadCertificate() {\n" +
                            "    const fileInput = document.getElementById(\"certFile\");\n" +
                            "    const password = document.getElementById(\"certPassword\");\n" +
                            "    const token = localStorage.getItem(\"token\");\n" +
                            "\n" +
                            "    if (!fileInput.files.length) {\n" +
                            "        alert(\"Please select a certificate file (.p12 or .pfx)\");\n" +
                            "        return;\n" +
                            "    }\n" +
                            "\n" +
                            "    if (!password.value.trim()) {\n" +
                            "        alert(\"Please enter certificate password\");\n" +
                            "        return;\n" +
                            "    }\n" +
                            "\n" +
                            "    const formData = new FormData();\n" +
                            "    formData.append(\"file\", fileInput.files[0]);\n" +
                            "    formData.append(\"password\", password.value);\n" +
                            "\n" +
                            "    try {\n" +
                            "        const res = await fetch(\"/uploadCertificate\", {\n" +
                            "            method: \"POST\",\n" +
                            "            headers: {\n" +
                            "                \"Authorization\": \"Bearer \" + token\n" +
                            "            },\n" +
                            "            body: formData\n" +
                            "        });\n" +
                            "\n" +
                            "        const data = await res.json();\n" +
                            "\n" +
                            "        if (data.status === \"success\") {\n" +
                            "            alert(\"Certificate uploaded successfully.\\nHTTPS restarting...\");\n" +
                            "// ⭐ Call upgradeToHttps after successful upload\n" +
                            "   // ⭐ Call upgradeToHttps after successful upload\n" +
                            "try {\n" +
                            "    const upgradeRes = await fetch(\"/upgradeToHttps\", {\n" +
                            "        method: \"POST\",\n" +
                            "        headers: {\n" +
                            "            \"Authorization\": \"Bearer \" + token\n" +
                            "        }\n" +
                            "    });\n" +
                            "    const upgradeData = await upgradeRes.json();\n" +
                            "\n" +
                            "    if (upgradeData.status === \"success\") {\n" +
                            "        alert(\"HTTPS upgraded successfully ✅\");\n" +
                            "    } else {\n" +
                            "        // Display the error returned by server\n" +
                            "        alert(\"HTTPS upgrade failed ❌\\n\" + upgradeData.message);\n" +
                            "        console.error(\"HTTPS upgrade failed:\", upgradeData.message);\n" +
                            "    }\n" +
                            "} catch (err) {\n" +
                            "    console.error(\"Error calling upgradeToHttps:\", err);\n" +
                            "    alert(\"Error calling upgradeToHttps. See console for details.\");\n" +
                            "}\n"+
                            "            closeSettings();\n" +
                            "        } else {\n" +
                            "            alert(\"Error: \" + data.message);\n" +
                            "        }\n" +
                            "\n" +
                            "    } catch (err) {\n" +
                            "        alert(\"Server error while uploading certificate\");\n" +
                            "        console.error(err);\n" +
                            "    }\n" +
                            "}\n"+
                            "function loadCertificateDetails() {\n" +
                            "    fetch(\"/getCertificateDetails\", {\n" +
                            "        method: \"GET\",\n" +
                            "        headers: { \"Authorization\": \"Bearer \" +localStorage.getItem(\"token\") }\n" +
                            "    })\n" +
                            "    .then(res => res.json())\n" +
                            "    .then(data => {\n" +
                            "        if (data.fileExists) {\n" +
                            "            // Show file name box\n" +
                            "            document.getElementById(\"certDisplayBox\").classList.remove(\"hidden\");\n" +
                            "            document.getElementById(\"certInputFields\").classList.add(\"hidden\");\n" +
                            "            document.getElementById(\"certFileName\").innerText = data.fileName;\n" +
                            "        } else {\n" +
                            "            // Show input fields\n" +
                            "            document.getElementById(\"certDisplayBox\").classList.add(\"hidden\");\n" +
                            "            document.getElementById(\"certInputFields\").classList.remove(\"hidden\");\n" +
                            "        }\n" +
                            "    });\n" +
                            "}\n"+
                            "function removeCertificate() {\n" +
                            "    // Hide certificate display\n" +
                            "    document.getElementById(\"certDisplayBox\").classList.add(\"hidden\");\n" +
                            "\n" +
                            "    // Show upload fields\n" +
                            "    document.getElementById(\"certInputFields\").classList.remove(\"hidden\");\n" +
                            "\n" +
                            "    // Clear file name text\n" +
                            "    document.getElementById(\"certFileName\").textContent = \"\";\n" +
                            "}\n"+
                            "async function validateCertificate() {\n" +
                            "    const token = localStorage.getItem(\"token\");\n" +
                            "\n" +
                            "    try {\n" +
                            "        const res = await fetch(\"/validateCertificate\", {\n" +
                            "            method: \"GET\",\n" +
                            "            headers: { \"Authorization\": \"Bearer \" + token }\n" +
                            "        });\n" +
                            "\n" +
                            "        const data = await res.json();\n" +
                            "\n" +
                            "        if (data.status === \"success\") {\n" +
                            "            alert(\"Certificate is valid ✅\\n\" + (data.message || \"\"));\n" +
                            "        } else {\n" +
                            "            alert(\"Certificate validation failed ❌\\n\" + (data.message || \"\"));\n" +
                            "        }\n" +
                            "\n" +
                            "    } catch (err) {\n" +
                            "        console.error(err);\n" +
                            "        alert(\"Server error while validating certificate\");\n" +
                            "    }\n" +
                            "}\n"+
                            "fetchPoints();\n" +
                            "</script>\n" +
                            "\n" +
                            "<div class=\"modal-overlay\" id=\"settingsModal\">\n" +
                            "<div class=\"modal-box\">\n" +
                            "    <h2>Certificate Configuration</h2>\n" +
                            "\n" +
                            "    <!-- Visible when certificate exists -->\n" +
                            "    <div id=\"certDisplayBox\" class=\"hidden cert-box\">\n" +
                            "    <span id=\"certFileName\"></span>\n" +
                            "    <span class=\"close-icon\" onclick=\"removeCertificate()\">✖</span>\n" +
                            "</div>\n\n" +
                            "\n" +
                            "    <!-- Fields (hidden when certificate exists) -->\n" +
                            "    <div id=\"certInputFields\">\n" +
                            "    <label>Certificate File (.p12)</label>\n" +
                            "    <input type=\"file\" id=\"certFile\" accept=\".p12,.pfx\">\n" +
                            "\n" +
                            "    <label>Password</label>\n" +
                            "    <input type=\"password\" id=\"certPassword\" placeholder=\"Enter certificate password\">\n" +
                            "</div>\n\n" +
                            "\n" +
                            "    <div class=\"modal-actions\">\n" +
                            "        <button class=\"modal-btn cancel\" onclick=\"closeSettings()\">Cancel</button>\n" +
                            "        <button class=\"modal-btn save\" onclick=\"uploadCertificate()\">Save</button>\n" +
//                            " <button class=\"modal-btn\" style=\"background:#f39c12;color:white;\" onclick=\"validateCertificate()\">Validate</button>"+
                            "    </div>\n" +
                            "</div>\n\n" +
                            "</div>\n"+
                            "</body>\n" +
                            "</html>";


            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
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
