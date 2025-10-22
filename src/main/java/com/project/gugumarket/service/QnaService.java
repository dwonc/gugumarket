package com.project.gugumarket.service;

import com.project.gugumarket.dto.QnaDto;
import com.project.gugumarket.entity.QnaPost;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.QnaRepository;
import com.project.gugumarket.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
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
}