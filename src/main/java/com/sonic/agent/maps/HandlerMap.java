package com.sonic.agent.maps;

import com.sonic.agent.automation.AndroidStepHandler;
import com.sonic.agent.automation.IOSStepHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HandlerMap {
    private static Map<String, AndroidStepHandler> androidHandlerMap = new ConcurrentHashMap<String, AndroidStepHandler>();
    public static Map<String, AndroidStepHandler> getAndroidMap() {
        return androidHandlerMap;
    }

    private static Map<String, IOSStepHandler> iosHandlerMap = new ConcurrentHashMap<String, IOSStepHandler>();
    public static Map<String, IOSStepHandler> getIOSMap() {
        return iosHandlerMap;
    }
}
