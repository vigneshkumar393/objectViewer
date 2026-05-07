package com.mayvel.objectViewer.controller;

import com.mayvel.objectViewer.BRestApiServerService;
import com.tridium.json.JSONObject;

import javax.baja.naming.BOrd;
import javax.baja.sys.BComponent;
import javax.baja.sys.Sys;
import java.util.*;

public class ConfigurationController {

    // Save points method
    public JSONObject savePoints(String[] points) {
        JSONObject response = new JSONObject();

        // Find all HS Network service paths
        String[] paths = getAllHSNetworkPaths();
        if (paths.length == 0) {
            response.put("status", "error");
            response.put("message", "No BHttpServerService found in station");
            return response;
        }

        try {
            // Usually only one http server service exists so use paths[0]
            BOrd ord = BOrd.make(paths[0]);
            BComponent comp = (BComponent) ord.resolve().get();

            if (comp instanceof BRestApiServerService) {
                String joined = String.join(",", points);
                ((BRestApiServerService) comp).setSavedPoints(joined);
                // IMPORTANT → persist to station file
                Sys.getStation().save();
            }

            response.put("status", "success");
            response.put("savedCount", points.length);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
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
