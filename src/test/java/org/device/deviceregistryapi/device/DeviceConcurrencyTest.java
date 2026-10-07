package org.device.deviceregistryapi.device;

import java.util.UUID;

import org.device.deviceregistryapi.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The domain rules are read-check-write: a device is loaded, the rule is tested against the
 * state it was loaded with, then the write is applied. Another transaction can commit in
 * between, which under READ COMMITTED would leave the write to succeed against a device the
 * rule was never tested on -- deleting one that had since gone into use, for instance.
 *
 * <p>These tests interleave two transactions at exactly that point. The second one is driven
 * to completion before the first writes, rather than run on a thread, so the interleaving is
 * the fixed part of the test and there is no timing to depend on.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DeviceConcurrencyTest {

    @Autowired
    private DeviceRepository repository;

    private TransactionTemplate tx;

    @BeforeEach
    void setUp(@Autowired PlatformTransactionManager txManager) {
        tx = new TransactionTemplate(txManager);
        repository.deleteAll();
    }

    private UUID given(DeviceState state) {
        return repository.save(new Device("Pixel 9", "Google", state)).getId();
    }

    /** A device as some earlier transaction read it, now detached and possibly out of date. */
    private Device readInItsOwnTransaction(UUID id) {
        return tx.execute(status -> repository.findById(id).orElseThrow());
    }

    private void changeStateInItsOwnTransaction(UUID id, DeviceState state) {
        tx.executeWithoutResult(
                status -> repository.findById(id).orElseThrow().changeState(state));
    }

    @Test
    void refusesToDeleteADeviceThatWentIntoUseAfterItWasRead() {
        UUID id = given(DeviceState.AVAILABLE);
        Device asRead = readInItsOwnTransaction(id);

        changeStateInItsOwnTransaction(id, DeviceState.IN_USE);

        assertThatThrownBy(() -> tx.executeWithoutResult(status -> repository.delete(asRead)))
                .isInstanceOf(OptimisticLockingFailureException.class);

        assertThat(repository.findById(id)).get()
                .extracting(Device::getState).isEqualTo(DeviceState.IN_USE);
    }

    @Test
    void refusesToRenameADeviceThatWentIntoUseAfterItWasRead() {
        UUID id = given(DeviceState.AVAILABLE);
        Device asRead = readInItsOwnTransaction(id);

        changeStateInItsOwnTransaction(id, DeviceState.IN_USE);

        // Passes the in-use check, because the state it was read with was AVAILABLE.
        asRead.rename("Pixel 10", "Google");

        assertThatThrownBy(() -> tx.executeWithoutResult(status -> repository.save(asRead)))
                .isInstanceOf(OptimisticLockingFailureException.class);

        assertThat(repository.findById(id)).get()
                .extracting(Device::getName).isEqualTo("Pixel 9");
    }

    @Test
    void refusesTheSecondOfTwoWritesBuiltOnTheSameRead() {
        UUID id = given(DeviceState.AVAILABLE);
        Device firstReader = readInItsOwnTransaction(id);
        Device secondReader = readInItsOwnTransaction(id);

        tx.executeWithoutResult(status -> {
            firstReader.changeState(DeviceState.IN_USE);
            repository.save(firstReader);
        });

        secondReader.changeState(DeviceState.INACTIVE);
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> repository.save(secondReader)))
                .isInstanceOf(OptimisticLockingFailureException.class);

        assertThat(repository.findById(id)).get()
                .extracting(Device::getState).isEqualTo(DeviceState.IN_USE);
    }

    @Test
    void stillDeletesWhenNothingChangedInBetween() {
        UUID id = given(DeviceState.AVAILABLE);
        Device asRead = readInItsOwnTransaction(id);

        tx.executeWithoutResult(status -> repository.delete(asRead));

        assertThat(repository.findById(id)).isEmpty();
    }

    @Test
    void countsVersionsFromZeroAndIncrementsOnEveryWrite() {
        UUID id = given(DeviceState.AVAILABLE);
        assertThat(repository.findById(id)).get().extracting(Device::getVersion).isEqualTo(0L);

        changeStateInItsOwnTransaction(id, DeviceState.IN_USE);
        assertThat(repository.findById(id)).get().extracting(Device::getVersion).isEqualTo(1L);

        changeStateInItsOwnTransaction(id, DeviceState.INACTIVE);
        assertThat(repository.findById(id)).get().extracting(Device::getVersion).isEqualTo(2L);
    }
}
