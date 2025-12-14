package com.crowdshield.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "moderation_results")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "content_id", nullable = false)
    private UUID contentId;

    @Column(name = "toxicity_score")
    private Float toxicityScore;

    @Column(name = "hate_score")
    private Float hateScore;

    @Column(name = "sexual_score")
    private Float sexualScore;

    @Column(name = "violence_score")
    private Float violenceScore;

    @Column(name = "overall_label")
    @Enumerated(EnumType.STRING)
    private ModerationLabel overallLabel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_response", columnDefinition = "jsonb")
    private Map<String, Object> rawResponse;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", insertable = false, updatable = false)
    private Content content;

    public enum ModerationLabel {
        SAFE, FLAGGED
    }
}

