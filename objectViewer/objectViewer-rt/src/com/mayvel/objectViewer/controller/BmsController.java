package com.mayvel.objectViewer.controller;

import com.mayvel.objectViewer.BRestApiServerService;
import com.tridium.json.JSONObject;
import com.tridium.json.JSONArray;

import javax.baja.control.BBooleanWritable;
import javax.baja.control.BNumericWritable;
import javax.baja.naming.BOrd;
import javax.baja.status.*;
import javax.baja.sys.*;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.*;

public class BmsController {

    // ================================================================
    // GET /getData
    // ================================================================
    public JSONObject getAllPoints(BComponent parent, String isSavedParam) {
        JSONObject response = new JSONObject();
        JSONArray pointsArray = new JSONArray();

        // 1️⃣ Collect all points (no filter)
        collectPoints(parent, pointsArray);

        // 2️⃣ Apply filter if isSavedParam is provided
        if (isSavedParam != null) {
            boolean filterSaved = "true".equalsIgnoreCase(isSavedParam);
            boolean filterUnsaved = "false".equalsIgnoreCase(isSavedParam);

            JSONArray filteredArray = new JSONArray();
            for (int i = 0; i < pointsArray.length(); i++) {
                JSONObject point = pointsArray.getJSONObject(i);
                boolean isSaved = point.optBoolean("isSaved", false);

                if ((filterSaved && isSaved) || (filterUnsaved && !isSaved)) {
                    filteredArray.put(point);
                }
            }
            pointsArray = filteredArray; // replace original array with filtered one
        }

        // 3️⃣ Build response
        response.put("status", "success");
        response.put("points", pointsArray);
        response.put("timestamp", BAbsTime.now().toString());

        return response;
    }

    private void collectPoints(BComponent parent, JSONArray pointsArray) {

        String[] savedPoints = getAllSavedPoints();   // list of saved point names

            for (BComponent comp : parent.getChildComponents()) {

                // ****************************************
                // Helper: check if this point is saved
                // ****************************************
                String pointName = comp.getName();
                boolean isSaved = Arrays.asList(savedPoints).contains(pointName);

            // ============ NUMERIC WRITABLE ============
            if (comp instanceof BNumericWritable) {
                BNumericWritable num = (BNumericWritable) comp;
                JSONObject o = new JSONObject();
                o.put("pointName", num.getName());
                o.put("type", "NumericWritable");
                o.put("isSaved", isSaved);     // <-- ADD THIS


                Double val = num.getOutStatusValue().isNull()
                        ? null
                        : num.getOut().getValue();

                o.put("value", val == null ? null : Math.round(val));

                JSONObject pri = new JSONObject();
                for (int i = 1; i <= 16; i++) {
                    BStatusNumeric pVal = getNumericPriorityValue(num, i);
                    if (pVal == null || pVal.isNull() || !pVal.getStatus().isOk())
                        pri.put("In" + i, JSONObject.NULL);
                    else
                        pri.put("In" + i, Math.round(pVal.getValue()));
                }
                o.put("priorityArray", pri);
                pointsArray.put(o);
            }

            // ============ BOOLEAN WRITABLE ============
            else if (comp instanceof BBooleanWritable) {
                BBooleanWritable bool = (BBooleanWritable) comp;
                JSONObject o = new JSONObject();
                o.put("pointName", bool.getName());
                o.put("type", "BooleanWritable");
                o.put("isSaved", isSaved);      // <-- ADD THIS

                Boolean val = bool.getOutStatusValue().isNull()
                        ? null
                        : bool.getOut().getValue();

                o.put("value", val);

                JSONObject pri = new JSONObject();
                for (int i = 1; i <= 16; i++) {
                    BStatusBoolean pVal = getBooleanPriorityValue(bool, i);
                    if (pVal == null || pVal.isNull() || !pVal.getStatus().isOk())
                        pri.put("In" + i, JSONObject.NULL);
                    else
                        pri.put("In" + i, pVal.getValue());
                }
                o.put("priorityArray", pri);
                pointsArray.put(o);
            }

            // RECURSE
            if (comp.getChildComponents().length > 0)
                collectPoints(comp, pointsArray);
        }
    }

