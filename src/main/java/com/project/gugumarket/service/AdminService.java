package com.project.gugumarket.service;

import com.project.gugumarket.entity.Product;
import com.project.gugumarket.repository.ProductRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j

public class AdminService {

    private final ProductRepository productRepository;

    @Transactional
    public void forceDeleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + id));

        log.info("상품 강제 삭제 시작 - ID: {}, 상품명: {}", id, product.getCategory());

        productRepository.delete(product);

        log.info("상품 강제 삭제 완료 - ID: {}", id);
    }
}