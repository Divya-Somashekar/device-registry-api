package org.device.deviceregistryapi.device;

/**
 * Raised when an operation is not permitted because the device is in use.
 */
public class DeviceInUseException extends RuntimeException {

    public DeviceInUseException(String message) {
        super(message);
    }
}
