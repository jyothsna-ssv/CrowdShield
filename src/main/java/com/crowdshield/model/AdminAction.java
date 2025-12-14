package com.crowdshield.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "admin_actions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "content_id", nullable = false)
    private UUID contentId;

    @Column(name = "admin_id", nullable = false)
    private String adminId;

    @Column(name = "previous_label")
    private String previousLabel;

    @Column(name = "new_label")
    private String newLabel;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", insertable = false, updatable = false)
    private Content content;
}

