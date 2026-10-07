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
import jakarta.persistence.Version;

/**
 * A device in the inventory.
 *
 * <p>The domain rules live here rather than in the service layer, so there is no way to
 * reach an invalid state through any caller:
 * <ul>
 *     <li>the creation time is assigned once and never changes;</li>
 *     <li>the name and brand cannot change while the device is in use;</li>
 *     <li>the name and brand always carry a visible character.</li>
 * </ul>
 *
 * <p>The rules above are checks against the state the device was loaded with, so on their own
 * they are only as current as that read. {@code version} closes the gap: a write built on a
 * stale read is rejected at flush rather than silently overwriting a newer one.
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

    /**
     * Incremented by Hibernate on every update, and matched in the {@code WHERE} clause of
     * every update and delete. Not part of the HTTP contract: it guards operations within the
     * service, which read and write a device in one transaction.
     */
    @Version
    private long version;

    protected Device() {
        // required by JPA
    }

    public Device(String name, String brand, DeviceState state) {
        this.name = requireText(name, "name");
        this.brand = requireText(brand, "brand");
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
     * @throws DeviceInUseException  if either value would change while the device is in use
     * @throws InvalidDeviceException if either value is blank
     */
    public void rename(String newName, String newBrand) {
        requireText(newName, "name");
        requireText(newBrand, "brand");

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

    /**
     * Guards the invariant at the point of mutation, so it holds for every caller rather than
     * only for the request models that happen to declare a constraint.
     */
    private static String requireText(String value, String property) {
        if (value == null || value.isBlank()) {
            throw new InvalidDeviceException("The device " + property + " must not be blank");
        }
        return value;
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

    public long getVersion() {
        return version;
    }
}
