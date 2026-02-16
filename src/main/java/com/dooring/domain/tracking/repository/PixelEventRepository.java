package com.dooring.domain.tracking.repository;

import com.dooring.domain.tracking.entity.PixelEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 픽셀 전환 이벤트 Repository
 */
public interface PixelEventRepository extends JpaRepository<PixelEvent, Long> {

    /**
     * 스토어 + 외부 주문 ID로 픽셀 이벤트 조회
     * 멱등성: 같은 주문에 대한 중복 픽셀 방지
     *
     * @param storeId 스토어 ID
     * @param externalOrderId 외부 주문 ID
     * @return 픽셀 이벤트
     */
    Optional<PixelEvent> findByStoreIdAndExternalOrderId(Long storeId, String externalOrderId);

    /**
     * 스토어 + 외부 주문 ID 픽셀 이벤트 존재 여부 확인
     * 멱등성 체크용
     *
     * @param storeId 스토어 ID
     * @param externalOrderId 외부 주문 ID
     * @return 존재 여부
     */
    boolean existsByStoreIdAndExternalOrderId(Long storeId, String externalOrderId);

    /**
     * 어트리뷰션 세션별 픽셀 이벤트 조회
     *
     * @param attributionSessionId 어트리뷰션 세션 ID
     * @return 픽셀 이벤트 목록
     */
    @Query("SELECT p FROM PixelEvent p WHERE p.attributionSession.id = :sessionId")
    List<PixelEvent> findAllByAttributionSessionId(@Param("sessionId") Long attributionSessionId);

    /**
     * 스토어별 픽셀 이벤트 조회 (기간별)
     *
     * @param storeId 스토어 ID
     * @param startTime 시작 시각
     * @param endTime 종료 시각
     * @return 픽셀 이벤트 목록
     */
    List<PixelEvent> findByStoreIdAndEventTimeBetween(Long storeId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 스토어의 모든 픽셀 이벤트 조회
     *
     * @param storeId 스토어 ID
     * @return 픽셀 이벤트 목록
     */
    List<PixelEvent> findAllByStoreId(Long storeId);

    /**
     * 어트리뷰션 세션이 없는 픽셀 이벤트 조회 (고아 픽셀)
     * 웹훅보다 먼저 도착한 픽셀 이벤트
     *
     * @return 고아 픽셀 이벤트 목록
     */
    @Query("SELECT p FROM PixelEvent p WHERE p.attributionSession IS NULL")
    List<PixelEvent> findOrphanPixelEvents();

    /**
     * 오래된 고아 픽셀 이벤트 수 조회
     * 모니터링용
     *
     * @param threshold 기준 시각 (e.g. 1시간 전)
     * @return 고아 픽셀 수
     */
    @Query("SELECT COUNT(p) FROM PixelEvent p WHERE p.attributionSession IS NULL AND p.eventTime < :threshold")
    long countOrphanPixelEventsBefore(@Param("threshold") LocalDateTime threshold);
}
