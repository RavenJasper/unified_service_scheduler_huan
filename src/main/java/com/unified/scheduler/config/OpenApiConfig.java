package com.unified.scheduler.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("The Unified Service Scheduler (USS) API")
                .version("1.0.0")
                .description("Production-grade high-concurrency automotive service scheduling API. Provides zero double-booking guarantees across service bays and certified technicians.")
                .contact(new Contact()
                    .name("Engineering Team")
                    .email("support@keyloop.scheduler.io"))
                .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
            .servers(List.of(
                new Server().url("/").description("Current Server Context")
            ));
    }
}
