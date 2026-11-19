package com.optiroute.backend.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * Délais maximaux des appels HTTP sortants (HERE, PTV). Sans eux, un fournisseur qui ne répond plus bloque
 * les threads et les connexions base de données indéfiniment.
 */
@Configuration
public class HttpClientConfig {

    @Bean
    RestClientCustomizer timeoutRestClientCustomizer(@Value("${app.http.connect-timeout:5s}") Duration connectTimeout,
            @Value("${app.http.read-timeout:30s}") Duration readTimeout) {
        return builder -> builder.requestFactory(requestFactory(connectTimeout, readTimeout));
    }

    public static SimpleClientHttpRequestFactory requestFactory(Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return factory;
    }
}
