package com.optiroute.backend.mapper;

import com.optiroute.backend.dto.request.route.RouteRequest;
import com.optiroute.backend.entity.transport.Transport;
import com.optiroute.backend.model.Position;
import com.optiroute.backend.type.GpsModeType;
import com.optiroute.backend.type.RouteTimeMode;

import org.springframework.stereotype.Component;

@Component
public class RouteRequestFactory {

	public RouteRequest fromTransport(Transport transport) {

		RouteRequest request = new RouteRequest();

		// Départ
		Position origin = new Position();
		origin.setLat(transport.getOriginLat());
		origin.setLng(transport.getOriginLng());
		request.setOrigin(origin);

		// Destination
		Position destination = new Position();
		destination.setLat(transport.getDestinationLat());
		destination.setLng(transport.getDestinationLng());
		request.setDestination(destination);

		// Heure de départ
		request.setRouteTime(transport.getPlannedStart());
		request.setTimeMode(RouteTimeMode.DEPARTURE);

		// Mode de calcul (à adapter selon ton enum)
		request.setMode(GpsModeType.FASTEST);

		// Caractéristiques véhicule
		request.setTractorId(transport.getTractorId());
		request.setSemiTrailerId(transport.getSemiTrailerId());
		request.setEmptyTrip(transport.isEmptyTrip());

		return request;
	}
}