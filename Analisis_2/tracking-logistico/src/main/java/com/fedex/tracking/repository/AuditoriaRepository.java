package com.fedex.tracking.repository;

import com.fedex.tracking.model.entity.RegistroAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {
}

