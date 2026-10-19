package co.edu.udea.codefactory.tracking;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShipmentAuditRepository extends JpaRepository<ShipmentAudit, Long> {
    long countByShipmentId(Long shipmentId);
    List<ShipmentAudit> findByShipmentIdOrderByOccurredAtDescIdDesc(Long shipmentId);
    List<ShipmentAudit> findByShipmentReferenceOrderByOccurredAtDescIdDesc(Long shipmentReference);
    List<ShipmentAudit> findByOrderByOccurredAtDescIdDesc();

    @Modifying
    @Query("delete from ShipmentAudit audit where audit.shipment.id = :shipmentId")
    int deleteAllByShipmentId(@Param("shipmentId") Long shipmentId);
}
