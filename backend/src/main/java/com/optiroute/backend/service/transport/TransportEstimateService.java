package com.optiroute.backend.service.transport;

import com.optiroute.backend.dto.response.route.RoutesResponse;
import com.optiroute.backend.dto.response.route.RouteDto;
import com.optiroute.backend.entity.transport.Transport;
import com.optiroute.backend.entity.transport.TransportEstimate;
import com.optiroute.backend.repository.transport.TransportEstimateRepository;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransportEstimateService {

	private final TransportEstimateRepository transportEstimateRepository;

	public TransportEstimateService(TransportEstimateRepository transportEstimateRepository) {
		this.transportEstimateRepository = transportEstimateRepository;
	}

	@Transactional
	public TransportEstimate saveEstimate(Transport transport, RoutesResponse response) {
		RouteDto fasterRoute = response.getRoutes().stream().min((route1, route2) -> Long.compare(route1.getDuration(),route2.getDuration()))
			.orElseThrow(() -> new IllegalArgumentException("No route available"));

		TransportEstimate estimate = new TransportEstimate();
		estimate.setTransportId(transport.getId());
		estimate.setDistanceMeters(fasterRoute.getDistanceMeters());
		estimate.setDurationSeconds(fasterRoute.getDuration());
		estimate.setPolyline(fasterRoute.getPolyline());

		estimate.setEstimatedFuelCost(BigDecimal.valueOf(fasterRoute.getCosts().getFuelCost()));
		estimate.setEstimatedTollCost(BigDecimal.valueOf(fasterRoute.getCosts().getTollCost()));

		return transportEstimateRepository.save(estimate);
	}

	@Transactional
	public TransportEstimate saveEstimate(Transport transport, RouteDto route) {

		TransportEstimate estimate = new TransportEstimate();

		estimate.setTransportId(transport.getId());

		estimate.setDistanceMeters(route.getDistanceMeters());
		estimate.setDurationSeconds(route.getDuration());
		estimate.setPolyline(route.getPolyline());

		estimate.setEstimatedFuelCost(BigDecimal.valueOf(route.getCosts().getFuelCost()));
		estimate.setEstimatedTollCost(BigDecimal.valueOf(route.getCosts().getTollCost()));

		return transportEstimateRepository.save(estimate);
	}

}