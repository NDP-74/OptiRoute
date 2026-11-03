package com.optiroute.backend.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FlexiblePolylineDecoderTest {

    @Test
    void shouldDecodeHereReferenceGeometry() {
        var points = FlexiblePolylineDecoder.decode("BFoz5xJ67i1B1B7PzIhaxL7Y");
        assertEquals(4, points.size());
        assertEquals(50.10228, points.getFirst().getLat(), 0.000001);
        assertEquals(8.69821, points.getFirst().getLng(), 0.000001);
        assertEquals(50.09878, points.getLast().getLat(), 0.000001);
        assertEquals(8.68752, points.getLast().getLng(), 0.000001);
    }

    @Test
    void shouldConsumeThirdDimensionWithoutChangingLatitudeOrLongitude() {
        var points = FlexiblePolylineDecoder.decode("BlBCCC");
        assertEquals(1, points.size());
        assertEquals(0.00001, points.getFirst().getLat(), 0.000001);
        assertEquals(0.00001, points.getFirst().getLng(), 0.000001);
    }

    @Test
    void shouldRejectInvalidTruncatedAndUnsupportedPolylines() {
        for (String encoded : new String[] {"", "AF", "BF!", "BFg", "BFACg", "BF", "BgggggggggggggA"}) {
            assertThrows(IllegalArgumentException.class, () -> FlexiblePolylineDecoder.decode(encoded), encoded);
        }
    }
}
