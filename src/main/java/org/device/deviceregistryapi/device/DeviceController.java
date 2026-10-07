package org.device.deviceregistryapi.device;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.device.deviceregistryapi.common.PageResponse;
import org.device.deviceregistryapi.device.dto.CreateDeviceRequest;
import org.device.deviceregistryapi.device.dto.DeviceResponse;
import org.device.deviceregistryapi.device.dto.PatchDeviceRequest;
import org.device.deviceregistryapi.device.dto.ReplaceDeviceRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
public class DeviceController {

    private final DeviceService service;

    DeviceController(DeviceService service) {
        this.service = service;
    }

    @PostMapping
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
    public PageResponse<DeviceResponse> list(
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) DeviceState state,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return PageResponse.of(service.find(brand, state, pageable), DeviceResponse::from);
    }

    @GetMapping("/{id}")
    public DeviceResponse byId(@PathVariable UUID id) {
        return DeviceResponse.from(service.findById(id));
    }

    @PutMapping("/{id}")
    public DeviceResponse replace(
            @PathVariable UUID id, @Valid @RequestBody ReplaceDeviceRequest request) {
        return DeviceResponse.from(service.replace(id, request));
    }

    @PatchMapping("/{id}")
    public DeviceResponse patch(
            @PathVariable UUID id, @Valid @RequestBody PatchDeviceRequest request) {
        return DeviceResponse.from(service.patch(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
