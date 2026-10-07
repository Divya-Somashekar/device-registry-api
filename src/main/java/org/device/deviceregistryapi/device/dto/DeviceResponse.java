package org.device.deviceregistryapi.device.dto;

import java.time.Instant;
import java.util.UUID;

import org.device.deviceregistryapi.device.Device;
import org.device.deviceregistryapi.device.DeviceState;

public record DeviceResponse(
        UUID id,
        String name,
        String brand,
        DeviceState state,
        Instant creationTime) {

    public static DeviceResponse from(Device device) {
        return new DeviceResponse(
                device.getId(),
                device.getName(),
                device.getBrand(),
                device.getState(),
                device.getCreationTime());
    }
}
