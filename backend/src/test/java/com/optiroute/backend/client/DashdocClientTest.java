package com.optiroute.backend.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

class DashdocClientTest {

    @Test
    void sendsRawTokenInAuthorizationHeader() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.dashdoc.eu/api/v4/transports/?status=acknowledged"))
            .andExpect(header("Authorization", "test-token"))
            .andRespond(withSuccess("""
                {"results": [{"uid": "transport-1"}], "next": null}
                """, MediaType.APPLICATION_JSON));
        DashdocClient client = new DashdocClient(builder.build(), "test-token", Duration.ofSeconds(120));

        List<?> transports = client.getTransports();

        assertEquals(1, transports.size());
        server.verify();
    }

    @Test
    void reportsRejectedDashdocCredentialsWithoutExposingTheToken() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.dashdoc.eu/api/v4/transports/?status=acknowledged"))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        DashdocClient client = new DashdocClient(builder.build(), "test-token", Duration.ofSeconds(120));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, client::getTransports);

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
        org.junit.jupiter.api.Assertions.assertTrue(exception.getReason().contains("Dashdoc a refusé la clé API"));
        org.junit.jupiter.api.Assertions.assertTrue(exception.getReason().contains("l’en-tête Authorization"));
        org.junit.jupiter.api.Assertions.assertFalse(exception.getReason().contains("test-token"));
        server.verify();
    }

    @Test
    void reportsDashdocReadTimeoutAsGatewayTimeout() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.dashdoc.eu/api/v4/transports/?status=acknowledged"))
            .andRespond(withException(new IOException("Read timed out")));
        DashdocClient client = new DashdocClient(builder.build(), "test-token", Duration.ofSeconds(120));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, client::getTransports);

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, exception.getStatusCode());
        org.junit.jupiter.api.Assertions.assertTrue(exception.getReason().contains("DASHDOC_READ_TIMEOUT"));
        server.verify();
    }

    @Test
    void preservesDashdocGatewayTimeoutStatus() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.dashdoc.eu/api/v4/transports/?status=acknowledged"))
            .andRespond(withStatus(HttpStatus.GATEWAY_TIMEOUT));
        DashdocClient client = new DashdocClient(builder.build(), "test-token", Duration.ofSeconds(120));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, client::getTransports);

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, exception.getStatusCode());
        org.junit.jupiter.api.Assertions.assertTrue(exception.getReason().contains("passerelle Dashdoc"));
        server.verify();
    }
}
