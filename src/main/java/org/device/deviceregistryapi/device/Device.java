package org.device.deviceregistryapi.device;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.device.deviceregistryapi.common.DeviceInUseException;

/**
 * A device in the inventory.
 *
 * <p>The domain rules live here rather than in the service layer, so there is no way to
 * reach an invalid state through any caller:
 * <ul>
 *     <li>the creation time is assigned once and never changes;</li>
 *     <li>the name and brand cannot change while the device is in use.</li>
 * </ul>
 */
@Entity
@Table(name = "device")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 120)
    private String brand;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeviceState state;

    @Column(name = "creation_time", nullable = false, updatable = false)
    private Instant creationTime;

    protected Device() {
        // required by JPA
    }

    public Device(String name, String brand, DeviceState state) {
        this.name = name;
        this.brand = brand;
        this.state = state;
        this.creationTime = Instant.now();
    }

    /**
     * Changes the name and brand.
     *
     * <p>Rejected when the device is in use, unless both values already match, in which case
     * nothing is being updated. That exemption is what lets a full replacement (PUT) resend
     * the unchanged name and brand alongside a new state.
     *
     * @throws DeviceInUseException if either value would change while the device is in use
     */
    public void rename(String newName, String newBrand) {
        boolean unchanged = this.name.equals(newName) && this.brand.equals(newBrand);
        if (!unchanged && isInUse()) {
            throw new DeviceInUseException(
                    "Cannot change the name or brand of device " + id + " while it is in use");
        }
        this.name = newName;
        this.brand = newBrand;
    }

    public void changeState(DeviceState newState) {
        this.state = newState;
    }

    public boolean isInUse() {
        return state == DeviceState.IN_USE;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBrand() {
        return brand;
    }

    public DeviceState getState() {
        return state;
    }

    public Instant getCreationTime() {
        return creationTime;
    }
}
