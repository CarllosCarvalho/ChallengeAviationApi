package com.aviation.airport.config;

import feign.Logger;
import feign.Request;
import feign.Retryer;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Fine-grained Feign + OkHttp configuration.
 *
 * <p>OkHttp is preferred over the default Feign HTTP client because it
 * provides a proper connection pool, keep-alive support, and more
 * predictable timeout behaviour under load.
 *
 * <p><b>Retry note:</b> Feign's built-in {@link Retryer} is disabled here
 * ({@code Retryer.NEVER_RETRY}) because retries are managed by Resilience4j
 * in the adapter layer.  Having two retry mechanisms would create confusing
 * interactions (e.g. 3 × 3 = 9 actual upstream calls).
 */
@Configuration
public class FeignConfig {

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .connectionPool(new okhttp3.ConnectionPool(20, 5, TimeUnit.MINUTES))
                .build();
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        // BASIC logs method, URL, status and elapsed time – fine for production.
        // Switch to FULL for debugging headers/body.
        return Logger.Level.BASIC;
    }

    @Bean
    public Retryer feignRetryer() {
        // Delegate all retry responsibility to Resilience4j
        return Retryer.NEVER_RETRY;
    }

    @Bean
    public Request.Options feignOptions() {
        return new Request.Options(3, TimeUnit.SECONDS, 5, TimeUnit.SECONDS, true);
    }
}