    private String[] getAllSavedPoints() {
        String savedPoints = "";   // FIXED (valid Java)

        try {
            // Get all HS Network paths
            String[] paths = getAllHSNetworkPaths();

            if (paths.length == 0) {
                return new String[0]; // nothing found
            }

            // Resolve HTTP server service
            BOrd ord = BOrd.make(paths[0]);
            BComponent comp = (BComponent) ord.resolve().get();

            if (comp instanceof BRestApiServerService) {
                savedPoints = ((BRestApiServerService) comp).getSavedPoints();
            }

        } catch (Exception e) {
            System.out.println("Error: " + e);
        }

        // 🔥 If no points saved → return empty array
        if (savedPoints == null || savedPoints.trim().isEmpty()) {
            return new String[0];
        }

        // 🔥 Split CSV into array
        return savedPoints.split(",");
    }

    // ================================================================
    // POST /updatePoint
    // ================================================================
    public JSONObject updatePoint(HttpExchange exchange, BComponent parent) throws IOException {

        String body = new Scanner(exchange.getRequestBody()).useDelimiter("\\A").next();
        JSONObject req = new JSONObject(body);

        String pointName = req.optString("pointName", null);
        Object value = req.opt("value");
        int priority = req.optInt("priority", 8);

        JSONObject response = new JSONObject();

        if (pointName == null) {
            response.put("status", "error");
            response.put("message", "Missing 'pointName'");
            return response;
        }

        BComponent point = findPointByName(parent, pointName);
        if (point == null) {
            response.put("status", "error");
            response.put("message", "Point not found: " + pointName);
            return response;
        }

        try {
            // NUMERIC
            if (point instanceof BNumericWritable) {
                Double val = null;
                if (!(value == null || value.equals(JSONObject.NULL)))
                    val = ((Number) value).doubleValue();

                setNumericByPriority((BNumericWritable) point, val, priority);
            }

            // BOOLEAN
            else if (point instanceof BBooleanWritable) {
                Boolean val = null;
                if (!(value == null || value.equals(JSONObject.NULL)))
                    val = Boolean.parseBoolean(value.toString());

                setBooleanByPriority((BBooleanWritable) point, val, priority);
            }

            else {
                response.put("status", "error");
                response.put("message", "Unsupported point type");
                return response;
            }

            response.put("status", "success");
            response.put("pointName", pointName);
            response.put("value", value);
            response.put("priority", priority);
        }
        catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    // ================================================================
    // POST /api/start
    // ================================================================
    public JSONObject startMeeting(HttpExchange exchange, BComponent parent) throws IOException {

        String body = new Scanner(exchange.getRequestBody()).useDelimiter("\\A").next();
        JSONObject req = new JSONObject(body);

        String pointName = req.optString("pointName", null);
        int priority = req.optInt("priority", 8);

        JSONObject response = new JSONObject();

        if (pointName == null) {
            response.put("status", "error");
            response.put("message", "Missing pointName");
            return response;
        }

        List<BBooleanWritable> bools = new ArrayList<>();
        List<BNumericWritable> nums = new ArrayList<>();
        collectRoomPoints(parent, pointName, bools, nums);

        if (bools.isEmpty() && nums.isEmpty()) {
            response.put("status", "error");
            response.put("message", "No room devices found for: " + pointName);
            return response;
        }

        // Turn ON boolean devices
        for (BBooleanWritable b : bools)
            setBooleanByPriority(b, true, priority);

        // Turn ON numeric devices (default = 1)
        for (BNumericWritable n : nums)
            setNumericByPriority(n, 1.0, priority);

        response.put("status", "success");
        response.put("message", "Room devices turned ON using priority " + priority);
        response.put("timestamp", BAbsTime.now().toString());

        return response;
    }

    // ================================================================
    // POST /api/end
    // ================================================================
    public JSONObject endMeeting(HttpExchange exchange, BComponent parent) throws IOException {

        String body = new Scanner(exchange.getRequestBody()).useDelimiter("\\A").next();
        JSONObject req = new JSONObject(body);

        String pointName = req.optString("pointName", null);
        int priority = req.optInt("priority", 8);

        JSONObject response = new JSONObject();

        if (pointName == null) {
            response.put("status", "error");
            response.put("message", "Missing pointName");
            return response;
        }

        List<BBooleanWritable> bools = new ArrayList<>();
        List<BNumericWritable> nums = new ArrayList<>();
        collectRoomPoints(parent, pointName, bools, nums);

        if (bools.isEmpty() && nums.isEmpty()) {
            response.put("status", "error");
            response.put("message", "No room devices found for: " + pointName);
            return response;
        }

        // Reset priority
        for (BBooleanWritable b : bools)
            resetBooleanByPriority(b, priority);

        for (BNumericWritable n : nums)
            resetNumericByPriority(n, priority);

        response.put("status", "success");
        response.put("message", "Room devices turned OFF successfully at priority " + priority);
        response.put("timestamp", BAbsTime.now().toString());

        return response;
    }

    public JSONObject getRoomStatus(HttpExchange exchange, BComponent parent) throws IOException {

        String body = new Scanner(exchange.getRequestBody()).useDelimiter("\\A").next();
        JSONObject req = new JSONObject(body);

        String pointName = req.optString("pointName", null);

        JSONObject response = new JSONObject();

        if (pointName == null) {
            response.put("status", "error");
            response.put("message", "Missing pointName");
            return response;
        }

        // Points array (required by you)
        JSONArray pointsArr = new JSONArray();

        // Find room-specific points
        collectRoomStatus(parent, pointName, pointsArr);

        response.put("status", "success");
        response.put("pointName", pointName);
        response.put("points", pointsArr);
        response.put("timestamp", BAbsTime.now().toString());

        return response;
    }

    // ================================================================
    // Helper: Collect points by room
    // ================================================================

    private void collectRoomStatus(BComponent parent, String pointName, JSONArray arr) {

        for (BComponent comp : parent.getChildComponents()) {

            String name = comp.getName();

            if (name.startsWith(pointName)) {

                JSONObject obj = new JSONObject();
                obj.put("pointName", pointName);

                // Boolean
                if (comp instanceof BBooleanWritable) {
                    BBooleanWritable b = (BBooleanWritable) comp;

                    boolean v = b.getOut().getValue();
                    obj.put("value", v ? "ON" : "OFF");
                    obj.put("type", "BooleanWritable");
                }

                // Numeric
                else if (comp instanceof BNumericWritable) {
                    BNumericWritable n = (BNumericWritable) comp;

                    double v = n.getOut().getValue();
                    obj.put("value", Math.round(v));  // rounded numeric
                    obj.put("type", "NumericWritable");
                }

                arr.put(obj);
            }

            // Recurse
            if (comp.getChildComponents().length > 0)
                collectRoomStatus(comp, pointName, arr);
        }
    }

    private void collectRoomPoints(BComponent parent, String pointName,
                                   List<BBooleanWritable> bools,
                                   List<BNumericWritable> nums) {

        for (BComponent comp : parent.getChildComponents()) {

            String name = comp.getName();

            if (name.startsWith(pointName)) {

                if (comp instanceof BBooleanWritable)
                    bools.add((BBooleanWritable) comp);

                if (comp instanceof BNumericWritable)
                    nums.add((BNumericWritable) comp);
            }

            if (comp.getChildComponents().length > 0)
                collectRoomPoints(comp, pointName, bools, nums);
        }
    }

    private BComponent findPointByName(BComponent parent, String pointName) {
        for (BComponent comp : parent.getChildComponents()) {
            if (pointName.equals(comp.getName())
                    && (comp instanceof BBooleanWritable || comp instanceof BNumericWritable))
                return comp;

            if (comp.getChildComponents().length > 0) {
                BComponent found = findPointByName(comp, pointName);
                if (found != null) return found;
            }
        }
        return null;
    }

    // ================================================================
    // Priority Write Helpers
    // ================================================================
    private void setNumericByPriority(BNumericWritable num, Double value, int priority) {
        BStatusNumeric val = (value == null)
                ? new BStatusNumeric(Double.NaN, BStatus.nullStatus)
                : new BStatusNumeric(value, BStatus.ok);

        switch (priority) {
            case 1: num.setIn1(val); break;
            case 2: num.setIn2(val); break;
            case 3: num.setIn3(val); break;
            case 4: num.setIn4(val); break;
            case 5: num.setIn5(val); break;
            case 6: num.setIn6(val); break;
            case 7: num.setIn7(val); break;
            case 8: num.setIn8(val); break;
            case 9: num.setIn9(val); break;
            case 10: num.setIn10(val); break;
            case 11: num.setIn11(val); break;
            case 12: num.setIn12(val); break;
            case 13: num.setIn13(val); break;
            case 14: num.setIn14(val); break;
            case 15: num.setIn15(val); break;
            case 16: num.setIn16(val); break;
        }
    }

    private void resetNumericByPriority(BNumericWritable num, int priority) {
        switch (priority) {
            case 1: num.setIn1(null); break;
            case 2: num.setIn2(null); break;
            case 3: num.setIn3(null); break;
            case 4: num.setIn4(null); break;
            case 5: num.setIn5(null); break;
            case 6: num.setIn6(null); break;
            case 7: num.setIn7(null); break;
            case 8: num.setIn8(null); break;
            case 9: num.setIn9(null); break;
            case 10: num.setIn10(null); break;
            case 11: num.setIn11(null); break;
            case 12: num.setIn12(null); break;
            case 13: num.setIn13(null); break;
            case 14: num.setIn14(null); break;
            case 15: num.setIn15(null); break;
            case 16: num.setIn16(null); break;
        }
    }

    private void setBooleanByPriority(BBooleanWritable bool, Boolean value, int priority) {

        BStatusBoolean val = (value == null)
                ? new BStatusBoolean(false, BStatus.nullStatus)
                : new BStatusBoolean(value, BStatus.ok);

        switch (priority) {
            case 1: bool.setIn1(val); break;
            case 2: bool.setIn2(val); break;
            case 3: bool.setIn3(val); break;
            case 4: bool.setIn4(val); break;
            case 5: bool.setIn5(val); break;
            case 6: bool.setIn6(val); break;
            case 7: bool.setIn7(val); break;
            case 8: bool.setIn8(val); break;
            case 9: bool.setIn9(val); break;
            case 10: bool.setIn10(val); break;
            case 11: bool.setIn11(val); break;
            case 12: bool.setIn12(val); break;
            case 13: bool.setIn13(val); break;
            case 14: bool.setIn14(val); break;
            case 15: bool.setIn15(val); break;
            case 16: bool.setIn16(val); break;
        }
    }

    private void resetBooleanByPriority(BBooleanWritable bool, int priority) {
        BStatusBoolean val =  new BStatusBoolean(false, BStatus.nullStatus);
        switch (priority) {
            case 1: bool.setIn1(val); break;
            case 2: bool.setIn2(val); break;
            case 3: bool.setIn3(val); break;
            case 4: bool.setIn4(val); break;
            case 5: bool.setIn5(val); break;
            case 6: bool.setIn6(val); break;
            case 7: bool.setIn7(val); break;
            case 8: bool.setIn8(val); break;
            case 9: bool.setIn9(val); break;
            case 10: bool.setIn10(val); break;
            case 11: bool.setIn11(val); break;
            case 12: bool.setIn12(val); break;
            case 13: bool.setIn13(val); break;
            case 14: bool.setIn14(val); break;
            case 15: bool.setIn15(val); break;
            case 16: bool.setIn16(val); break;
        }
    }

    // ================================================================
    // Fetch individual priority slot
    // ================================================================
    private BStatusNumeric getNumericPriorityValue(BNumericWritable num, int priority) {
        switch (priority) {
            case 1: return num.getIn1();
            case 2: return num.getIn2();
            case 3: return num.getIn3();
            case 4: return num.getIn4();
            case 5: return num.getIn5();
            case 6: return num.getIn6();
            case 7: return num.getIn7();
            case 8: return num.getIn8();
            case 9: return num.getIn9();
            case 10: return num.getIn10();
            case 11: return num.getIn11();
            case 12: return num.getIn12();
            case 13: return num.getIn13();
            case 14: return num.getIn14();
            case 15: return num.getIn15();
            case 16: return num.getIn16();
        }
        return null;
    }

    private BStatusBoolean getBooleanPriorityValue(BBooleanWritable bool, int priority) {
        switch (priority) {
            case 1: return bool.getIn1();
            case 2: return bool.getIn2();
            case 3: return bool.getIn3();
            case 4: return bool.getIn4();
            case 5: return bool.getIn5();
            case 6: return bool.getIn6();
            case 7: return bool.getIn7();
            case 8: return bool.getIn8();
            case 9: return bool.getIn9();
            case 10: return bool.getIn10();
            case 11: return bool.getIn11();
            case 12: return bool.getIn12();
            case 13: return bool.getIn13();
            case 14: return bool.getIn14();
            case 15: return bool.getIn15();
            case 16: return bool.getIn16();
        }
        return null;
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
