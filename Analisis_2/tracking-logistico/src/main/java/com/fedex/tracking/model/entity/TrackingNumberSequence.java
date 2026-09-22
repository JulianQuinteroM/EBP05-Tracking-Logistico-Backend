package com.fedex.tracking.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tracking_number_sequences")
@Getter
@NoArgsConstructor
public class TrackingNumberSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tracking_number_sequence")
    @SequenceGenerator(
            name = "tracking_number_sequence",
            sequenceName = "tracking_number_seq",
            allocationSize = 1
    )
    private Long id;
}
