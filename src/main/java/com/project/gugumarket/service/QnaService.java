package com.project.gugumarket.service;

import com.project.gugumarket.DataNotFoundException;
import com.project.gugumarket.entity.QnaPost;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.QnaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QnaService {

    private final QnaRepository qnaRepository;

    // ==================== 기존 메서드들 ==================== //

    public QnaPost getQnaPost(Long qnaId) {
        Optional<QnaPost> qnaPost = qnaRepository.findById(qnaId);
        if (qnaPost.isPresent()) {
            return qnaPost.get();
        } else {
            throw new DataNotFoundException("QnA 게시글을 찾을 수 없습니다.");
        }
    }

    @Transactional
    public QnaPost createQna(String title, String content, User user) {
        QnaPost qnaPost = QnaPost.builder()
                .user(user)
                .title(title)
                .content(content)
                .isAnswered(false)
                .build();

        return qnaRepository.save(qnaPost);
    }

    @Transactional
    public void updateQna(Long qnaId, String title, String content) {
        QnaPost qnaPost = getQnaPost(qnaId);
        qnaPost.setTitle(title);
        qnaPost.setContent(content);
        qnaRepository.save(qnaPost);
    }

    @Transactional
    public void deleteQna(Long qnaId) {
        QnaPost qnaPost = getQnaPost(qnaId);
        qnaRepository.delete(qnaPost);
    }

    // ==================== 검색 기능 ==================== //

    /**
     * QnA 검색 (제목 + 내용, 페이징)
     */
    public Page<QnaPost> searchQna(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            System.out.println("🔍 QnA 검색: '" + keyword + "'");
            return qnaRepository.searchByKeyword(keyword, pageable);
        } else {
            System.out.println("📋 QnA 전체 조회");
            return qnaRepository.findAllByOrderByCreatedDateDesc(pageable);
        }
    }

    /**
     * 전체 목록 조회 (페이징)
     */
    public Page<QnaPost> getQnaList(Pageable pageable) {
        return qnaRepository.findAllByOrderByCreatedDateDesc(pageable);
    }
}