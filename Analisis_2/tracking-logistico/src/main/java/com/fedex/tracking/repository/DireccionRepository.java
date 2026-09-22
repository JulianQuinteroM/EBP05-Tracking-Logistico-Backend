package com.fedex.tracking.repository;

import com.fedex.tracking.model.entity.Direccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DireccionRepository extends JpaRepository<Direccion, Long> {
    Optional<Direccion> findFirstByCalleIgnoreCaseAndNumeroIgnoreCaseAndCiudadIgnoreCaseAndDepartamentoProvinciaIgnoreCaseAndCodigoPostalIgnoreCaseAndPaisIgnoreCase(
            String calle, String numero, String ciudad, String departamentoProvincia,
            String codigoPostal, String pais);
}
