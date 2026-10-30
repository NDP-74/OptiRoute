package com.optiroute.backend.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.optiroute.backend.config.HereApiProperties;
import com.optiroute.backend.model.TruckConfiguration;
import com.optiroute.backend.type.RouteTimeMode;

class HereApiClientTest {

    @Test
    void shouldSendArrivalTimeInsteadOfDepartureTime() {
        verifyTimeParameter(RouteTimeMode.ARRIVAL, "arrivalTime", "departureTime");
    }

    @Test
    void shouldPreserveDepartureTimeMode() {
        verifyTimeParameter(RouteTimeMode.DEPARTURE, "departureTime", "arrivalTime");
    }

    private void verifyTimeParameter(RouteTimeMode mode, String expected, String absent) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HereApiProperties properties = new HereApiProperties();
        properties.setApiKey("test-key");
        HereApiClient client = new HereApiClient(builder, properties);
        server.expect(request -> {
            String query = request.getURI().getQuery();
            assertTrue(query.contains(expected + "=2026-10-29T10:00:00Z"));
            assertFalse(query.contains(absent + "="));
            assertTrue(query.contains("via=48,2"));
        }).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.getRoutes("47,1", "49,3", List.of("48,2"), "2026-10-29T10:00:00Z",
            mode, new TruckConfiguration(), false, 0);

        server.verify();
    }
}
