package com.optiroute.backend.mapper;

import com.optiroute.backend.dto.request.route.RouteRequest;
import com.optiroute.backend.dto.request.transport.TransportRequest;
import com.optiroute.backend.model.Position;
import com.optiroute.backend.type.GpsModeType;
import com.optiroute.backend.type.RouteTimeMode;

import org.springframework.stereotype.Component;

@Component
public class RouteRequestFactory {

	public RouteRequest fromTransport(TransportRequest transport) {

		RouteRequest request = new RouteRequest();

		// Départ
		Position origin = new Position();
		origin.setLat(transport.originLat());
		origin.setLng(transport.originLng());
		request.setOrigin(origin);

		// Destination
		Position destination = new Position();
		destination.setLat(transport.destinationLat());
		destination.setLng(transport.destinationLng());
		request.setDestination(destination);

		// Heure de départ
		request.setRouteTime(transport.plannedStart());
		request.setTimeMode(RouteTimeMode.DEPARTURE);

		// Mode de calcul (à adapter selon ton enum)
		request.setMode(GpsModeType.FASTEST);

		// Caractéristiques véhicule
		request.setTractorId(transport.tractorId());
		request.setSemiTrailerId(transport.semiTrailerId());
		request.setEmptyTrip(transport.emptyTrip());

		return request;
	}
}