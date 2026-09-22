package com.fedex.tracking.repository;

import com.fedex.tracking.model.entity.Envio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EnvioRepository extends JpaRepository<Envio, Long>, JpaSpecificationExecutor<Envio> {
    
    Optional<Envio> findByTrackingNumberIgnoreCase(String trackingNumber);
    
    Boolean existsByTrackingNumberIgnoreCase(String trackingNumber);
}

