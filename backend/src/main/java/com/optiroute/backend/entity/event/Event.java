package com.optiroute.backend.entity.event;

import com.optiroute.backend.entity.EntityUtils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import com.optiroute.backend.entity.transport.Route;
import com.optiroute.backend.entity.cost.CostParameter;
import com.optiroute.backend.entity.vehicle.Tractor;
import com.optiroute.backend.entity.vehicle.SemiTrailer;

@Entity
@Table(name = "event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event extends EntityUtils {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "point_of_interest_id")
    private PointOfInterest pointOfInterest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cost_parameter_id", nullable = false)
    private CostParameter costParameter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tractor_id")
    private Tractor tractor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semi_trailer_id")
    private SemiTrailer semiTrailer;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;
}
