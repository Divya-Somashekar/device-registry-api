package org.device.deviceregistryapi.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.device.deviceregistryapi.device.DeviceState;

/**
 * Full replacement (PUT): every mutable property must be supplied.
 */
public record ReplaceDeviceRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 120) String brand,
        @NotNull DeviceState state) {
}
