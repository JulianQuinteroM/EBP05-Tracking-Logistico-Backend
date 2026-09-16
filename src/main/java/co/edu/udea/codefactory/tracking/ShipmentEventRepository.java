package co.edu.udea.codefactory.tracking;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentEventRepository extends JpaRepository<ShipmentEvent, Long> {
    Optional<ShipmentEvent> findFirstByShipmentIdAndPublicVisibleTrueOrderByOccurredAtDescIdDesc(Long shipmentId);
    List<ShipmentEvent> findByShipmentIdOrderByOccurredAtAscIdAsc(Long shipmentId);
    long countByShipmentId(Long shipmentId);
}
