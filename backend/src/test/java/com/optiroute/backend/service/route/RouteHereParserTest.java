package com.optiroute.backend.service.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class RouteHereParserTest {

    @Test
    void shouldParseTimedSpansWithOffsetsAcrossMultipleSections() {
        RouteHereParser parser = new RouteHereParser();
        var timeline = parser.parseTimeline("""
            {"routes":[{"sections":[
              {"summary":{"duration":400},"polyline":"BGozinkDq77vZApLwCnfA7aQjL",
               "spans":[{"offset":0,"duration":100,"length":1000},{"offset":3,"duration":300,"length":500}]},
              {"summary":{"duration":100},"polyline":"BFoz5xJ67i1B1B7PzIhaxL7Y",
               "spans":[{"offset":0,"duration":100}]}
            ]}]}
            """);
        assertEquals(500, timeline.durationSeconds());
        assertEquals(3, timeline.portions().size());
        assertEquals(4, timeline.portions().getFirst().geometry().size());
        assertEquals(2, timeline.portions().get(1).geometry().size());
        assertEquals(1, timeline.portions().getLast().sectionIndex());
        assertEquals(50.10228, timeline.positionAt(400).position().getLat(), 0.000001);
        assertEquals(0, timeline.positionAt(400).firstWaypointCount());
        assertEquals(1, timeline.positionAt(400).remainingWaypointIndex());
    }

    @Test
    void shouldRejectMissingSpansAndInvalidOffsetsInsteadOfGuessingPosition() {
        RouteHereParser parser = new RouteHereParser();
        for (String spans : List.of("[]", "[{\"offset\":1,\"duration\":100}]",
            "[{\"offset\":0,\"duration\":-1}]", "[{\"offset\":0}]", "[{\"offset\":9,\"duration\":100}]")) {
            String json = """
                {"routes":[{"sections":[{"summary":{"duration":100},
                "polyline":"BFoz5xJ67i1B1B7PzIhaxL7Y","spans":%s}]}]}
                """.formatted(spans);
            assertThrows(IllegalArgumentException.class, () -> parser.parseTimeline(json));
        }
        assertThrows(IllegalArgumentException.class, () -> parser.parseTimeline("{\"routes\":[]}"));
    }

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
