package com.project.gugumarket.service;

import com.project.gugumarket.dto.CategoryDto;
import com.project.gugumarket.entity.Category;
import com.project.gugumarket.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 카테고리 관련 비즈니스 로직을 처리하는 서비스
 * 카테고리 조회, 상품 개수 집계 등의 기능 담당
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categoryRepository;

    /**
     * 전체 카테고리 목록 조회 (DTO 반환)
     * 무한 재귀 방지를 위해 Entity를 직접 반환하지 않음
     */
    public List<CategoryDto> getAllCategories() {
        log.info("전체 카테고리 목록 조회");

        List<Category> categories = categoryRepository.findAllByOrderByCategoryIdAsc();

        log.info("카테고리 {}개 조회 완료", categories.size());

        // Entity를 DTO로 변환하여 반환
        return categories.stream()
                .map(CategoryDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 카테고리 목록 조회 (상품 개수 포함, DTO 반환)
     * 각 카테고리에 속한 상품의 개수를 함께 반환
     */
    public List<CategoryDto> getCategoriesWithProductCount() {
        log.info("카테고리 목록 조회 (상품 개수 포함)");

        List<Category> categories = categoryRepository.findAllByOrderByCategoryIdAsc();

        List<CategoryDto> categoryDtos = categories.stream()
                .map(category -> {
                    // 각 카테고리의 상품 개수 조회
                    long productCount = categoryRepository.countProductsByCategoryId(category.getCategoryId());
                    return CategoryDto.fromEntityWithCount(category, (int) productCount);
                })
                .collect(Collectors.toList());

        log.info("카테고리 {}개 조회 완료 (상품 개수 포함)", categoryDtos.size());

        return categoryDtos;
    }

    /**
     * 카테고리 상세 조회 (DTO 반환)
     * 특정 카테고리의 상세 정보와 상품 개수를 함께 반환
     */
    public CategoryDto getCategoryDetail(Long categoryId) {
        log.info("카테고리 상세 조회 - ID: {}", categoryId);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "카테고리를 찾을 수 없습니다. ID: " + categoryId));

        // 상품 개수 조회
        long productCount = categoryRepository.countProductsByCategoryId(categoryId);

        log.info("카테고리 조회 완료 - 이름: {}, 상품 개수: {}", category.getName(), productCount);

        return CategoryDto.fromEntityWithCount(category, (int) productCount);
    }

    /**
     * 카테고리 ID로 조회 (Entity 반환)
     * 내부 서비스 로직에서 사용 (외부 API 응답용 아님)
     */
    public Category getCategoryById(Long id) {
        log.debug("카테고리 Entity 조회 - ID: {}", id);

        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다. ID: " + id));
    }

    /**
     * 카테고리명으로 조회 (DTO 반환)
     * REST API 응답용
     */
    public CategoryDto getCategoryByName(String name) {
        log.info("카테고리 조회 - 이름: {}", name);

        Category category = categoryRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException(
                        "카테고리를 찾을 수 없습니다. 이름: " + name));

        log.info("카테고리 조회 완료 - ID: {}, 이름: {}", category.getCategoryId(), category.getName());

        return CategoryDto.fromEntity(category);
    }

    /**
     * 카테고리 개수 조회
     */
    public long getCategoryCount() {
        long count = categoryRepository.count();
        log.info("전체 카테고리 개수: {}", count);
        return count;
    }

    /**
     * 카테고리 존재 여부 확인
     */
    public boolean existsById(Long categoryId) {
        boolean exists = categoryRepository.existsById(categoryId);
        log.debug("카테고리 존재 여부 - ID: {}, 존재: {}", categoryId, exists);
        return exists;
    }
}
