package com.fedex.tracking.repository;

import com.fedex.tracking.model.entity.PersonalLogistico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonalLogisticoRepository extends JpaRepository<PersonalLogistico, Long> {
}

