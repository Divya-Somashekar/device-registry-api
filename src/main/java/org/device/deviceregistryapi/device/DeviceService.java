package org.device.deviceregistryapi.device;

import java.util.UUID;

import org.device.deviceregistryapi.device.dto.CreateDeviceRequest;
import org.device.deviceregistryapi.device.dto.PatchDeviceRequest;
import org.device.deviceregistryapi.device.dto.ReplaceDeviceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    /**
     * A blank brand is treated as no brand at all. {@code ?brand=} is how a form or a client
     * that clears a filter spells "unset", and matching devices whose brand is the empty
     * string would answer a question nobody asked: the column never holds one.
     */
    @Transactional(readOnly = true)
    public Page<Device> find(String brand, DeviceState state, Pageable request) {
        Pageable pageable = withStableOrder(request);
        String brandFilter = (brand == null || brand.isBlank()) ? null : brand.strip();
        if (brandFilter != null && state != null) {
            return repository.findByBrandIgnoreCaseAndState(brandFilter, state, pageable);
        }
        if (brandFilter != null) {
            return repository.findByBrandIgnoreCase(brandFilter, pageable);
        }
        if (state != null) {
            return repository.findByState(state, pageable);
        }
        return repository.findAll(pageable);
    }

    /**
     * Appends the primary key to the requested sort. Without a total order, rows that tie on
     * the sort key have no defined position, so a concurrent write can move one across a page
     * boundary and leave it duplicated on one page and missing from the next.
     */
    private static Pageable withStableOrder(Pageable pageable) {
        if (pageable.getSort().getOrderFor("id") != null) {
            return pageable;
        }
        Sort stable = pageable.getSort().and(Sort.by(Sort.Direction.DESC, "id"));
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), stable);
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
