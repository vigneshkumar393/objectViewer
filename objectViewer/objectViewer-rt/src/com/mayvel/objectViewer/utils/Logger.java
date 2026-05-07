package com.mayvel.objectViewer.utils;

public class Logger {
    static final String DEFAULTCOLOR = "";
    static final String LOGCOLOR = "LOG : ";
    static final String WARNINGCOLOR = "WARN : ";
    static final String ERRORCOLOR = "ERROR : ";

    public static void Log(String message)
    {
        Write(LOGCOLOR + message + DEFAULTCOLOR);
    }

    public static void Error(String message)
    {
        Write(ERRORCOLOR + message + DEFAULTCOLOR);
    }

    public static void Warn(String message)
    {
        Write(WARNINGCOLOR + message + DEFAULTCOLOR);
    }

    private static void Write(String message)
    {
        System.out.println(message);
    }
}
