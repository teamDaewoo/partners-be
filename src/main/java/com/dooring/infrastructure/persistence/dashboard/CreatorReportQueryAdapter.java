package com.dooring.infrastructure.persistence.dashboard;

import com.dooring.domain.dashboard.port.CreatorReportQueryPort;
import com.dooring.domain.dashboard.querymodel.CreatorReport;
import com.dooring.domain.dashboard.querymodel.LinkPerformance;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * CreatorReport QueryPort 구현체
 *
 * 여러 테이블을 조인해서 집계 데이터를 직접 조회
 * - Native Query로 성능 최적화
 * - 다른 도메인 Service 호출 금지
 */
@Repository
@RequiredArgsConstructor
public class CreatorReportQueryAdapter implements CreatorReportQueryPort {

    @PersistenceContext
    private final EntityManager em;

    @Override
    public Optional<CreatorReport> findCreatorReport(Long creatorId) {
        // Native Query로 여러 테이블 조인/집계
        String sql = """
            SELECT
                c.id AS creator_id,
                c.nickname,
                COUNT(DISTINCT l.id) AS total_links,
                COUNT(DISTINCT cl.id) AS total_clicks,
                COUNT(DISTINCT a.id) AS total_conversions,
                COALESCE(SUM(CASE WHEN comm.status = 'PENDING' THEN comm.amount ELSE 0 END), 0) AS pending_commission,
                COALESCE(SUM(CASE WHEN comm.status = 'CONFIRMED' THEN comm.amount ELSE 0 END), 0) AS confirmed_commission,
                COALESCE(SUM(CASE WHEN comm.status = 'PAID' THEN comm.amount ELSE 0 END), 0) AS paid_commission
            FROM creators c
            LEFT JOIN links l ON l.creator_id = c.id
            LEFT JOIN clicks cl ON cl.link_id = l.id
            LEFT JOIN attributions a ON a.click_id = cl.id
            LEFT JOIN commission_ledgers comm ON comm.attribution_id = a.id
            WHERE c.id = :creatorId
            GROUP BY c.id, c.nickname
        """;

        Query query = em.createNativeQuery(sql);
        query.setParameter("creatorId", creatorId);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        if (results.isEmpty()) {
            return Optional.empty();
        }

        Object[] row = results.get(0);

        // 링크별 성과 조회
        List<LinkPerformance> linkPerformances = findLinkPerformancesByCreator(creatorId);

        // QueryModel 생성
        CreatorReport report = new CreatorReport(
            ((Number) row[0]).longValue(),      // creator_id
            (String) row[1],                     // nickname
            ((Number) row[2]).longValue(),       // total_links
            ((Number) row[3]).longValue(),       // total_clicks
            ((Number) row[4]).longValue(),       // total_conversions
            (BigDecimal) row[5],                 // pending_commission
            (BigDecimal) row[6],                 // confirmed_commission
            (BigDecimal) row[7],                 // paid_commission
            linkPerformances
        );

        return Optional.of(report);
    }

    @Override
    public Optional<CreatorReport> findCreatorReportByPeriod(Long creatorId,
                                                             LocalDateTime startDate,
                                                             LocalDateTime endDate) {
        String sql = """
            SELECT
                c.id AS creator_id,
                c.nickname,
                COUNT(DISTINCT l.id) AS total_links,
                COUNT(DISTINCT cl.id) AS total_clicks,
                COUNT(DISTINCT a.id) AS total_conversions,
                COALESCE(SUM(CASE WHEN comm.status = 'PENDING' THEN comm.amount ELSE 0 END), 0) AS pending_commission,
                COALESCE(SUM(CASE WHEN comm.status = 'CONFIRMED' THEN comm.amount ELSE 0 END), 0) AS confirmed_commission,
                COALESCE(SUM(CASE WHEN comm.status = 'PAID' THEN comm.amount ELSE 0 END), 0) AS paid_commission
            FROM creators c
            LEFT JOIN links l ON l.creator_id = c.id
            LEFT JOIN clicks cl ON cl.link_id = l.id AND cl.clicked_at BETWEEN :startDate AND :endDate
            LEFT JOIN attributions a ON a.click_id = cl.id AND a.attributed_at BETWEEN :startDate AND :endDate
            LEFT JOIN commission_ledgers comm ON comm.attribution_id = a.id
            WHERE c.id = :creatorId
            GROUP BY c.id, c.nickname
        """;

        Query query = em.createNativeQuery(sql);
        query.setParameter("creatorId", creatorId);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        if (results.isEmpty()) {
            return Optional.empty();
        }

        Object[] row = results.get(0);

        List<LinkPerformance> linkPerformances =
            findLinkPerformancesByCreatorAndPeriod(creatorId, startDate, endDate);

        CreatorReport report = new CreatorReport(
            ((Number) row[0]).longValue(),
            (String) row[1],
            ((Number) row[2]).longValue(),
            ((Number) row[3]).longValue(),
            ((Number) row[4]).longValue(),
            (BigDecimal) row[5],
            (BigDecimal) row[6],
            (BigDecimal) row[7],
            linkPerformances
        );

        return Optional.of(report);
    }

