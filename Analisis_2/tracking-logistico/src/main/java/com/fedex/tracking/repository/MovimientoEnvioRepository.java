package com.fedex.tracking.repository;

import com.fedex.tracking.model.entity.MovimientoEnvio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MovimientoEnvioRepository extends JpaRepository<MovimientoEnvio, Long> {
    List<MovimientoEnvio> findByEnvioIdOrderByFechaHoraAsc(Long envioId);
}
