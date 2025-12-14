package com.crowdshield.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "moderation_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "toxicity_threshold", nullable = false)
    @Builder.Default
    private Float toxicityThreshold = 0.7f;

    @Column(name = "hate_threshold", nullable = false)
    @Builder.Default
    private Float hateThreshold = 0.6f;

    @Column(name = "sexual_threshold", nullable = false)
    @Builder.Default
    private Float sexualThreshold = 0.6f;

    @Column(name = "violence_threshold", nullable = false)
    @Builder.Default
    private Float violenceThreshold = 0.6f;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

