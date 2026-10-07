package org.device.deviceregistryapi.device;

import java.util.UUID;

public class DeviceNotFoundException extends RuntimeException {

    public DeviceNotFoundException(UUID id) {
        super("No device found with id " + id);
    }
}
