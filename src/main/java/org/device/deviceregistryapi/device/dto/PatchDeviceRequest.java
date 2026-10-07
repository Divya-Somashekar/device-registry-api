package org.device.deviceregistryapi.device.dto;

import jakarta.validation.constraints.Size;

import org.device.deviceregistryapi.device.DeviceState;

/**
 * Partial update (PATCH): an omitted or null property is left unchanged. No property is
 * nullable on the device itself, so null and absent are treated alike.
 */
public record PatchDeviceRequest(
        @Size(max = 120) String name,
        @Size(max = 120) String brand,
        DeviceState state) {
}
