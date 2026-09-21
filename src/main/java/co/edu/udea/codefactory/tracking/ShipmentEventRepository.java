package co.edu.udea.codefactory.tracking;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShipmentEventRepository extends JpaRepository<ShipmentEvent, Long> {
    Optional<ShipmentEvent> findFirstByShipmentIdAndPublicVisibleTrueOrderByOccurredAtDescIdDesc(Long shipmentId);
    List<ShipmentEvent> findByShipmentIdOrderByOccurredAtAscIdAsc(Long shipmentId);
    long countByShipmentId(Long shipmentId);

    @Modifying
    @Query("delete from ShipmentEvent event where event.shipment.id = :shipmentId")
    int deleteAllByShipmentId(@Param("shipmentId") Long shipmentId);
}
