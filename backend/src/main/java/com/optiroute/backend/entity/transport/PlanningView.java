package com.optiroute.backend.entity.transport;

import com.optiroute.backend.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "planning_view", indexes = @Index(name = "idx_planning_view_owner", columnList = "owner_id"))
@Getter
@Setter
public class PlanningView {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User owner;

    @Column(nullable = false, length = 100)
    private String name;

    @ElementCollection
    @CollectionTable(name = "planning_view_row", joinColumns = @JoinColumn(name = "view_id", nullable = false))
    // Let Hibernate delete rows first, including on schemas with a non-cascading foreign key.
    @OrderColumn(name = "row_position", nullable = false)
    private List<Row> rows = new ArrayList<>();

    public enum RowType { DRIVER, UNUSED_VEHICLES }

    @Embeddable
    public record Row(
        @Enumerated(EnumType.STRING) @Column(name = "row_type", nullable = false, length = 24) RowType type,
        @Column(name = "driver_id") Long driverId
    ) {}
}
