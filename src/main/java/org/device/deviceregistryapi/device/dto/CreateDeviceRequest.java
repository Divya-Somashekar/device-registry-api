package org.device.deviceregistryapi.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.device.deviceregistryapi.device.DeviceState;

/**
 * @param state optional; a device with no state given is {@link DeviceState#AVAILABLE}
 */
public record CreateDeviceRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 120) String brand,
        DeviceState state) {

    public DeviceState stateOrDefault() {
        return state == null ? DeviceState.AVAILABLE : state;
    }
}
