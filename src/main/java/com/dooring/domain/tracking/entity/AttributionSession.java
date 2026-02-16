package com.dooring.domain.tracking.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 클릭-세션 매핑 (24h 어트리뷰션 윈도우)
 * click_token과 클릭을 연결하는 세션
 */
@Entity
@Table(
    name = "attribution_sessions",
    indexes = {
        @Index(name = "attribution_sessions_expires_idx", columnList = "expires_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttributionSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * click_token과 동일하거나 파생된 세션 식별자
     */
    @Column(name = "session_token", nullable = false, unique = true)
    private String sessionToken;

    /**
     * 클릭 (도메인 내부 참조)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "click_id", nullable = false)
    private Click click;

    /**
     * 어트리뷰션 윈도우 만료 시각 (클릭 + 24시간)
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public AttributionSession(String sessionToken, Click click, LocalDateTime expiresAt) {
        this.sessionToken = sessionToken;
        this.click = click;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
    }
}
