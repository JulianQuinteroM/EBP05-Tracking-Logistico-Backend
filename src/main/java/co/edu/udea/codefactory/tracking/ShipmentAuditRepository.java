package co.edu.udea.codefactory.tracking;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentAuditRepository extends JpaRepository<ShipmentAudit, Long> {
    long countByShipmentId(Long shipmentId);
}
