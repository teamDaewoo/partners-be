package com.dooring.domain.dashboard.port;

import com.dooring.domain.dashboard.querymodel.CreatorReport;
import com.dooring.domain.dashboard.querymodel.LinkPerformance;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 크리에이터 리포트 조회 Port (읽기 전용)
 * 구현체는 infrastructure/persistence/dashboard/에 위치
 */
public interface CreatorReportQueryPort {

    /**
     * 크리에이터 실적 리포트 조회
     *
     * @param creatorId 크리에이터 ID
     * @return 크리에이터 실적 리포트
     */
    Optional<CreatorReport> findCreatorReport(Long creatorId);

    /**
     * 기간별 크리에이터 실적 리포트 조회
     *
     * @param creatorId 크리에이터 ID
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 크리에이터 실적 리포트
     */
    Optional<CreatorReport> findCreatorReportByPeriod(Long creatorId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 크리에이터의 링크별 성과 목록 조회
     *
     * @param creatorId 크리에이터 ID
     * @return 링크별 성과 목록
     */
    List<LinkPerformance> findLinkPerformancesByCreator(Long creatorId);

    /**
     * 기간별 크리에이터의 링크별 성과 목록 조회
     *
     * @param creatorId 크리에이터 ID
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 링크별 성과 목록
     */
    List<LinkPerformance> findLinkPerformancesByCreatorAndPeriod(Long creatorId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 특정 링크의 성과 조회
     *
     * @param linkId 링크 ID
     * @return 링크 성과
     */
    Optional<LinkPerformance> findLinkPerformance(Long linkId);
}
