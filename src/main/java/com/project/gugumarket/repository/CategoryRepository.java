package com.project.gugumarket.repository;

import com.project.gugumarket.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // 카테고리명으로 조회
    Optional<Category> findByName(String name);

    // 모든 카테고리 조회 (ID 순)
    // findAll()을 사용하면 되지만, 정렬을 원하면:
    // List<Category> findAllByOrderByCategoryIdAsc();
}