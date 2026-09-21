package co.edu.udea.codefactory.tracking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShipmentAuditRepository extends JpaRepository<ShipmentAudit, Long> {
    long countByShipmentId(Long shipmentId);

    @Modifying
    @Query("delete from ShipmentAudit audit where audit.shipment.id = :shipmentId")
    int deleteAllByShipmentId(@Param("shipmentId") Long shipmentId);
}
