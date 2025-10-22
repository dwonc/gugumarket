package com.project.gugumarket.repository;

import com.project.gugumarket.entity.QnaAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QnaAnswerRepository extends JpaRepository<QnaAnswer, Long> {

    /**
     * 특정 Q&A의 답변 목록 조회
     */
    List<QnaAnswer> findByQnaPost_QnaIdOrderByCreatedDateAsc(Long qnaId);
}