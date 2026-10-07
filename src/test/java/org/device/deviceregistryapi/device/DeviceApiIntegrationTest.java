package org.device.deviceregistryapi.device;

import org.device.deviceregistryapi.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the API end to end against PostgreSQL, covering each domain rule through HTTP.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DeviceApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeviceRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    private String createDevice(String name, String brand, DeviceState state) throws Exception {
        String body = mockMvc.perform(post("/api/v1/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","brand":"%s","state":"%s"}"""
                                .formatted(name, brand, state)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(body, "$.id");
    }

    @Test
    void createsAndFetchesADevice() throws Exception {
        String id = createDevice("Pixel 9", "Google", DeviceState.AVAILABLE);

        mockMvc.perform(get("/api/v1/devices/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Pixel 9"))
                .andExpect(jsonPath("$.brand").value("Google"))
                .andExpect(jsonPath("$.state").value("AVAILABLE"))
                .andExpect(jsonPath("$.creationTime").isNotEmpty());
    }

    @Test
    void fetchesAllDevices() throws Exception {
        createDevice("Pixel 9", "Google", DeviceState.AVAILABLE);
        createDevice("Galaxy S25", "Samsung", DeviceState.IN_USE);

        mockMvc.perform(get("/api/v1/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void fetchesDevicesByBrand() throws Exception {
        createDevice("Pixel 9", "Google", DeviceState.AVAILABLE);
        createDevice("Galaxy S25", "Samsung", DeviceState.AVAILABLE);

        mockMvc.perform(get("/api/v1/devices").param("brand", "Samsung"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Galaxy S25"));
    }

    /**
     * The brand filter strips its argument, so a brand persisted with surrounding whitespace
     * used to be unreachable from either side: the trimmed query missed the padded column, and
     * a padded query was stripped before it was used. The brand is now normalised on write.
     */
    @Test
    void fetchesADeviceWhoseBrandWasCreatedWithSurroundingWhitespace() throws Exception {
        createDevice("Pixel 9", "  Google  ", DeviceState.AVAILABLE);

        mockMvc.perform(get("/api/v1/devices").param("brand", "Google"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].brand").value("Google"));
    }

    @Test
    void ignoresABlankBrandFilter() throws Exception {
        createDevice("Pixel 9", "Google", DeviceState.AVAILABLE);
        createDevice("Galaxy S25", "Samsung", DeviceState.AVAILABLE);

        mockMvc.perform(get("/api/v1/devices").param("brand", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void fetchesDevicesByState() throws Exception {
        createDevice("Pixel 9", "Google", DeviceState.AVAILABLE);
        createDevice("Galaxy S25", "Samsung", DeviceState.IN_USE);

        mockMvc.perform(get("/api/v1/devices").param("state", "IN_USE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].brand").value("Samsung"));
    }

    @Test
    void paginatesTheCollection() throws Exception {
        createDevice("Pixel 9", "Google", DeviceState.AVAILABLE);
        createDevice("Pixel 8", "Google", DeviceState.AVAILABLE);
        createDevice("Pixel 7", "Google", DeviceState.AVAILABLE);

        mockMvc.perform(get("/api/v1/devices").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void fullyUpdatesADevice() throws Exception {
        String id = createDevice("Pixel 9", "Google", DeviceState.AVAILABLE);

        mockMvc.perform(put("/api/v1/devices/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Pixel 10","brand":"Alphabet","state":"INACTIVE"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pixel 10"))
                .andExpect(jsonPath("$.brand").value("Alphabet"))
                .andExpect(jsonPath("$.state").value("INACTIVE"));
    }

    @Test
    void partiallyUpdatesADevice() throws Exception {
        String id = createDevice("Pixel 9", "Google", DeviceState.AVAILABLE);

        mockMvc.perform(patch("/api/v1/devices/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"state":"IN_USE"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pixel 9"))
                .andExpect(jsonPath("$.state").value("IN_USE"));
    }

    @Test
    void ignoresACreationTimeSuppliedByTheClient() throws Exception {
        String id = createDevice("Pixel 9", "Google", DeviceState.AVAILABLE);
        String original = com.jayway.jsonpath.JsonPath.read(
                mockMvc.perform(get("/api/v1/devices/{id}", id))
                        .andReturn().getResponse().getContentAsString(),
                "$.creationTime");

        mockMvc.perform(put("/api/v1/devices/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Pixel 10","brand":"Google","state":"AVAILABLE",
                                 "creationTime":"2000-01-01T00:00:00Z"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creationTime").value(original));
    }

    @Test
    void refusesToRenameADeviceInUse() throws Exception {
        String id = createDevice("Pixel 9", "Google", DeviceState.IN_USE);

        mockMvc.perform(patch("/api/v1/devices/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Pixel 10"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Device is in use"));

        mockMvc.perform(get("/api/v1/devices/{id}", id))
                .andExpect(jsonPath("$.name").value("Pixel 9"));
    }

    @Test
    void refusesToChangeTheBrandOfADeviceInUse() throws Exception {
        String id = createDevice("Pixel 9", "Google", DeviceState.IN_USE);

        mockMvc.perform(put("/api/v1/devices/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Pixel 9","brand":"Alphabet","state":"IN_USE"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void releasesADeviceInUseWhenNameAndBrandAreUnchanged() throws Exception {
        String id = createDevice("Pixel 9", "Google", DeviceState.IN_USE);

        mockMvc.perform(put("/api/v1/devices/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Pixel 9","brand":"Google","state":"AVAILABLE"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("AVAILABLE"));
    }

    @Test
    void deletesADevice() throws Exception {
        String id = createDevice("Pixel 9", "Google", DeviceState.AVAILABLE);

        mockMvc.perform(delete("/api/v1/devices/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/devices/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void refusesToDeleteADeviceInUse() throws Exception {
        String id = createDevice("Pixel 9", "Google", DeviceState.IN_USE);

        mockMvc.perform(delete("/api/v1/devices/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Device is in use"));

        mockMvc.perform(get("/api/v1/devices/{id}", id)).andExpect(status().isOk());
    }

    @Test
    void capsThePageSize() throws Exception {
        mockMvc.perform(get("/api/v1/devices").param("size", "100000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void rejectsAnUnknownSortProperty() throws Exception {
        mockMvc.perform(get("/api/v1/devices").param("sort", "notAProperty"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid sort property"));
    }

    @Test
    void rejectsABlankNameOnPatch() throws Exception {
        String id = createDevice("Pixel 9", "Google", DeviceState.AVAILABLE);

        mockMvc.perform(patch("/api/v1/devices/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"   "}"""))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/devices/{id}", id))
                .andExpect(jsonPath("$.name").value("Pixel 9"));
    }

    @Test
    void exposesAHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void returns404ForAnUnknownDevice() throws Exception {
        mockMvc.perform(get("/api/v1/devices/{id}", "11111111-1111-1111-1111-111111111111"))
                .andExpect(status().isNotFound());
    }
}
