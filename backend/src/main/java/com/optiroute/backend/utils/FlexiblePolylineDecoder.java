package com.optiroute.backend.utils;

import java.util.ArrayList;
import java.util.List;

import com.optiroute.backend.model.Position;

public final class FlexiblePolylineDecoder {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

    private FlexiblePolylineDecoder() {
    }

    public static List<Position> decode(String encoded) {
        Reader reader = new Reader(encoded);
        if (reader.readUnsigned() != 1) {
            throw new IllegalArgumentException("Unsupported HERE flexible polyline version");
        }
        long header = reader.readUnsigned();
        double factor = Math.pow(10, header & 15);
        int thirdDimension = (int) ((header >> 4) & 7);
        if (thirdDimension == 4 || thirdDimension == 5 || header >> 11 != 0) {
            throw new IllegalArgumentException("Invalid HERE flexible polyline header");
        }
        List<Position> positions = new ArrayList<>();
        long lat = 0;
        long lng = 0;
        while (reader.hasNext()) {
            lat = Math.addExact(lat, reader.readSigned());
            lng = Math.addExact(lng, reader.readSigned());
            if (thirdDimension != 0) {
                reader.readSigned();
            }
            double latitude = lat / factor;
            double longitude = lng / factor;
            if (Math.abs(latitude) > 90 || Math.abs(longitude) > 180) {
                throw new IllegalArgumentException("Invalid HERE polyline coordinate");
            }
            positions.add(new Position(latitude, longitude));
        }
        if (positions.isEmpty()) {
            throw new IllegalArgumentException("Empty HERE flexible polyline");
        }
        return List.copyOf(positions);
    }

    private static final class Reader {
        private final String encoded;
        private int index;

        Reader(String encoded) {
            this.encoded = encoded;
        }

        boolean hasNext() {
            return index < encoded.length();
        }

        long readSigned() {
            long value = readUnsigned();
            return (value & 1) == 0 ? value >>> 1 : ~(value >>> 1);
        }

        long readUnsigned() {
            long result = 0;
            for (int shift = 0; shift <= 60; shift += 5) {
                if (!hasNext()) {
                    throw new IllegalArgumentException("Truncated HERE flexible polyline");
                }
                int value = ALPHABET.indexOf(encoded.charAt(index++));
                if (value < 0 || (shift == 60 && (value & 31) > 7)) {
                    throw new IllegalArgumentException("Invalid HERE flexible polyline encoding");
                }
                result |= (long) (value & 31) << shift;
                if ((value & 32) == 0) {
                    return result;
                }
            }
            throw new IllegalArgumentException("HERE flexible polyline integer overflow");
        }
    }
}
