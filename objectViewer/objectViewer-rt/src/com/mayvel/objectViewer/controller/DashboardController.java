package com.mayvel.objectViewer.controller;

import com.mayvel.objectViewer.BRestApiServerService;
import com.mayvel.objectViewer.utils.Generic;
import com.mayvel.objectViewer.utils.Logger;
import com.tridium.json.JSONArray;
import com.tridium.json.JSONObject;
import javax.baja.control.BBooleanWritable;
import javax.baja.control.BNumericWritable;
import javax.baja.history.*;
import javax.baja.history.db.BHistoryDatabase;
import javax.baja.history.db.HistoryDatabaseConnection;
import javax.baja.naming.BOrd;
import javax.baja.status.BStatusBoolean;
import javax.baja.status.BStatusNumeric;
import javax.baja.sys.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DashboardController  {

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

    public static Map<String, Object> GetAllHistoryFromDB(String StartTime, String EndTime, String limit, String offset, String historySource, String filterValues, boolean firstAndLastOnly) {
        Map<String, Object> responseMap = new HashMap<>();
        Map<String, List<Map<String, String>>> historyData = new LinkedHashMap<>();
        int totalFiltered = 0;
        int totalScanned = 0;

        try {
            BHistoryService historyService = (BHistoryService) Sys.getService(BHistoryService.TYPE);
            if (historyService == null) {
                responseMap.put("error", "History service not available");
                return responseMap;
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yy hh:mm:ss a");
            LocalDateTime endDateTime = LocalDateTime.parse(EndTime, formatter);

            String[] sourceNames = historySource.split(",");
            for (String name : sourceNames) {
                name = name.trim();
                String path = getHistoryPathByName(name);
                if (path == null) {
                    Logger.Log("❌ History not found: " + name);
                    continue;
                }

                BOrd ord = BOrd.make("history:" + path);  // Ensure prefix `history:` is added
                BIHistory history = (BIHistory) ord.resolve().get();
                BHistoryConfig config = history.getConfig();

                HistoryDatabaseConnection conn = historyService.getDatabase().getDbConnection(null);
                Cursor<BHistoryRecord> cursor = conn.scan(history);
                List<Map<String, String>> records = new ArrayList<>();
                List<Map<String, String>> tempFiltered = new ArrayList<>();

                while (cursor.next()) {
                    try {
                        totalScanned++;
                        BHistoryRecord record = cursor.get();
                        String formattedTimeStamp = Generic.formatTimeStamp(record.getTimestamp());
                        LocalDateTime recordTime = LocalDateTime.parse(formattedTimeStamp, formatter);

                        if ((recordTime.isBefore(endDateTime) || recordTime.equals(endDateTime))) {
                            totalFiltered++;
                            Map<String, String> recordMap = convertToSyncallMap(record, filterValues);
                            if (firstAndLastOnly) {
                                tempFiltered.add(recordMap);
                            } else {
                                records.add(recordMap);
                            }
                        }
                    } catch (Exception e) {
                        Logger.Error("Error reading record: " + e.getMessage());
                    }
                }

                if (firstAndLastOnly && !tempFiltered.isEmpty()) {
                    records.add(tempFiltered.get(0));
                    if (tempFiltered.size() > 1) {
                        records.add(tempFiltered.get(tempFiltered.size() - 1));
                    }
                }

                conn.close();
                historyData.put(name, records);
            }

        } catch (Exception e) {
            responseMap.put("error", e.getMessage());
            return responseMap;
        }

        responseMap.put("filterValues", filterValues);
        responseMap.put("totalDatabaseRecordCount", totalScanned);
        responseMap.put("filteredRecordCount", totalFiltered);
        responseMap.put("histories", historyData);
        return responseMap;
    }

    public static Map<String, String> convertToSyncallMap(BHistoryRecord record, String filterValues) {
        Map<String, String> mapData = new HashMap<>();

        // Convert comma-separated filterValues to a Set
        Set<String> filterKeys = new HashSet<>();
        if (filterValues != null && !filterValues.trim().isEmpty()) {
            for (String key : filterValues.split(",")) {
                filterKeys.add(key.trim());
            }
        }

        // Parsing alarm data
        BHistorySchema schema = record.getSchema();
        String schemaStr = schema.toString();
        // Split the schema to get field names
        String[] fields = schemaStr.split(";");
        for (String field : fields) {
            String[] keyValue = field.split(",", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String type = keyValue[1].trim();

                // Filter logic
                if (!filterKeys.isEmpty() && !filterKeys.contains(key)) {
                    continue; // skip fields not in the filter
                }

                try {
                    BValue value = record.get(key);
                    if (value != null) {
                        mapData.put(key, value.toString());
                        Logger.Log("10 Field: " + key + " => Type: " + type + " => Value: " + value.toString());
                    } else {
                        mapData.put(key, "null");
                        Logger.Log("10 Field: " + key + " => Type: " + type + " => Value: null");
                    }
                } catch (Exception e) {
                    Logger.Log("Error reading value for field '" + key + "': " + e.getMessage());
                }
            } else {
                Logger.Log("Malformed schema field: " + field);
            }
        }
        return mapData;
    }


    public static String getHistoryPathByName(String name) {
        String[] allPaths = getAllHistoryCount(); // This returns paths like "/NetCool/..."

        for (String path : allPaths) {
            if (path != null && path.endsWith("/" + name)) {
                return "history:"+path;
            }
        }
        return null; // or throw an exception if preferred
    }

    public static String[] getAllHistoryCount() {
        try {
            BHistoryService historyService = (BHistoryService) Sys.getService(BHistoryService.TYPE);
            if (historyService == null) {
                return new String[0];
            }

            BHistoryDatabase db = historyService.getDatabase();
            if (db == null) {
                return new String[0];
            }

            BIHistory[] histories = db.getHistories();
            if (histories == null || histories.length == 0) {
                Logger.Log("ℹ️ No histories found in the station.");
                return new String[0];
            }

            String[] historyNames = new String[histories.length];

            for (int i = 0; i < histories.length; i++) {
                try {
                    BHistoryConfig config = histories[i].getConfig();
                    historyNames[i] = config.getId().toString(); // Store name in array
                } catch (Exception e) {
                    Logger.Log("⚠️ Error reading a history config: " + e.getMessage());
                    historyNames[i] = "error";
                }
            }

            return historyNames;

        } catch (Exception e) {
            Logger.Error("❌ Error fetching history names: " + e.getMessage());
            return new String[0];
        }
    }
    public static Map<String, Object> GetAllHistory(String StartTime, String EndTime, String limit, String offset, String historySource, String filterValues, boolean firstAndLastOnly) {
        Map<String, Object> responseMap = new HashMap<>();
        Map<String, List<Map<String, String>>> historyRecordsMap = new LinkedHashMap<>();
        int totalRecords = 0;
        int totalScanned = 0;

        try {
            BHistoryService historyService = (BHistoryService) Sys.getService(BHistoryService.TYPE);
            if (historyService == null) {
                Logger.Error("History service not available");
                responseMap.put("error", "History service not available");
                return responseMap;
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yy hh:mm:ss a");
            LocalDateTime startDateTime = LocalDateTime.parse(StartTime, formatter);
            LocalDateTime endDateTime = LocalDateTime.parse(EndTime, formatter);

            String[] historyNames = historySource.split(",");
            for (String historyName : historyNames) {
                historyName = historyName.trim();
                String path = getHistoryPathByName(historyName);
                if (path == null) {
                    Logger.Log("History not found for: " + historyName);
                    continue;
                }

                BOrd ord = BOrd.make(path);
                BIHistory history = (BIHistory) ord.resolve().get();
                BHistoryConfig config = history.getConfig();
                BHistoryDatabase db = historyService.getDatabase();
                db.setConfig(config);
                BHistoryId historyId = config.getId();
                db.getSystemTable(String.valueOf(historyId));

                BIHistory[] tablesRecord = db.getHistories();
                for (BIHistory hist : tablesRecord) {
                    if (hist.getId().toString().equals(extractPath(path))) {
                        HistoryDatabaseConnection conn = db.getDbConnection(null);
                        Cursor<BHistoryRecord> cursor = conn.scan(hist);

                        List<Map<String, String>> resultList = new ArrayList<>();
                        List<Map<String, String>> tempFiltered = new ArrayList<>();

                        while (cursor.next()) {
                            try {
                                totalScanned++;
                                BHistoryRecord record = cursor.get();
                                String formattedTime = Generic.formatTimeStamp(record.getTimestamp());
                                LocalDateTime recordTime = LocalDateTime.parse(formattedTime, formatter);

                                if ((recordTime.isAfter(startDateTime) || recordTime.equals(startDateTime)) &&
                                        (recordTime.isBefore(endDateTime) || recordTime.equals(endDateTime))) {

                                    totalRecords++;
                                    Map<String, String> recordMap = convertToSyncallMap(record, filterValues);
                                    if (firstAndLastOnly) {
                                        tempFiltered.add(recordMap);
                                    } else {
                                        resultList.add(recordMap);
                                    }
                                }
                            } catch (Exception e) {
                                Logger.Error("Record parse error: " + e.getMessage());
                            }
                        }

                        if (firstAndLastOnly && !tempFiltered.isEmpty()) {
                            resultList.add(tempFiltered.get(0));
                            if (tempFiltered.size() > 1) {
                                resultList.add(tempFiltered.get(tempFiltered.size() - 1));
                            }
                        }

                        conn.close();
                        historyRecordsMap.put(historyName, resultList);
                    }
                }
            }
        } catch (Exception e) {
            Logger.Error("Error in GetAllHistory: " + e.getMessage());
            responseMap.put("error", e.getMessage());
            return responseMap;
        }

        responseMap.put("filteredRecordCount", totalRecords);
        responseMap.put("totalDatabaseRecordCount", totalScanned);
        responseMap.put("filterValues", filterValues);
        responseMap.put("histories", historyRecordsMap);
        return responseMap;
    }

    private static String extractPath(String input) {
        if (input == null || !input.contains(":")) {
            return input; // or return null;
        }

        // Split the string by colon
        String[] parts = input.split(":", 2);
        return parts[1]; // This will be "/NetCool/TestPoint01"
    }

}
