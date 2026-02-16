package com.dooring.infrastructure.persistence.dashboard;

import com.dooring.domain.dashboard.port.SellerReportQueryPort;
import com.dooring.domain.dashboard.querymodel.CampaignPerformance;
import com.dooring.domain.dashboard.querymodel.SellerReport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * SellerReport QueryPort 구현체
 *
 * 여러 테이블을 조인해서 셀러 실적 데이터를 직접 조회
 * - Native Query로 성능 최적화
 * - 읽기 전용, 동시성 고려 불필요
 */
@Repository
@RequiredArgsConstructor
public class SellerReportQueryAdapter implements SellerReportQueryPort {

    @PersistenceContext
    private final EntityManager em;

    @Override
    @Transactional(readOnly = true)
    public Optional<SellerReport> findSellerReport(Long sellerId) {
        String sql = """
            SELECT
                s.id AS seller_id,
                s.name,
                COUNT(DISTINCT cam.id) AS total_campaigns,
                COUNT(DISTINCT a.id) AS total_conversions,
                COALESCE(SUM(CASE WHEN cl.status = 'PENDING' THEN cl.amount ELSE 0 END), 0) AS pending_commission,
                COALESCE(SUM(CASE WHEN cl.status = 'CONFIRMED' THEN cl.amount ELSE 0 END), 0) AS confirmed_commission,
                COALESCE(SUM(CASE WHEN cl.status = 'PAID' THEN cl.amount ELSE 0 END), 0) AS paid_commission
            FROM sellers s
            LEFT JOIN campaigns cam ON cam.seller_id = s.id
            LEFT JOIN attributions a ON a.campaign_id = cam.id
            LEFT JOIN commission_ledgers cl ON cl.attribution_id = a.id
            WHERE s.id = :sellerId
            GROUP BY s.id, s.name
        """;

        Query query = em.createNativeQuery(sql);
        query.setParameter("sellerId", sellerId);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        if (results.isEmpty()) {
            return Optional.empty();
        }

        Object[] row = results.get(0);

        // 캠페인별 성과 조회
        List<CampaignPerformance> campaignPerformances = findCampaignPerformancesBySeller(sellerId);

        SellerReport report = new SellerReport(
            ((Number) row[0]).longValue(),      // seller_id
            (String) row[1],                     // name
            ((Number) row[2]).longValue(),       // total_campaigns
            ((Number) row[3]).longValue(),       // total_conversions
            (BigDecimal) row[4],                 // pending_commission
            (BigDecimal) row[5],                 // confirmed_commission
            (BigDecimal) row[6],                 // paid_commission
            campaignPerformances
        );

        return Optional.of(report);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SellerReport> findSellerReportByPeriod(Long sellerId,
                                                           LocalDateTime startDate,
                                                           LocalDateTime endDate) {
        String sql = """
            SELECT
                s.id AS seller_id,
                s.name,
                COUNT(DISTINCT cam.id) AS total_campaigns,
                COUNT(DISTINCT a.id) AS total_conversions,
                COALESCE(SUM(CASE WHEN cl.status = 'PENDING' THEN cl.amount ELSE 0 END), 0) AS pending_commission,
                COALESCE(SUM(CASE WHEN cl.status = 'CONFIRMED' THEN cl.amount ELSE 0 END), 0) AS confirmed_commission,
                COALESCE(SUM(CASE WHEN cl.status = 'PAID' THEN cl.amount ELSE 0 END), 0) AS paid_commission
            FROM sellers s
            LEFT JOIN campaigns cam ON cam.seller_id = s.id
            LEFT JOIN attributions a ON a.campaign_id = cam.id
                AND a.attributed_at BETWEEN :startDate AND :endDate
            LEFT JOIN commission_ledgers cl ON cl.attribution_id = a.id
            WHERE s.id = :sellerId
            GROUP BY s.id, s.name
        """;

        Query query = em.createNativeQuery(sql);
        query.setParameter("sellerId", sellerId);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        if (results.isEmpty()) {
            return Optional.empty();
        }

        Object[] row = results.get(0);

        List<CampaignPerformance> campaignPerformances =
            findCampaignPerformancesBySellerAndPeriod(sellerId, startDate, endDate);

        SellerReport report = new SellerReport(
            ((Number) row[0]).longValue(),
            (String) row[1],
            ((Number) row[2]).longValue(),
            ((Number) row[3]).longValue(),
            (BigDecimal) row[4],
            (BigDecimal) row[5],
            (BigDecimal) row[6],
            campaignPerformances
        );

        return Optional.of(report);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CampaignPerformance> findCampaignPerformancesBySeller(Long sellerId) {
        String sql = """
            SELECT
                cam.id AS campaign_id,
                p.name AS product_name,
                cam.commission_amount,
                cam.starts_at,
                cam.ends_at,
                COUNT(DISTINCT l.id) AS total_links,
                COUNT(DISTINCT cl.id) AS total_clicks,
                COUNT(DISTINCT a.id) AS total_conversions,
                COALESCE(SUM(comm.amount), 0) AS total_commission
            FROM campaigns cam
            JOIN products p ON cam.product_id = p.id
            LEFT JOIN links l ON l.product_id = cam.product_id
            LEFT JOIN clicks cl ON cl.link_id = l.id AND cl.campaign_id = cam.id
            LEFT JOIN attributions a ON a.campaign_id = cam.id
            LEFT JOIN commission_ledgers comm ON comm.attribution_id = a.id
            WHERE cam.seller_id = :sellerId
            GROUP BY cam.id, p.name, cam.commission_amount, cam.starts_at, cam.ends_at
            ORDER BY cam.starts_at DESC
        """;

        Query query = em.createNativeQuery(sql);
        query.setParameter("sellerId", sellerId);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
            .map(row -> new CampaignPerformance(
                ((Number) row[0]).longValue(),                          // campaign_id
                (String) row[1],                                         // product_name
                (BigDecimal) row[2],                                     // commission_amount
                ((Timestamp) row[3]).toLocalDateTime(),                  // starts_at
                ((Timestamp) row[4]).toLocalDateTime(),                  // ends_at
                ((Number) row[5]).longValue(),                           // total_links
                ((Number) row[6]).longValue(),                           // total_clicks
                ((Number) row[7]).longValue(),                           // total_conversions
                (BigDecimal) row[8]                                      // total_commission
            ))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CampaignPerformance> findCampaignPerformancesBySellerAndPeriod(Long sellerId,
                                                                               LocalDateTime startDate,
                                                                               LocalDateTime endDate) {
        String sql = """
            SELECT
                cam.id AS campaign_id,
                p.name AS product_name,
                cam.commission_amount,
                cam.starts_at,
                cam.ends_at,
                COUNT(DISTINCT l.id) AS total_links,
                COUNT(DISTINCT cl.id) AS total_clicks,
                COUNT(DISTINCT a.id) AS total_conversions,
                COALESCE(SUM(comm.amount), 0) AS total_commission
            FROM campaigns cam
            JOIN products p ON cam.product_id = p.id
            LEFT JOIN links l ON l.product_id = cam.product_id
            LEFT JOIN clicks cl ON cl.link_id = l.id
                AND cl.campaign_id = cam.id
                AND cl.clicked_at BETWEEN :startDate AND :endDate
            LEFT JOIN attributions a ON a.campaign_id = cam.id
                AND a.attributed_at BETWEEN :startDate AND :endDate
            LEFT JOIN commission_ledgers comm ON comm.attribution_id = a.id
            WHERE cam.seller_id = :sellerId
            GROUP BY cam.id, p.name, cam.commission_amount, cam.starts_at, cam.ends_at
            ORDER BY cam.starts_at DESC
        """;

        Query query = em.createNativeQuery(sql);
        query.setParameter("sellerId", sellerId);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
            .map(row -> new CampaignPerformance(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (BigDecimal) row[2],
                ((Timestamp) row[3]).toLocalDateTime(),
                ((Timestamp) row[4]).toLocalDateTime(),
                ((Number) row[5]).longValue(),
                ((Number) row[6]).longValue(),
                ((Number) row[7]).longValue(),
                (BigDecimal) row[8]
            ))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CampaignPerformance> findCampaignPerformance(Long campaignId) {
        String sql = """
            SELECT
                cam.id AS campaign_id,
                p.name AS product_name,
                cam.commission_amount,
                cam.starts_at,
                cam.ends_at,
                COUNT(DISTINCT l.id) AS total_links,
                COUNT(DISTINCT cl.id) AS total_clicks,
                COUNT(DISTINCT a.id) AS total_conversions,
                COALESCE(SUM(comm.amount), 0) AS total_commission
            FROM campaigns cam
            JOIN products p ON cam.product_id = p.id
            LEFT JOIN links l ON l.product_id = cam.product_id
            LEFT JOIN clicks cl ON cl.link_id = l.id AND cl.campaign_id = cam.id
            LEFT JOIN attributions a ON a.campaign_id = cam.id
            LEFT JOIN commission_ledgers comm ON comm.attribution_id = a.id
            WHERE cam.id = :campaignId
            GROUP BY cam.id, p.name, cam.commission_amount, cam.starts_at, cam.ends_at
        """;

        Query query = em.createNativeQuery(sql);
        query.setParameter("campaignId", campaignId);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        if (results.isEmpty()) {
            return Optional.empty();
        }

        Object[] row = results.get(0);

        CampaignPerformance performance = new CampaignPerformance(
            ((Number) row[0]).longValue(),
            (String) row[1],
            (BigDecimal) row[2],
            ((Timestamp) row[3]).toLocalDateTime(),
            ((Timestamp) row[4]).toLocalDateTime(),
            ((Number) row[5]).longValue(),
            ((Number) row[6]).longValue(),
            ((Number) row[7]).longValue(),
            (BigDecimal) row[8]
        );

        return Optional.of(performance);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CampaignPerformance> findActiveCampaignPerformancesBySeller(Long sellerId) {
        String sql = """
            SELECT
                cam.id AS campaign_id,
                p.name AS product_name,
                cam.commission_amount,
                cam.starts_at,
                cam.ends_at,
                COUNT(DISTINCT l.id) AS total_links,
                COUNT(DISTINCT cl.id) AS total_clicks,
                COUNT(DISTINCT a.id) AS total_conversions,
                COALESCE(SUM(comm.amount), 0) AS total_commission
            FROM campaigns cam
            JOIN products p ON cam.product_id = p.id
            LEFT JOIN links l ON l.product_id = cam.product_id
            LEFT JOIN clicks cl ON cl.link_id = l.id AND cl.campaign_id = cam.id
            LEFT JOIN attributions a ON a.campaign_id = cam.id
            LEFT JOIN commission_ledgers comm ON comm.attribution_id = a.id
            WHERE cam.seller_id = :sellerId
                AND cam.is_active = true
                AND cam.starts_at <= :now
                AND cam.ends_at >= :now
            GROUP BY cam.id, p.name, cam.commission_amount, cam.starts_at, cam.ends_at
            ORDER BY cam.starts_at DESC
        """;

        Query query = em.createNativeQuery(sql);
        query.setParameter("sellerId", sellerId);
        query.setParameter("now", LocalDateTime.now());

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
            .map(row -> new CampaignPerformance(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (BigDecimal) row[2],
                ((Timestamp) row[3]).toLocalDateTime(),
                ((Timestamp) row[4]).toLocalDateTime(),
                ((Number) row[5]).longValue(),
                ((Number) row[6]).longValue(),
                ((Number) row[7]).longValue(),
                (BigDecimal) row[8]
            ))
            .toList();
    }
}
