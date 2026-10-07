package org.device.deviceregistryapi.common;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;

import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Device Registry API",
                version = "v1",
                description = """
                        Persists and manages device resources.

                        Three domain rules shape the API:
                        the creation time is assigned once and is never accepted from a client;
                        the name and brand of a device that is in use cannot be changed;
                        a device that is in use cannot be deleted.
                        A request that breaks either of the last two is answered with 409.""",
                license = @License(name = "MIT")))
class OpenApiConfig {
}
