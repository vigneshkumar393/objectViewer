package com.mayvel.objectViewer.controller;

import com.mayvel.objectViewer.BRestApiServerService;
import com.tridium.json.JSONObject;

import javax.baja.naming.BOrd;
import javax.baja.sys.BComponent;
import java.util.*;

public class AuthController {

    // Simple in-memory token store
    private final Map<String, TokenInfo> tokenStore = new HashMap<>();

    // Login method
    public JSONObject login(BComponent serviceComp, String username, String password) {
        JSONObject response = new JSONObject();

        String[] paths = getAllHSNetworkPaths();
        String path = paths.length > 0 ? paths[0] : "";

        BOrd ord = BOrd.make(path);
        BComponent comp = (BComponent) ord.resolve().get();

        String savedUsername = "";
        String savedPassword = "";
        if (comp instanceof BRestApiServerService) {
            savedUsername = ((BRestApiServerService) comp).getUsername();
            savedPassword = ((BRestApiServerService) comp).getPassword();
        }

        if (username == null || password == null) {
            response.put("status", "error");
            response.put("message", "Username and password required");
            return response;
        }

        if (!username.equals(savedUsername) || !password.equals(savedPassword)) {
            response.put("status", "error");
            response.put("message", "Invalid credentials");
            return response;
        }

        // ===== Generate token =====
        long timestamp = System.currentTimeMillis();
        long expiresInMillis = 12 * 60 * 60 * 1000; // 12 HOURS = 43,200 seconds

        String rawToken = username + ":" + timestamp;
        String token = Base64.getEncoder().encodeToString(rawToken.getBytes());

        // Save token with expiry time
        TokenInfo info = new TokenInfo(username, timestamp + expiresInMillis);
        tokenStore.put(token, info);

        response.put("status", "success");
        response.put("token", token);
        response.put("expiresIn", 43200); // 12 hours
        return response;
    }
    public class TokenInfo {
        public String username;
        public long expiry;

        public TokenInfo(String username, long expiry) {
            this.username = username;
            this.expiry = expiry;
        }
    }

    // Validate token
    public boolean validateToken(String token) {
        if (token == null || !tokenStore.containsKey(token)) {
            return false;
        }

        TokenInfo info = tokenStore.get(token);

        // Check expiry time
        if (System.currentTimeMillis() > info.expiry) {
            tokenStore.remove(token); // cleanup expired token
            return false;
        }

        return true;
    }

    // Optional: remove expired tokens (not required for simple demo)
    public void invalidateToken(String token) {
        tokenStore.remove(token);
    }

    public static String[] getAllHSNetworkPaths() {
        try {
            // Start from the root of the station
            BOrd ord = BOrd.make("station:|slot:/");
            BComponent root = (BComponent) ord.resolve().get();

            // List to hold all matching paths
            List<String> paths = new ArrayList<>();

            // Recursively collect all paths
            collectHSNPaths(root, paths);

            return paths.toArray(new String[0]);
        } catch (Exception e) {
            e.printStackTrace();
            return new String[0];
        }
    }

    private static void collectHSNPaths(BComponent parent, List<String> paths) {
        try {
            if (parent instanceof BRestApiServerService) {
                paths.add("station:|"+parent.getSlotPath().toString());
            }

            BComponent[] children = parent.getChildComponents();
            for (BComponent child : children) {
                collectHSNPaths(child, paths);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
