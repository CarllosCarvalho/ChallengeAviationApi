package com.aviation.airport.config;

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
    public OpenAPI airportServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Airport Service API")
                        .description("Microservice for fetching airport details by ICAO code")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Aviation Team")
                                .email("aviation@example.com"))
                        .license(new License()
                                .name("Apache 2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local")
                ));
    }
}