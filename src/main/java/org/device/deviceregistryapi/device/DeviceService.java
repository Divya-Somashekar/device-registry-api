package org.device.deviceregistryapi.device;

import java.util.UUID;

import org.device.deviceregistryapi.common.DeviceInUseException;
import org.device.deviceregistryapi.common.DeviceNotFoundException;
import org.device.deviceregistryapi.device.dto.CreateDeviceRequest;
import org.device.deviceregistryapi.device.dto.PatchDeviceRequest;
import org.device.deviceregistryapi.device.dto.ReplaceDeviceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeviceService {

    private final DeviceRepository repository;

    DeviceService(DeviceRepository repository) {
        this.repository = repository;
    }

    public Device create(CreateDeviceRequest request) {
        return repository.save(
                new Device(request.name(), request.brand(), request.stateOrDefault()));
    }

    @Transactional(readOnly = true)
    public Device findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new DeviceNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<Device> find(String brand, DeviceState state, Pageable pageable) {
        if (brand != null && state != null) {
            return repository.findByBrandIgnoreCaseAndState(brand, state, pageable);
        }
        if (brand != null) {
            return repository.findByBrandIgnoreCase(brand, pageable);
        }
        if (state != null) {
            return repository.findByState(state, pageable);
        }
        return repository.findAll(pageable);
    }

    /**
     * Full replacement. The rename is applied before the state change, so the in-use rule is
     * evaluated against the state the device is stored with, not the one being requested.
     */
    public Device replace(UUID id, ReplaceDeviceRequest request) {
        Device device = findById(id);
        device.rename(request.name(), request.brand());
        device.changeState(request.state());
        return device;
    }

    /**
     * Partial update. Absent properties keep their current value; the ordering rule described
     * on {@link #replace} applies here too.
     */
    public Device patch(UUID id, PatchDeviceRequest request) {
        Device device = findById(id);
        device.rename(
                request.name() != null ? request.name() : device.getName(),
                request.brand() != null ? request.brand() : device.getBrand());
        if (request.state() != null) {
            device.changeState(request.state());
        }
        return device;
    }

    public void delete(UUID id) {
        Device device = findById(id);
        if (device.isInUse()) {
            throw new DeviceInUseException("Cannot delete device " + id + " while it is in use");
        }
        repository.delete(device);
    }
}
