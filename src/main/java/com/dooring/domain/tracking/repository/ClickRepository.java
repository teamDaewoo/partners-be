package com.dooring.domain.tracking.repository;

import com.dooring.domain.tracking.entity.Click;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 클릭 로그 Repository
 */
public interface ClickRepository extends JpaRepository<Click, Long> {

    /**
     * 클릭 토큰으로 조회
     * 구매 귀속 시 click_token으로 Click 찾기
     *
     * @param clickToken 클릭 토큰
     * @return 클릭
     */
    Optional<Click> findByClickToken(String clickToken);

    /**
     * 링크별 클릭 로그 조회 (시간 범위)
     * 어트리뷰션 윈도우 내 클릭 조회
     *
     * @param linkId 링크 ID
     * @param startTime 시작 시각
     * @param endTime 종료 시각
     * @return 클릭 목록
     */
    List<Click> findByLinkIdAndClickedAtBetween(Long linkId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 링크의 모든 클릭 조회
     *
     * @param linkId 링크 ID
     * @return 클릭 목록
     */
    List<Click> findAllByLinkId(Long linkId);

    /**
     * 캠페인별 클릭 수 조회
     *
     * @param campaignId 캠페인 ID
     * @return 클릭 수
     */
    long countByCampaignId(Long campaignId);

    /**
     * 링크 + 어트리뷰션 윈도우 내 최근 클릭 조회 (last-click)
     * 24시간 이내 가장 최근 클릭
     *
     * @param linkId 링크 ID
     * @param windowStart 윈도우 시작 시각 (현재 - 24h)
     * @return 가장 최근 클릭
     */
    @Query("SELECT c FROM Click c WHERE c.link.id = :linkId " +
           "AND c.clickedAt >= :windowStart " +
           "ORDER BY c.clickedAt DESC")
    List<Click> findRecentClicksWithinWindow(@Param("linkId") Long linkId,
                                              @Param("windowStart") LocalDateTime windowStart);

    /**
     * IP 주소 기반 중복 클릭 조회 (중복 필터링용)
     *
     * @param linkId 링크 ID
     * @param ipAddress IP 주소
     * @param since 시작 시각
     * @return 클릭 존재 여부
     */
    boolean existsByLinkIdAndIpAddressAndClickedAtAfter(Long linkId, String ipAddress, LocalDateTime since);
}
