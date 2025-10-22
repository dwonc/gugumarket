package com.project.gugumarket.repository;

import com.project.gugumarket.entity.QnaPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QnaRepository extends JpaRepository<QnaPost, Long> {

    /**
     * 전체 목록 조회 (최신순, 페이징)
     */
    Page<QnaPost> findAllByOrderByCreatedDateDesc(Pageable pageable);

    /**
     * 제목 또는 내용으로 검색 (제목 + 내용 둘 다 검색)
     */
    @Query("SELECT q FROM QnaPost q WHERE " +
            "q.title LIKE %:keyword% OR q.content LIKE %:keyword% " +
            "ORDER BY q.createdDate DESC")
    Page<QnaPost> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 답변 상태별 조회
     */
    Page<QnaPost> findByIsAnsweredOrderByCreatedDateDesc(Boolean isAnswered, Pageable pageable);

    /**
     * 답변 상태 + 검색
     */
    @Query("SELECT q FROM QnaPost q WHERE " +
            "(q.title LIKE %:keyword% OR q.content LIKE %:keyword%) " +
            "AND q.isAnswered = :isAnswered " +
            "ORDER BY q.createdDate DESC")
    Page<QnaPost> searchByKeywordAndAnswerStatus(
            @Param("keyword") String keyword,
            @Param("isAnswered") Boolean isAnswered,
            Pageable pageable);
}