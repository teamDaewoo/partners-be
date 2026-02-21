package com.dooring.domain.catalog.service;

import com.dooring.common.exception.BusinessException;
import com.dooring.common.exception.ErrorCode;
import com.dooring.domain.catalog.entity.Product;
import com.dooring.domain.catalog.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    /** 다른 도메인 서비스용 — Entity 반환 */
    @Transactional(readOnly = true)
    public Product findEntityById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
