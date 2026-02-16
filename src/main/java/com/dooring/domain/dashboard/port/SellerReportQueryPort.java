package com.dooring.domain.dashboard.port;

import com.dooring.domain.dashboard.querymodel.CampaignPerformance;
import com.dooring.domain.dashboard.querymodel.SellerReport;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 셀러 리포트 조회 Port (읽기 전용)
 * 구현체는 infrastructure/persistence/dashboard/에 위치
 */
public interface SellerReportQueryPort {

    /**
     * 셀러 지급 현황 리포트 조회
     *
     * @param sellerId 셀러 ID
     * @return 셀러 지급 현황 리포트
     */
    Optional<SellerReport> findSellerReport(Long sellerId);

    /**
     * 기간별 셀러 지급 현황 리포트 조회
     *
     * @param sellerId 셀러 ID
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 셀러 지급 현황 리포트
     */
    Optional<SellerReport> findSellerReportByPeriod(Long sellerId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 셀러의 캠페인별 성과 목록 조회
     *
     * @param sellerId 셀러 ID
     * @return 캠페인별 성과 목록
     */
    List<CampaignPerformance> findCampaignPerformancesBySeller(Long sellerId);

    /**
     * 기간별 셀러의 캠페인별 성과 목록 조회
     *
     * @param sellerId 셀러 ID
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 캠페인별 성과 목록
     */
    List<CampaignPerformance> findCampaignPerformancesBySellerAndPeriod(Long sellerId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 특정 캠페인의 성과 조회
     *
     * @param campaignId 캠페인 ID
     * @return 캠페인 성과
     */
    Optional<CampaignPerformance> findCampaignPerformance(Long campaignId);

    /**
     * 셀러의 활성 캠페인 성과 목록 조회
     *
     * @param sellerId 셀러 ID
     * @return 활성 캠페인 성과 목록
     */
    List<CampaignPerformance> findActiveCampaignPerformancesBySeller(Long sellerId);
}
