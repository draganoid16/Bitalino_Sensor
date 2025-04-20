package com.example.myapplication;

import android.content.Context;
import android.os.Parcelable;

/**
 * Common interface for all device integrations.
 */
public interface DeviceConnector {
    void initialize(Context ctx);

    /**
     * Connect to the device (e.g., by MAC or identifier).
     */
    void connect(String id) throws Exception;

    /**
     * Start data acquisition or service.
     */
    void start() throws Exception;

    /**
     * Stop data acquisition or service.
     */
    void stop() throws Exception;

    /**
     * Disconnect and cleanup.
     */
    void disconnect() throws Exception;
}
