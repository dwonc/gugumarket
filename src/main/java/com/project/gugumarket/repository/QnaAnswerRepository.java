package com.project.gugumarket.repository;

import com.project.gugumarket.entity.QnaAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QnaAnswerRepository extends JpaRepository<QnaAnswer, Long> {

    /**
     * 특정 Q&A의 답변 조회
     */
    List<QnaAnswer> findByQnaPostQnaId(Long qnaId);

    /**
     * 특정 Q&A의 답변 조회 (작성일순)
     */
    List<QnaAnswer> findByQnaPostQnaIdOrderByCreatedDateAsc(Long qnaId);

    /**
     * 특정 관리자의 답변 조회
     */
    List<QnaAnswer> findByAnswerId(Long userId);
}