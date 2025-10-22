package com.project.gugumarket.service;

import com.project.gugumarket.DataNotFoundException;
import com.project.gugumarket.dto.QnaDto;
import com.project.gugumarket.entity.QnaPost;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.QnaRepository;
import com.project.gugumarket.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QnaService {

    private final QnaRepository qnaRepository;
    private final UserRepository userRepository;

    /**
     * Q&A 작성
     */
    @Transactional
    public QnaPost create(QnaDto dto, String loginName) {
        log.info("Q&A 작성 - loginName: {}", loginName);

        // loginName으로 사용자 찾기 (email 먼저, 없으면 userName)
        User user = userRepository.findByEmail(loginName)
                .orElseGet(() ->
                        userRepository.findByUserName(loginName)
                                .orElseThrow(() -> new EntityNotFoundException(
                                        "사용자를 찾을 수 없습니다: " + loginName)));

        // DTO → Entity 변환
        QnaPost qnaPost = QnaDto.toEntity(dto);
        qnaPost.setUser(user);
        qnaPost.setIsAnswered(false);

        QnaPost savedQna = qnaRepository.save(qnaPost);

        log.info("Q&A 작성 완료 - ID: {}", savedQna.getQnaId());

        return savedQna;
    }

    /**
     * 전체 Q&A 목록 조회
     */
    public List<QnaDto> findAllDtos() {
        log.info("전체 Q&A 목록 조회");

        List<QnaPost> qnaList = qnaRepository.findAll();

        log.info("Q&A {}개 조회 완료", qnaList.size());

        return qnaList.stream()
                .map(QnaDto::fromEntity)
                .toList();
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