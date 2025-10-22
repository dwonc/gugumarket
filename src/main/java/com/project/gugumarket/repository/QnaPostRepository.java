package com.project.gugumarket.repository;

import com.project.gugumarket.entity.QnaPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QnaPostRepository extends JpaRepository<QnaPost, Long> {

    /**
     * 전체 Q&A 조회 (최신순)
     */
    List<QnaPost> findAllByOrderByCreatedDateDesc();

    /**
     * 전체 Q&A 조회 (미답변 우선, 최신순)
     */
    List<QnaPost> findAllByOrderByIsAnsweredAscCreatedDateDesc();

    /**
     * 답변 상태별 Q&A 조회
     */
    List<QnaPost> findByIsAnsweredOrderByCreatedDateDesc(Boolean isAnswered);

    /**
     * 미답변 Q&A 수 조회
     */
    long countByIsAnswered(Boolean isAnswered);

    /**
     * 특정 회원의 Q&A 조회 (최신순)
     */
    List<QnaPost> findByUserUserIdOrderByCreatedDateDesc(Long userId);

    /**
     * 제목 또는 내용으로 검색
     */
    List<QnaPost> findByTitleContainingOrContentContaining(String title, String content);
}