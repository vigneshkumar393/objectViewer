package com.mayvel.objectViewer.utils;

import javax.baja.sys.BAbsTime;
import java.text.SimpleDateFormat;
import java.util.*;

public class Generic {

    public static String formatTimeStamp(BAbsTime alarmTimeStamp){
        SimpleDateFormat inputFormat = new SimpleDateFormat("dd-MMM-yy h:mm a", Locale.ENGLISH);
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd-MMM-yy hh:mm:ss a", Locale.ENGLISH);

        try{
            Date parsedDate = inputFormat.parse(alarmTimeStamp.toString());
            return outputFormat.format(parsedDate);
        }catch (Exception e){
            Logger.Error("Error parsing timestamp: "+e.getMessage());
            return "";
        }
    }

    public static void appendField(StringBuilder jsonString, String key, Object value) {
        jsonString.append("\"").append(key).append("\":");

        if (value instanceof String) {
            jsonString.append("\"").append(value).append("\"");
        } else {
            jsonString.append(value);
        }

        jsonString.append(",");
    }
}
