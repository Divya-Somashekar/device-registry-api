package org.device.deviceregistryapi.device;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeviceController.class)
class DeviceControllerTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeviceService service;

    private static Device device(String name, String brand, DeviceState state) {
        return new Device(name, brand, state);
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        when(service.create(any())).thenReturn(device("Pixel 9", "Google", DeviceState.AVAILABLE));

        mockMvc.perform(post("/api/v1/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Pixel 9","brand":"Google"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Pixel 9"))
                .andExpect(jsonPath("$.state").value("AVAILABLE"))
                .andExpect(jsonPath("$.creationTime").exists());
    }

    @Test
    void createRejectsBlankNameWithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"  ","brand":""}"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("brand"))
                .andExpect(jsonPath("$.errors[1].field").value("name"));
    }

    @Test
    void createRejectsUnknownState() throws Exception {
        mockMvc.perform(post("/api/v1/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Pixel 9","brand":"Google","state":"BROKEN"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Malformed request"));
    }

    @Test
    void getReturns404AsProblemDetail() throws Exception {
        when(service.findById(ID)).thenThrow(new DeviceNotFoundException(ID));

        mockMvc.perform(get("/api/v1/devices/{id}", ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Device not found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getRejectsMalformedId() throws Exception {
        mockMvc.perform(get("/api/v1/devices/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid parameter"));
    }

    @Test
    void listReturnsPagedEnvelope() throws Exception {
        Page<Device> page = new PageImpl<>(
                java.util.List.of(device("Pixel 9", "Google", DeviceState.AVAILABLE)),
                PageRequest.of(0, 20), 1);
        when(service.find(eq(null), eq(null), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].brand").value("Google"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void listPassesBrandAndStateFilters() throws Exception {
        when(service.find(eq("Google"), eq(DeviceState.IN_USE), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/devices").param("brand", "Google").param("state", "IN_USE"))
                .andExpect(status().isOk());

        verify(service).find(eq("Google"), eq(DeviceState.IN_USE), any());
    }

    @Test
    void listRejectsUnknownStateFilter() throws Exception {
        mockMvc.perform(get("/api/v1/devices").param("state", "BROKEN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid parameter"));
    }

    @Test
    void putRequiresEveryProperty() throws Exception {
        mockMvc.perform(put("/api/v1/devices/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Pixel 9"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"));
    }

    @Test
    void putReturns409WhenRenamingAnInUseDevice() throws Exception {
        when(service.replace(eq(ID), any()))
                .thenThrow(new DeviceInUseException("Cannot change the name or brand"));

        mockMvc.perform(put("/api/v1/devices/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Pixel 10","brand":"Google","state":"IN_USE"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Device is in use"));
    }

    @Test
    void patchAcceptsAPartialBody() throws Exception {
        when(service.patch(eq(ID), any()))
                .thenReturn(device("Pixel 9", "Google", DeviceState.INACTIVE));

        mockMvc.perform(patch("/api/v1/devices/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"state":"INACTIVE"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("INACTIVE"));
    }

    @Test
    void patchRejectsABlankName() throws Exception {
        mockMvc.perform(patch("/api/v1/devices/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"   "}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("must not be blank"));
    }

    @Test
    void patchRejectsABlankBrand() throws Exception {
        mockMvc.perform(patch("/api/v1/devices/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"brand":""}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("brand"));
    }

    @Test
    void patchStillAcceptsAnAbsentName() throws Exception {
        when(service.patch(eq(ID), any()))
                .thenReturn(device("Pixel 9", "Google", DeviceState.INACTIVE));

        mockMvc.perform(patch("/api/v1/devices/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"state":"INACTIVE"}"""))
                .andExpect(status().isOk());
    }

    @Test
    void listRejectsAnUnknownSortProperty() throws Exception {
        when(service.find(any(), any(), any()))
                .thenThrow(new PropertyReferenceException(
                        "notAProperty", TypeInformation.of(Device.class), java.util.List.of()));

        mockMvc.perform(get("/api/v1/devices").param("sort", "notAProperty"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid sort property"));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/devices/{id}", ID))
                .andExpect(status().isNoContent());

        verify(service).delete(ID);
    }

    @Test
    void deleteRejectsMalformedId() throws Exception {
        mockMvc.perform(delete("/api/v1/devices/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid parameter"));
    }

    @Test
    void deleteReturns409WhenDeviceIsInUse() throws Exception {
        doThrow(new DeviceInUseException("Cannot delete device while it is in use"))
                .when(service).delete(ID);

        mockMvc.perform(delete("/api/v1/devices/{id}", ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void deleteReturns409WhenTheDeviceWasChangedConcurrently() throws Exception {
        doThrow(new OptimisticLockingFailureException("stale")).when(service).delete(ID);

        mockMvc.perform(delete("/api/v1/devices/{id}", ID))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Concurrent modification"))
                .andExpect(jsonPath("$.type")
                        .value("https://api.device-registry/problems/concurrent-modification"));
    }

    @Test
    void patchReturns409WhenTheDeviceWasChangedConcurrently() throws Exception {
        when(service.patch(eq(ID), any()))
                .thenThrow(new OptimisticLockingFailureException("stale"));

        mockMvc.perform(patch("/api/v1/devices/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":\"INACTIVE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Concurrent modification"));
    }
}
