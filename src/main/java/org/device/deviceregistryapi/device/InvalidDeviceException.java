package org.device.deviceregistryapi.device;

/**
 * Raised when a device would be left in an invalid state, independently of the request model
 * that asked for it.
 */
public class InvalidDeviceException extends RuntimeException {

    public InvalidDeviceException(String message) {
        super(message);
    }
}
