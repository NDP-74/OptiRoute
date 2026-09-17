package com.optiroute.backend.service.route;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class RoutePtvParserTest {

  @Test
  void shouldParseRouteAndAlternativeRoute() {
    RoutePtvParser parser = new RoutePtvParser();

    String json = """
      {
        "distance": 12000,
        "travelTime": 900,
        "trafficDelay": 60,
        "polyline": "route-polyline",
        "toll": {"costs": {"prices": [{"price": 7.5, "currency": "EUR"}] }},
        "alternativeRoutes": [
          {"distance": 14000, "travelTime": 1000, "polyline": "alternative-polyline"}
        ]
      }
      """;

    List<RoutePtvParser.ParsedRoute> routes = parser.parseRoutes(json);

    assertEquals(2,routes.size());
    assertEquals(12000,routes.getFirst().distanceMeters);
    assertEquals(840,routes.getFirst().baseDuration);
    assertEquals(7.5,routes.getFirst().tollCost,0.0001);
    assertEquals("alternative-polyline",routes.get(1).polyline);
  }
}