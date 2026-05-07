package com.mayvel.objectViewer.route;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;

public class LoginRoute {

    public static void registerRoutes(HttpServer server) {
        server.createContext("/", new HomeHandler());
    }

    static class HomeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {

            String html =
                    "<!DOCTYPE html>" +
                            "<!DOCTYPE html>\n" +
                            "<html lang=\"en\">\n" +
                            "<head>\n" +
                            "    <meta charset=\"UTF-8\">\n" +
                            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                            "    <title>Login</title>\n" +
                            "    <style>\n" +
                            "        body { font-family: Arial, sans-serif; background:#f0f2f5; display:flex; justify-content:center; align-items:center; height:100vh; margin:0; }\n" +
                            "        .login-card { background:white; padding:40px; border-radius:10px; box-shadow:0 4px 20px rgba(0,0,0,0.1); width:300px; }\n" +
                            "        .login-card h2 { text-align:center; margin-bottom:20px; }\n" +
                            "        .login-card input { width:100%; padding:10px; margin:10px 0; border:1px solid #ccc; border-radius:5px; box-sizing:border-box; }\n" +
                            "        .login-card button { width:100%; padding:10px; background:#4CAF50; color:white; border:none; border-radius:5px; cursor:pointer; }\n" +
                            "        .login-card button:hover { background:#45a049; }\n" +
                            "        .message { text-align:center; margin-top:10px; font-size:0.9em; }\n" +
                            "    </style>\n" +
                            "</head>\n" +
                            "<body>\n" +
                            "<div class=\"login-card\">\n" +
                            "    <h2>Login</h2>\n" +
                            "    <input type=\"text\" id=\"username\" placeholder=\"Username\" required>\n" +
                            "    <input type=\"password\" id=\"password\" placeholder=\"Password\" required>\n" +
                            "    <button onclick=\"login()\">Login</button>\n" +
                            "    <div class=\"message\" id=\"message\"></div>\n" +
                            "</div>\n" +
                            "\n" +
                            "<script>\n" +
                            "function isValidCredential(value) {\n" +
                            "    if (!value || value.length < 6 || value.length > 20) return false;\n" +
                            "    let hasLetter = false, hasDigit = false, hasSymbol = false;\n" +
                            "    for (const c of value) {\n" +
                            "        if (/[a-zA-Z]/.test(c)) hasLetter = true;\n" +
                            "        else if (/[0-9]/.test(c)) hasDigit = true;\n" +
                            "        else hasSymbol = true;\n" +
                            "    }\n" +
                            "    return hasLetter && hasDigit && hasSymbol;\n" +
                            "}\n" +
                            "\n" +
                            "async function login() {\n" +
                            "    const username = document.getElementById('username').value;\n" +
                            "    const password = document.getElementById('password').value;\n" +
                            "    const messageEl = document.getElementById('message');\n" +
                            "    messageEl.textContent = '';\n" +
                            "\n" +
                            "    // Client-side validation\n" +
                            "    if (!isValidCredential(username)) {\n" +
                            "        messageEl.style.color = 'red';\n" +
                            "        messageEl.textContent = 'Invalid username! Must contain letters, numbers, and symbol, 6-20 chars';\n" +
                            "        return;\n" +
                            "    }\n" +
                            "    if (!isValidCredential(password)) {\n" +
                            "        messageEl.style.color = 'red';\n" +
                            "        messageEl.textContent = 'Invalid password! Must contain letters, numbers, and symbol, 6-20 chars';\n" +
                            "        return;\n" +
                            "    }\n" +
                            "\n" +
                            "    try {\n" +
                            "        const res = await fetch('/login', {\n" +
                            "            method: 'POST',\n" +
                            "            headers: { 'Content-Type': 'application/json' },\n" +
                            "            body: JSON.stringify({ username, password })\n" +
                            "        });\n" +
                            "        const data = await res.json();\n" +
                            "        if (data.status === 'success') {\n" +
                            "            messageEl.style.color = 'green';\n" +
                            "            messageEl.textContent = 'Login successful! Redirecting...';\n" +
                            "\n" +
                            "            // Store token in localStorage for API calls\n" +
                            "            localStorage.setItem('token', data.token);\n" +
                            "\n" +
                            "            // Redirect to objectviewer page\n" +
                            "            setTimeout(() => window.location.href='/objectviewer', 1000);\n" +
                            "        } else {\n" +
                            "            messageEl.style.color = 'red';\n" +
                            "            messageEl.textContent = data.message || 'Login failed';\n" +
                            "        }\n" +
                            "    } catch(err) {\n" +
                            "        messageEl.style.color = 'red';\n" +
                            "        messageEl.textContent = 'Server error';\n" +
                            "    }\n" +
                            "}\n" +
                            "</script>\n" +
                            "</body>\n" +
                            "</html>\n";

            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, html.getBytes().length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(html.getBytes());
            }
        }
    }
}
