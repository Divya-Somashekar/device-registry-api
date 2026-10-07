package org.device.deviceregistryapi.device;

import java.util.Optional;
import java.util.UUID;

import org.device.deviceregistryapi.device.dto.CreateDeviceRequest;
import org.device.deviceregistryapi.device.dto.PatchDeviceRequest;
import org.device.deviceregistryapi.device.dto.ReplaceDeviceRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    private static final UUID ID = UUID.randomUUID();
    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    @Mock
    private DeviceRepository repository;

    @InjectMocks
    private DeviceService service;

    private void existing(Device device) {
        when(repository.findById(ID)).thenReturn(Optional.of(device));
    }

    @Test
    void createsWithAvailableWhenStateOmitted() {
        when(repository.save(any(Device.class))).thenAnswer(call -> call.getArgument(0));

        service.create(new CreateDeviceRequest("Pixel 9", "Google", null));

        ArgumentCaptor<Device> saved = ArgumentCaptor.forClass(Device.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getState()).isEqualTo(DeviceState.AVAILABLE);
    }

    @Test
    void createsWithGivenState() {
        when(repository.save(any(Device.class))).thenAnswer(call -> call.getArgument(0));

        Device created = service.create(
                new CreateDeviceRequest("Pixel 9", "Google", DeviceState.INACTIVE));

        assertThat(created.getState()).isEqualTo(DeviceState.INACTIVE);
    }

    @Test
    void throwsWhenDeviceIsMissing() {
        when(repository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(ID)).isInstanceOf(DeviceNotFoundException.class);
    }

    @Test
    void replaceAppliesEveryProperty() {
        existing(new Device("Pixel 9", "Google", DeviceState.AVAILABLE));

        Device result = service.replace(ID,
                new ReplaceDeviceRequest("Pixel 10", "Alphabet", DeviceState.IN_USE));

        assertThat(result.getName()).isEqualTo("Pixel 10");
        assertThat(result.getBrand()).isEqualTo("Alphabet");
        assertThat(result.getState()).isEqualTo(DeviceState.IN_USE);
    }

    @Test
    void replaceRejectsRenameOfInUseDeviceEvenWhenMovingItOutOfUse() {
        existing(new Device("Pixel 9", "Google", DeviceState.IN_USE));

        assertThatThrownBy(() -> service.replace(ID,
                new ReplaceDeviceRequest("Pixel 10", "Google", DeviceState.AVAILABLE)))
                .isInstanceOf(DeviceInUseException.class);
    }

    @Test
    void replaceCanReleaseAnInUseDeviceWhenNameAndBrandAreUnchanged() {
        existing(new Device("Pixel 9", "Google", DeviceState.IN_USE));

        Device result = service.replace(ID,
                new ReplaceDeviceRequest("Pixel 9", "Google", DeviceState.AVAILABLE));

        assertThat(result.getState()).isEqualTo(DeviceState.AVAILABLE);
    }

    @Test
    void patchLeavesAbsentPropertiesUntouched() {
        existing(new Device("Pixel 9", "Google", DeviceState.AVAILABLE));

        Device result = service.patch(ID, new PatchDeviceRequest(null, null, DeviceState.INACTIVE));

        assertThat(result.getName()).isEqualTo("Pixel 9");
        assertThat(result.getBrand()).isEqualTo("Google");
        assertThat(result.getState()).isEqualTo(DeviceState.INACTIVE);
    }

    @Test
    void patchRejectsRenameWhileInUse() {
        existing(new Device("Pixel 9", "Google", DeviceState.IN_USE));

        assertThatThrownBy(() ->
                service.patch(ID, new PatchDeviceRequest("Pixel 10", null, null)))
                .isInstanceOf(DeviceInUseException.class);
    }

    @Test
    void patchCanChangeStateOfAnInUseDevice() {
        existing(new Device("Pixel 9", "Google", DeviceState.IN_USE));

        Device result = service.patch(ID, new PatchDeviceRequest(null, null, DeviceState.AVAILABLE));

        assertThat(result.getState()).isEqualTo(DeviceState.AVAILABLE);
    }

    @Test
    void deletesWhenNotInUse() {
        Device device = new Device("Pixel 9", "Google", DeviceState.AVAILABLE);
        existing(device);

        service.delete(ID);

        verify(repository).delete(device);
    }

    @Test
    void refusesToDeleteAnInUseDevice() {
        existing(new Device("Pixel 9", "Google", DeviceState.IN_USE));

        assertThatThrownBy(() -> service.delete(ID)).isInstanceOf(DeviceInUseException.class);

        verify(repository, never()).delete(any());
    }

    @Test
    void treatsABlankBrandAsNoFilter() {
        when(repository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        service.find("   ", null, PAGEABLE);

        verify(repository).findAll(any(Pageable.class));
        verify(repository, never()).findByBrandIgnoreCase(any(), any());
    }

    @Test
    void trimsTheBrandFilter() {
        when(repository.findByBrandIgnoreCase(eq("Google"), any())).thenReturn(Page.empty());

        service.find("  Google  ", null, PAGEABLE);

        verify(repository).findByBrandIgnoreCase(eq("Google"), any());
    }

    @Test
    void appendsThePrimaryKeyToTheSortSoPagingIsStable() {
        ArgumentCaptor<Pageable> used = ArgumentCaptor.forClass(Pageable.class);
        when(repository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        service.find(null, null, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC,
                "creationTime")));

        verify(repository).findAll(used.capture());
        assertThat(used.getValue().getSort().getOrderFor("id")).isNotNull();
    }

    @Test
    void doesNotDuplicateAnExplicitIdSort() {
        ArgumentCaptor<Pageable> used = ArgumentCaptor.forClass(Pageable.class);
        when(repository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        service.find(null, null, PageRequest.of(0, 20, Sort.by("id")));

        verify(repository).findAll(used.capture());
        assertThat(used.getValue().getSort()).hasSize(1);
    }

    @Test
    void routesFiltersToTheMatchingFinder() {
        when(repository.findByBrandIgnoreCaseAndState(eq("Google"), eq(DeviceState.IN_USE), any()))
                .thenReturn(Page.empty());
        when(repository.findByBrandIgnoreCase(eq("Google"), any())).thenReturn(Page.empty());
        when(repository.findByState(eq(DeviceState.IN_USE), any())).thenReturn(Page.empty());
        when(repository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        service.find("Google", DeviceState.IN_USE, PAGEABLE);
        service.find("Google", null, PAGEABLE);
        service.find(null, DeviceState.IN_USE, PAGEABLE);
        service.find(null, null, PAGEABLE);

        verify(repository).findByBrandIgnoreCaseAndState(eq("Google"), eq(DeviceState.IN_USE),
                any());
        verify(repository).findByBrandIgnoreCase(eq("Google"), any());
        verify(repository).findByState(eq(DeviceState.IN_USE), any());
        verify(repository).findAll(any(Pageable.class));
    }
}
