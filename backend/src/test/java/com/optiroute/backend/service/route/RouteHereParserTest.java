package com.optiroute.backend.service.route;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class RouteHereParserTest {

    @Test
    void shouldAggregateAllSectionsInRoute() {
        RouteHereParser parser = new RouteHereParser();

        String json = """
            {
              "routes": [
                {
                  "sections": [
                    {
                      "summary": {"duration": 100, "baseDuration": 80, "length": 200},
                      "polyline": "AAA",
                      "tolls": []
                    },
                    {
                      "summary": {"duration": 200, "baseDuration": 150, "length": 300},
                      "polyline": "BBB",
                      "tolls": [{"fares": [{"price": {"value": 5.5}}]}]
                    }
                  ]
                }
              ]
            }
            """;

        List<RouteHereParser.ParsedRoute> routes = parser.parseRoutes(json);

        assertEquals(1,routes.size());
        assertEquals(300L,routes.getFirst().duration);
        assertEquals(230L,routes.getFirst().baseDuration);
        assertEquals(500L,routes.getFirst().distanceMeters);
        assertEquals(5.5,routes.getFirst().tollCost,0.0001);
        assertEquals("[\"AAA\",\"BBB\"]",routes.getFirst().polyline);
    }

    @Test
    void shouldUseOneFarePerTollSystemAndAddSeparateTollSystems() {
        RouteHereParser parser = new RouteHereParser();

        String json = """
            {
              "routes": [
                {
                  "sections": [
                    {
                      "summary": {"duration": 100, "baseDuration": 80, "length": 200},
                      "tolls": [
                        {"fares": [
                          {"price": {"value": 5.5}},
                          {"price": {"value": 7.0}}
                        ]},
                        {"fares": [{"price": {"value": 2.5}}]}
                      ]
                    }
                  ]
                }
              ]
            }
            """;

        List<RouteHereParser.ParsedRoute> routes = parser.parseRoutes(json);

        assertEquals(8.0,routes.getFirst().tollCost,0.0001);
    }
}
