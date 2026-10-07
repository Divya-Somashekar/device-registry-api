package org.device.deviceregistryapi.device;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeviceTest {

    @Test
    void stampsCreationTimeOnConstruction() {
        Instant before = Instant.now();

        Device device = new Device("Pixel 9", "Google", DeviceState.AVAILABLE);

        assertThat(device.getCreationTime()).isBetween(before, Instant.now());
    }

    @Test
    void renamesWhenAvailable() {
        Device device = new Device("Pixel 9", "Google", DeviceState.AVAILABLE);

        device.rename("Pixel 10", "Google Inc");

        assertThat(device.getName()).isEqualTo("Pixel 10");
        assertThat(device.getBrand()).isEqualTo("Google Inc");
    }

    @Test
    void renamesWhenInactive() {
        Device device = new Device("Pixel 9", "Google", DeviceState.INACTIVE);

        device.rename("Pixel 10", "Google Inc");

        assertThat(device.getName()).isEqualTo("Pixel 10");
    }

    @Test
    void rejectsNameChangeWhileInUse() {
        Device device = new Device("Pixel 9", "Google", DeviceState.IN_USE);

        assertThatThrownBy(() -> device.rename("Pixel 10", "Google"))
                .isInstanceOf(DeviceInUseException.class);

        assertThat(device.getName()).isEqualTo("Pixel 9");
    }

    @Test
    void rejectsBrandChangeWhileInUse() {
        Device device = new Device("Pixel 9", "Google", DeviceState.IN_USE);

        assertThatThrownBy(() -> device.rename("Pixel 9", "Alphabet"))
                .isInstanceOf(DeviceInUseException.class);

        assertThat(device.getBrand()).isEqualTo("Google");
    }

    @Test
    void allowsRenameToIdenticalValuesWhileInUse() {
        Device device = new Device("Pixel 9", "Google", DeviceState.IN_USE);

        assertThatCode(() -> device.rename("Pixel 9", "Google")).doesNotThrowAnyException();
    }

    @Test
    void rejectsABlankNameOnRename() {
        Device device = new Device("Pixel 9", "Google", DeviceState.AVAILABLE);

        assertThatThrownBy(() -> device.rename("   ", "Google"))
                .isInstanceOf(InvalidDeviceException.class);

        assertThat(device.getName()).isEqualTo("Pixel 9");
    }

    @Test
    void rejectsABlankBrandOnRename() {
        Device device = new Device("Pixel 9", "Google", DeviceState.AVAILABLE);

        assertThatThrownBy(() -> device.rename("Pixel 9", ""))
                .isInstanceOf(InvalidDeviceException.class);

        assertThat(device.getBrand()).isEqualTo("Google");
    }

    @Test
    void rejectsABlankNameOnConstruction() {
        assertThatThrownBy(() -> new Device(" ", "Google", DeviceState.AVAILABLE))
                .isInstanceOf(InvalidDeviceException.class);
    }

    @Test
    void allowsStateChangeWhileInUse() {
        Device device = new Device("Pixel 9", "Google", DeviceState.IN_USE);

        device.changeState(DeviceState.AVAILABLE);

        assertThat(device.getState()).isEqualTo(DeviceState.AVAILABLE);
        assertThat(device.isInUse()).isFalse();
    }

    @Test
    void keepsCreationTimeAcrossUpdates() {
        Device device = new Device("Pixel 9", "Google", DeviceState.AVAILABLE);
        Instant created = device.getCreationTime();

        device.rename("Pixel 10", "Google");
        device.changeState(DeviceState.INACTIVE);

        assertThat(device.getCreationTime()).isEqualTo(created);
    }
}
