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

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categoryRepository;

    /**
     * 전체 카테고리 목록 조회
     */
    public List<CategoryDto> getAllCategories() {
        log.info("전체 카테고리 목록 조회");

        List<Category> categories = categoryRepository.findAllByOrderByCategoryIdAsc();

        log.info("카테고리 {}개 조회 완료", categories.size());

        return categories.stream()
                .map(CategoryDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 카테고리 목록 조회 (상품 개수 포함)
     */
    public List<CategoryDto> getCategoriesWithProductCount() {
        log.info("카테고리 목록 조회 (상품 개수 포함)");

        List<Category> categories = categoryRepository.findAllByOrderByCategoryIdAsc();

        List<CategoryDto> categoryDtos = categories.stream()
                .map(category -> {
                    long productCount = categoryRepository.countProductsByCategoryId(category.getCategoryId());
                    return CategoryDto.fromEntityWithCount(category, productCount);
                })
                .collect(Collectors.toList());

        log.info("카테고리 {}개 조회 완료 (상품 개수 포함)", categoryDtos.size());

        return categoryDtos;
    }

    /**
     * 카테고리 상세 조회
     */
    public CategoryDto getCategoryDetail(Long categoryId) {
        log.info("카테고리 상세 조회 - ID: {}", categoryId);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "카테고리를 찾을 수 없습니다. ID: " + categoryId));

        // 상품 개수 조회
        long productCount = categoryRepository.countProductsByCategoryId(categoryId);

        return CategoryDto.fromEntityWithCount(category, productCount);
    }

    /**
     * 카테고리 ID로 조회
     */
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));
    }

    /**
     * 카테고리명으로 조회
     */

    public CategoryDto getCategoryByName(String name) {
        log.info("카테고리 조회 - 이름: {}", name);

        Category category = categoryRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException(
                        "카테고리를 찾을 수 없습니다. 이름: " + name));

        return CategoryDto.fromEntity(category);
    }

    /**
     * 카테고리 개수 조회
     */
    public long getCategoryCount() {
        return categoryRepository.count();
    }
}


