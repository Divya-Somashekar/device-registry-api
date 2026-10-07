package org.device.deviceregistryapi.device.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.device.deviceregistryapi.device.DeviceState;

/**
 * Partial update (PATCH): an omitted or null property is left unchanged. No property is
 * nullable on the device itself, so null and absent are treated alike.
 *
 * <p>Blankness is checked with {@link Pattern} rather than {@code @NotBlank}: the latter
 * implies {@code @NotNull}, which would make every property mandatory and defeat the point of
 * a partial update. {@code @Pattern} skips nulls, so an omitted property stays optional while
 * a supplied one must still carry a visible character.
 */
public record PatchDeviceRequest(
        @Pattern(regexp = ".*\\S.*", flags = Pattern.Flag.DOTALL, message = "must not be blank")
        @Size(max = 120) String name,

        @Pattern(regexp = ".*\\S.*", flags = Pattern.Flag.DOTALL, message = "must not be blank")
        @Size(max = 120) String brand,

        DeviceState state) {
}
