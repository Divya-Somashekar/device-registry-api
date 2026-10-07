package org.device.deviceregistryapi.device;

import org.device.deviceregistryapi.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against a real PostgreSQL instance, so the Flyway schema and the entity mapping are
 * verified together.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class DeviceRepositoryTest {

    @Autowired
    private DeviceRepository repository;

    @BeforeEach
    void seed() {
        repository.deleteAll();
        repository.save(new Device("Pixel 9", "Google", DeviceState.AVAILABLE));
        repository.save(new Device("Pixel 8", "Google", DeviceState.IN_USE));
        repository.save(new Device("Galaxy S25", "Samsung", DeviceState.AVAILABLE));
    }

    @Test
    void assignsIdAndPersistsCreationTime() {
        Device saved = repository.save(new Device("Xperia", "Sony", DeviceState.AVAILABLE));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreationTime()).isNotNull();
    }

    @Test
    void findsByBrandIgnoringCase() {
        var page = repository.findByBrandIgnoreCase("google", PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(Device::getBrand).containsOnly("Google");
    }

    @Test
    void findsByState() {
        var page = repository.findByState(DeviceState.AVAILABLE, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void findsByBrandAndState() {
        var page = repository.findByBrandIgnoreCaseAndState(
                "Google", DeviceState.IN_USE, PageRequest.of(0, 10));

        assertThat(page.getContent()).singleElement()
                .extracting(Device::getName).isEqualTo("Pixel 8");
    }

    @Test
    void paginates() {
        var page = repository.findAll(PageRequest.of(0, 2));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }
}
