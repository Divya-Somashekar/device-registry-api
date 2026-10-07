package org.device.deviceregistryapi.device;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, UUID> {

    Page<Device> findByBrandIgnoreCase(String brand, Pageable pageable);

    Page<Device> findByState(DeviceState state, Pageable pageable);

    Page<Device> findByBrandIgnoreCaseAndState(String brand, DeviceState state, Pageable pageable);
}
