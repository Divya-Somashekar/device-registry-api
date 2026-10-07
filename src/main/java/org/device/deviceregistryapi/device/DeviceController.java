package org.device.deviceregistryapi.device;

import java.net.URI;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.device.deviceregistryapi.common.PageResponse;
import org.device.deviceregistryapi.device.dto.CreateDeviceRequest;
import org.device.deviceregistryapi.device.dto.DeviceResponse;
import org.device.deviceregistryapi.device.dto.PatchDeviceRequest;
import org.device.deviceregistryapi.device.dto.ReplaceDeviceRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/devices")
@Tag(name = "Devices", description = "Create, query and manage device resources")
public class DeviceController {

    private final DeviceService service;

    DeviceController(DeviceService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create a device",
            description = "The state may be omitted, in which case the device is AVAILABLE.")
    @ApiResponse(responseCode = "201", description = "Device created")
    @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<DeviceResponse> create(@Valid @RequestBody CreateDeviceRequest request) {
        Device created = service.create(request);
        return ResponseEntity
                .created(URI.create("/api/v1/devices/" + created.getId()))
                .body(DeviceResponse.from(created));
    }

    /**
     * Brand and state are filters on the collection rather than separate endpoints: narrowing
     * a collection does not introduce a new resource.
     */
    @GetMapping
    @Operation(summary = "Fetch devices",
            description = "Returns a page of devices, optionally narrowed by brand, by state "
                    + "or by both. Brand matching is case-insensitive.")
    @ApiResponse(responseCode = "200", description = "A page of devices")
    @ApiResponse(responseCode = "400", description = "Unrecognised state value",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    public PageResponse<DeviceResponse> list(
            @Parameter(description = "Filter by brand, case-insensitive")
            @RequestParam(required = false) String brand,
            @Parameter(description = "Filter by state")
            @RequestParam(required = false) DeviceState state,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return PageResponse.of(service.find(brand, state, pageable), DeviceResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch a single device")
    @ApiResponse(responseCode = "200", description = "The device")
    @ApiResponse(responseCode = "404", description = "No device with that id",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    public DeviceResponse byId(@PathVariable UUID id) {
        return DeviceResponse.from(service.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Fully update a device",
            description = "Every mutable property must be supplied. The creation time is not "
                    + "accepted and is left unchanged.")
    @ApiResponse(responseCode = "200", description = "The updated device")
    @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "No device with that id",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409",
            description = "The name or brand would change while the device is in use, or the "
                    + "device was changed concurrently by another request",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    public DeviceResponse replace(
            @PathVariable UUID id, @Valid @RequestBody ReplaceDeviceRequest request) {
        return DeviceResponse.from(service.replace(id, request));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update a device",
            description = "Omitted properties keep their current value.")
    @ApiResponse(responseCode = "200", description = "The updated device")
    @ApiResponse(responseCode = "404", description = "No device with that id",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409",
            description = "The name or brand would change while the device is in use, or the "
                    + "device was changed concurrently by another request",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    public DeviceResponse patch(
            @PathVariable UUID id, @Valid @RequestBody PatchDeviceRequest request) {
        return DeviceResponse.from(service.patch(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a device")
    @ApiResponse(responseCode = "204", description = "Device deleted")
    @ApiResponse(responseCode = "404", description = "No device with that id",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409",
            description = "The device is in use, or was changed concurrently by another request",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
