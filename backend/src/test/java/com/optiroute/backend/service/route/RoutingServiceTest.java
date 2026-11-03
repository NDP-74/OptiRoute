package com.optiroute.backend.service.route;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.optiroute.backend.client.HereApiClient;
import com.optiroute.backend.client.PtvApiClient;
import com.optiroute.backend.config.HereApiProperties;
import com.optiroute.backend.dto.request.route.RouteRequest;
import com.optiroute.backend.exception.TransportPlanningException;
import com.optiroute.backend.model.Position;
import com.optiroute.backend.model.TruckConfiguration;
import com.optiroute.backend.type.GpsModeType;
import com.optiroute.backend.type.RouteTimeMode;

class RoutingServiceTest {

    @Test
    void shouldRetrieveAndDecodeTimedGeometryWithTheRequestedRouteModeAndWaypoints() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HereApiProperties properties = new HereApiProperties();
        properties.setApiKey("test-key");
        HereApiClient here = new HereApiClient(builder, properties);
        RoutingService service = new RoutingService(mock(PtvApiClient.class), here,
            new RoutePtvParser(), new RouteHereParser(), "here");
        RouteRequest request = new RouteRequest();
        request.setOrigin(new Position(50, 8));
        request.setDestination(new Position(51, 9));
        request.setWaypoints(List.of(new Position(50.5, 8.5)));
        request.setRouteTime(OffsetDateTime.parse("2026-10-29T18:00:00+01:00"));
        request.setTimeMode(RouteTimeMode.DEPARTURE);
        request.setMode(GpsModeType.CHEAPEST);
        server.expect(http -> {
            String query = http.getURI().getQuery();
            assertTrue(query.contains("spans=duration,length,dynamicSpeedInfo"));
            assertTrue(query.contains("departureTime="));
            assertTrue(query.contains("via=50.5,8.5"));
            assertTrue(query.contains("avoid[features]=tollRoad"));
        }).andRespond(withSuccess("""
            {"routes":[{"sections":[
              {"summary":{"duration":100},"polyline":"BFoz5xJ67i1B1B7PzIhaxL7Y",
               "spans":[{"offset":0,"duration":20},{"offset":1,"duration":80}]},
              {"summary":{"duration":100},"polyline":"BFoz5xJ67i1B1B7PzIhaxL7Y",
               "spans":[{"offset":0,"duration":100}]}
            ]}]}
            """, MediaType.APPLICATION_JSON));

        var timeline = service.calculateTimeline(request, new TruckConfiguration());

        assertEquals(200, timeline.durationSeconds());
        assertEquals(3, timeline.portions().size());
        assertEquals(2, timeline.portions().getFirst().geometry().size());
        assertEquals(50.102145, timeline.positionAt(10).position().getLat(), 0.000001);
        assertEquals(8.69694, timeline.positionAt(10).position().getLng(), 0.000001);
        server.verify();
    }

    @Test
    void shouldExplicitlyRejectUnimplementedPtvTimelineWithoutCallingEitherProvider() {
        HereApiClient here = mock(HereApiClient.class);
        PtvApiClient ptv = mock(PtvApiClient.class);
        RoutingService service = new RoutingService(ptv, here, new RoutePtvParser(), new RouteHereParser(), "ptv");

        assertThrows(TransportPlanningException.class,
            () -> service.calculateTimeline(new RouteRequest(), new TruckConfiguration()));
        verifyNoInteractions(here, ptv);
    }
}
