package co.edu.udea.codefactory.tracking;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByTrackingCode(String trackingCode);

    @Query("""
        select s from Shipment s where :q = ''
        or lower(s.trackingCode) like lower(concat('%', :q, '%')) escape '!'
        or lower(s.senderName) like lower(concat('%', :q, '%')) escape '!'
        or lower(s.recipientName) like lower(concat('%', :q, '%')) escape '!'
        """)
    Page<Shipment> search(@Param("q") String query, Pageable pageable);
}
