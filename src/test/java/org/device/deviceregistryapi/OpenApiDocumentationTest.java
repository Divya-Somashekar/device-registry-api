package org.device.deviceregistryapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guards the published contract: the specification must stay reachable and keep describing
 * every operation, including the conflict responses the domain rules produce.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OpenApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publishesTheSpecification() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Device Registry API"))
                .andExpect(jsonPath("$.paths['/api/v1/devices'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/devices'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/devices/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/devices/{id}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/devices/{id}'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/v1/devices/{id}'].delete").exists());
    }

    @Test
    void documentsTheConflictResponses() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/devices/{id}'].put.responses.409").exists())
                .andExpect(jsonPath("$.paths['/api/v1/devices/{id}'].patch.responses.409").exists())
                .andExpect(jsonPath("$.paths['/api/v1/devices/{id}'].delete.responses.409")
                        .exists());
    }

    /**
     * The validation responses are easy to leave off an operation, because the constraint
     * lives on the request record rather than the controller method. Every operation that
     * binds a body or a typed parameter can answer 400, so each one must say so.
     */
    @Test
    void documentsTheValidationResponses() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/devices'].post.responses.400").exists())
                .andExpect(jsonPath("$.paths['/api/v1/devices'].get.responses.400").exists())
                .andExpect(jsonPath("$.paths['/api/v1/devices/{id}'].put.responses.400").exists())
                .andExpect(
                        jsonPath("$.paths['/api/v1/devices/{id}'].patch.responses.400").exists());
    }

    @Test
    void servesSwaggerUi() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
