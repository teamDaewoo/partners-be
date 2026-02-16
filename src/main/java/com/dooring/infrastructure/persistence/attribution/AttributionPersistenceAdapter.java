package com.dooring.infrastructure.persistence.attribution;

import com.dooring.domain.attribution.aggregate.Attribution;
import com.dooring.domain.attribution.aggregate.CommissionLedger;
import com.dooring.domain.attribution.port.AttributionReader;
import com.dooring.domain.attribution.port.AttributionWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Attribution Persistence Adapter
 * Port 구현체 (도메인 모델 ↔ JPA Entity 변환)
 *
 * 책임:
 * 1. 원자성: Attribution + CommissionLedger 동시 저장 (CascadeType.ALL)
 * 2. 멱등성: 같은 orderId 중복 방지 (UNIQUE 제약 + 체크)
 * 3. 변환: 도메인 모델 ↔ JPA Entity
 */
@Repository
@RequiredArgsConstructor
public class AttributionPersistenceAdapter implements AttributionWriter, AttributionReader {

    private final AttributionJpaRepository attributionJpaRepository;

    // ==================== AttributionWriter ====================

    /**
     * Attribution 저장 (중복 시 예외 발생)
     *
     * @throws IllegalStateException 이미 존재하는 orderId
     * @throws DataIntegrityViolationException DB UNIQUE 제약 위반
     */
    @Override
    @Transactional
    public void save(Attribution attribution) {
        // 멱등성 체크: 사전 검증
        if (attributionJpaRepository.existsByOrderId(attribution.getOrderId())) {
            throw new IllegalStateException(
                String.format("이미 귀속이 존재하는 주문입니다. orderId=%d", attribution.getOrderId())
            );
        }

        // 도메인 → JPA Entity 변환
        AttributionJpaEntity entity = toEntity(attribution);

        // 저장 (CascadeType.ALL로 CommissionLedger도 함께 원자적 저장)
        attributionJpaRepository.save(entity);
    }

    /**
     * Attribution 저장 (멱등 처리 - 중복 시 기존 반환)
     *
     * 동시성 환경에서도 안전:
     * - UNIQUE 제약으로 DB 레벨 보장
     * - 충돌 시 기존 데이터 조회 후 반환
     */
    @Override
    @Transactional
    public Attribution saveIdempotent(Attribution attribution) {
        try {
            // 이미 존재하는지 먼저 확인
            Optional<AttributionJpaEntity> existing =
                attributionJpaRepository.findByOrderId(attribution.getOrderId());

            if (existing.isPresent()) {
                // 이미 존재하면 기존 것 반환
                return toDomain(existing.get());
            }

            // 없으면 저장
            AttributionJpaEntity entity = toEntity(attribution);
            AttributionJpaEntity saved = attributionJpaRepository.save(entity);
            return toDomain(saved);

        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 UNIQUE 제약 위반 시 기존 데이터 반환
            // (Race Condition 대응)
            AttributionJpaEntity existing =
                attributionJpaRepository.findByOrderId(attribution.getOrderId())
                    .orElseThrow(() -> new IllegalStateException(
                        "UNIQUE 제약 위반했지만 데이터를 찾을 수 없음 (DB 정합성 문제)", e
                    ));
            return toDomain(existing);
        }
    }

    // ==================== AttributionReader ====================

    @Override
    @Transactional(readOnly = true)
    public Optional<Attribution> findById(Long id) {
        return attributionJpaRepository.findById(id)
            .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Attribution> findByOrderId(Long orderId) {
        return attributionJpaRepository.findByOrderId(orderId)
            .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByOrderId(Long orderId) {
        return attributionJpaRepository.existsByOrderId(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Attribution> findByClickId(Long clickId) {
        return attributionJpaRepository.findByClickId(clickId)
            .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Attribution> findAllByCampaignId(Long campaignId) {
        return attributionJpaRepository.findAllByCampaignId(campaignId)
            .stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Attribution> findAllByCreatorId(Long creatorId) {
        return attributionJpaRepository.findAllByCreatorId(creatorId)
            .stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Attribution> findAllBySellerId(Long sellerId) {
        return attributionJpaRepository.findAllBySellerId(sellerId)
            .stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    // ==================== 변환 로직 ====================

    /**
     * 도메인 모델 → JPA Entity 변환
     *
     * 양방향 관계 자동 설정 (AttributionJpaEntity 생성자에서 처리)
     */
    private AttributionJpaEntity toEntity(Attribution attribution) {
        CommissionLedger ledger = attribution.getCommissionLedger();

        // CommissionLedger Entity 생성
        CommissionLedgerJpaEntity ledgerEntity = new CommissionLedgerJpaEntity(
            ledger.getCampaignId(),
            ledger.getCreatorId(),
            ledger.getSellerId(),
            ledger.getAmount(),
            ledger.getStatus(),
            ledger.getCreatedAt(),
            ledger.getUpdatedAt(),
            ledger.getConfirmedAt(),
            ledger.getPaidAt()
        );

        // Attribution Entity 생성 (양방향 관계 자동 설정됨)
        return new AttributionJpaEntity(
            attribution.getOrderId(),
            attribution.getClickId(),
            attribution.getCampaignId(),
            attribution.getAttributedAt(),
            ledgerEntity
        );
    }

    /**
     * JPA Entity → 도메인 모델 변환 (재구성)
     *
     * JPA Entity에서 모든 데이터를 읽어 도메인 Aggregate를 재구성
     */
    private Attribution toDomain(AttributionJpaEntity entity) {
        CommissionLedgerJpaEntity ledgerEntity = entity.getCommissionLedger();

        // CommissionLedger 재구성 (도메인 재구성 생성자 사용)
        CommissionLedger ledger = new CommissionLedger(
            ledgerEntity.getId(),
            ledgerEntity.getCampaignId(),
            ledgerEntity.getCreatorId(),
            ledgerEntity.getSellerId(),
            ledgerEntity.getAmount(),
            ledgerEntity.getStatus(),
            ledgerEntity.getCreatedAt(),
            ledgerEntity.getUpdatedAt(),
            ledgerEntity.getConfirmedAt(),
            ledgerEntity.getPaidAt()
        );

        // Attribution 재구성 (도메인 재구성 생성자 사용)
        return new Attribution(
            entity.getId(),
            entity.getOrderId(),
            entity.getClickId(),
            entity.getCampaignId(),
            entity.getAttributedAt(),
            ledger
        );
    }
}
