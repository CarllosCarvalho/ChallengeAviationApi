package com.aviation.airport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Entry point for the Airport Service microservice.
 *
 * <p>Uses Java 21 Virtual Threads (spring.threads.virtual.enabled=true in application.yml)
 * so every incoming request is handled on a lightweight virtual thread, allowing high
 * concurrency without the overhead of a traditional thread-pool tuning exercise.
 */
@SpringBootApplication
@EnableFeignClients
public class AirportServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AirportServiceApplication.class, args);
    }
}
