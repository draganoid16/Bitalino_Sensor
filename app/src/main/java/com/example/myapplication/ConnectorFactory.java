package com.example.myapplication;

import com.example.myapplication.DeviceConnector;
import com.example.myapplication.BitalinoConnector;
import com.example.myapplication.ScientistConnector;
/**
 * Factory to instantiate the correct connector based on device type.
 */
public class ConnectorFactory {
    public enum DeviceType { BITALINO, SCIENTIST }
    public static DeviceConnector create(DeviceType type) {
        switch(type) {
            case BITALINO:    return new BitalinoConnector();
            case SCIENTIST:   return new ScientistConnector();
            default: throw new IllegalArgumentException("Unknown type");
        }
    }
}