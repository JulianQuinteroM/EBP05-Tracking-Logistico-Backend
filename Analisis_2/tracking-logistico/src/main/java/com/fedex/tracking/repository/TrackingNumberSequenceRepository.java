package com.fedex.tracking.repository;

import com.fedex.tracking.model.entity.TrackingNumberSequence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackingNumberSequenceRepository extends JpaRepository<TrackingNumberSequence, Long> {
}
