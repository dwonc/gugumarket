package com.project.gugumarket.repository;

import com.project.gugumarket.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByProduct_ProductIdAndIsDeletedFalseOrderByCreatedDateAsc(Long productId);

    long countByProduct_ProductIdAndIsDeletedFalse(Long productId);
}