    @Override
    public List<LinkPerformance> findLinkPerformancesByCreator(Long creatorId) {
        String sql = """
            SELECT
                l.id AS link_id,
                l.short_code,
                p.name AS product_name,
                COUNT(DISTINCT cl.id) AS clicks,
                COUNT(DISTINCT a.id) AS conversions,
                COALESCE(SUM(comm.amount), 0) AS total_commission
            FROM links l
            JOIN products p ON l.product_id = p.id
            LEFT JOIN clicks cl ON cl.link_id = l.id
            LEFT JOIN attributions a ON a.click_id = cl.id
            LEFT JOIN commission_ledgers comm ON comm.attribution_id = a.id
            WHERE l.creator_id = :creatorId
            GROUP BY l.id, l.short_code, p.name
            ORDER BY total_commission DESC
        """;

        Query query = em.createNativeQuery(sql);
        query.setParameter("creatorId", creatorId);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
            .map(row -> new LinkPerformance(
                ((Number) row[0]).longValue(),    // link_id
                (String) row[1],                   // short_code
                (String) row[2],                   // product_name
                ((Number) row[3]).longValue(),     // clicks
                ((Number) row[4]).longValue(),     // conversions
                (BigDecimal) row[5]                // total_commission
            ))
            .toList();
    }

    @Override
    public List<LinkPerformance> findLinkPerformancesByCreatorAndPeriod(Long creatorId,
                                                                        LocalDateTime startDate,
                                                                        LocalDateTime endDate) {
        String sql = """
            SELECT
                l.id AS link_id,
                l.short_code,
                p.name AS product_name,
                COUNT(DISTINCT cl.id) AS clicks,
                COUNT(DISTINCT a.id) AS conversions,
                COALESCE(SUM(comm.amount), 0) AS total_commission
            FROM links l
            JOIN products p ON l.product_id = p.id
            LEFT JOIN clicks cl ON cl.link_id = l.id AND cl.clicked_at BETWEEN :startDate AND :endDate
            LEFT JOIN attributions a ON a.click_id = cl.id AND a.attributed_at BETWEEN :startDate AND :endDate
            LEFT JOIN commission_ledgers comm ON comm.attribution_id = a.id
            WHERE l.creator_id = :creatorId
            GROUP BY l.id, l.short_code, p.name
            ORDER BY total_commission DESC
        """;

        Query query = em.createNativeQuery(sql);
        query.setParameter("creatorId", creatorId);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
            .map(row -> new LinkPerformance(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                ((Number) row[3]).longValue(),
                ((Number) row[4]).longValue(),
                (BigDecimal) row[5]
            ))
            .toList();
    }

    @Override
    public Optional<LinkPerformance> findLinkPerformance(Long linkId) {
        String sql = """
            SELECT
                l.id AS link_id,
                l.short_code,
                p.name AS product_name,
                COUNT(DISTINCT cl.id) AS clicks,
                COUNT(DISTINCT a.id) AS conversions,
                COALESCE(SUM(comm.amount), 0) AS total_commission
            FROM links l
            JOIN products p ON l.product_id = p.id
            LEFT JOIN clicks cl ON cl.link_id = l.id
            LEFT JOIN attributions a ON a.click_id = cl.id
            LEFT JOIN commission_ledgers comm ON comm.attribution_id = a.id
            WHERE l.id = :linkId
            GROUP BY l.id, l.short_code, p.name
        """;

        Query query = em.createNativeQuery(sql);
        query.setParameter("linkId", linkId);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        if (results.isEmpty()) {
            return Optional.empty();
        }

        Object[] row = results.get(0);

        LinkPerformance performance = new LinkPerformance(
            ((Number) row[0]).longValue(),
            (String) row[1],
            (String) row[2],
            ((Number) row[3]).longValue(),
            ((Number) row[4]).longValue(),
            (BigDecimal) row[5]
        );

        return Optional.of(performance);
    }
}
