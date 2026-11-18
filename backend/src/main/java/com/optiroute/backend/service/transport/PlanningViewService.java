package com.optiroute.backend.service.transport;

import com.optiroute.backend.dto.request.transport.PlanningViewRequest;
import com.optiroute.backend.dto.response.transport.PlanningViewResponse;
import com.optiroute.backend.entity.transport.PlanningView;
import com.optiroute.backend.entity.transport.PlanningView.Row;
import com.optiroute.backend.entity.transport.PlanningView.RowType;
import com.optiroute.backend.repository.UserRepository;
import com.optiroute.backend.repository.driver.DriverRepository;
import com.optiroute.backend.repository.transport.PlanningViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanningViewService {
    private final PlanningViewRepository views;
    private final UserRepository users;
    private final DriverRepository drivers;

    public List<PlanningViewResponse> list(String email) {
        return views.findAllByOwnerEmailOrderByNameAscIdAsc(email).stream().map(this::response).toList();
    }

    public PlanningViewResponse get(String email, Long id) {
        return response(requireOwnedView(email, id));
    }

    @Transactional
    public PlanningViewResponse create(String email, PlanningViewRequest request) {
        PlanningView view = new PlanningView();
        view.setOwner(users.findByEmail(email).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Compte introuvable.")));
        apply(view, request);
        return response(views.save(view));
    }

    @Transactional
    public PlanningViewResponse update(String email, Long id, PlanningViewRequest request) {
        PlanningView view = requireOwnedView(email, id);
        apply(view, request);
        return response(view);
    }

    @Transactional
    public void delete(String email, Long id) {
        views.delete(requireOwnedView(email, id));
    }

    private PlanningView requireOwnedView(String email, Long id) {
        return views.findByIdAndOwnerEmail(id, email).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Vue du planning introuvable."));
    }

    private void apply(PlanningView view, PlanningViewRequest request) {
        List<Row> rows = request.rows().stream().map(row -> new Row(row.type(), row.driverId())).toList();
        Set<Row> seen = new HashSet<>();
        Set<Long> addedDriverIds = new HashSet<>();
        for (Row row : rows) {
            if ((row.type() == RowType.DRIVER) != (row.driverId() != null) || !seen.add(row)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les lignes de la vue sont invalides ou en double.");
            }
            if (row.type() == RowType.DRIVER && !view.getRows().contains(row)) {
                addedDriverIds.add(row.driverId());
            }
        }
        if (!addedDriverIds.isEmpty() && drivers.findAllById(addedDriverIds).size() != addedDriverIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un chauffeur sélectionné n’existe plus.");
        }
        view.setName(request.name().strip());
        view.getRows().clear();
        view.getRows().addAll(rows);
    }

    private PlanningViewResponse response(PlanningView view) {
        return new PlanningViewResponse(view.getId(), view.getName(), List.copyOf(view.getRows()));
    }
}
