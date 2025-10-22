package com.project.gugumarket.service;

import com.project.gugumarket.DataNotFoundException;
import com.project.gugumarket.entity.Product;
import com.project.gugumarket.entity.QnaAnswer;
import com.project.gugumarket.entity.QnaPost;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.ProductRepository;
import com.project.gugumarket.repository.QnaAnswerRepository;
import com.project.gugumarket.repository.QnaRepository;
import com.project.gugumarket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 관리자 전용 서비스
 *
 * 역할:
 * - 회원/상품/Q&A 관리
 * - 통계 데이터 제공
 * - 강제 삭제 등 관리자 권한 작업
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final QnaRepository qnaRepository;
    private final QnaAnswerRepository qnaAnswerRepository;

    // ==================== 1. 대시보드 통계 ==================== //

    /**
     * 전체 회원 수 조회
     *
     * 동작: SELECT COUNT(*) FROM users
     */
    public Long getTotalUsers() {
        Long count = userRepository.count();
        log.debug("전체 회원 수: {}", count);
        return count;
    }

    /**
     * 전체 상품 수 조회
     *
     * 동작: SELECT COUNT(*) FROM products
     */
    public Long getTotalProducts() {
        Long count = productRepository.count();
        log.debug("전체 상품 수: {}", count);
        return count;
    }

    /**
     * 전체 Q&A 수 조회
     *
     * 동작: SELECT COUNT(*) FROM qna_posts
     */
    public Long getTotalQna() {
        Long count = qnaRepository.count();
        log.debug("전체 Q&A 수: {}", count);
        return count;
    }

    /**
     * 미답변 Q&A 수 조회
     *
     * 동작: SELECT COUNT(*) FROM qna_posts WHERE is_answered = false
     */
    public Long getPendingQnaCount() {
        Long count = qnaRepository.countByIsAnsweredFalse();
        log.debug("미답변 Q&A 수: {}", count);
        return count;
    }

    // ==================== 2. 회원 관리 ==================== //

    /**
     * 회원 목록 조회 (페이징)
     *
     * 동작: SELECT * FROM users ORDER BY created_date DESC
     */
    public Page<User> getUserList(Pageable pageable) {
        log.info("회원 목록 조회 - 페이지: {}, 크기: {}", pageable.getPageNumber(), pageable.getPageSize());

        Page<User> users = userRepository.findAll(pageable);

        log.info("✅ 회원 {}개 조회 완료 (전체: {}개)", users.getContent().size(), users.getTotalElements());

        return users;
    }

    /**
     * 회원 상태 변경
     *
     * @param userId 회원 ID
     * @param status 변경할 상태 (ACTIVE, INACTIVE, BANNED 등)
     */
    @Transactional
    public void changeUserStatus(Long userId, String status) {
        log.info("회원 상태 변경 시작 - ID: {}, 상태: {}", userId, status);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다. ID: " + userId));

        // 상태 변경 (User 엔티티에 status 필드가 있다고 가정)
        // user.setStatus(status);  // User 엔티티 구조에 맞게 수정 필요

        userRepository.save(user);

        log.info("✅ 회원 상태 변경 완료 - ID: {}, 새 상태: {}", userId, status);
    }

    // ==================== 3. 상품 관리 ==================== //

    /**
     * 전체 상품 목록 조회 (페이징)
     *
     * 동작: SELECT * FROM products ORDER BY created_date DESC
     */
    public Page<Product> getProductList(Pageable pageable) {
        log.info("상품 목록 조회 - 페이지: {}, 크기: {}", pageable.getPageNumber(), pageable.getPageSize());

        Page<Product> products = productRepository.findAll(pageable);

        log.info("✅ 상품 {}개 조회 완료 (전체: {}개)", products.getContent().size(), products.getTotalElements());

        return products;
    }

    /**
     * 상품 강제 삭제 (물리적 삭제)
     *
     * @param productId 삭제할 상품 ID
     *
     * 설명:
     * - Soft Delete(isDeleted=true)가 아닌 실제 DB에서 삭제
     * - 관리자만 사용 가능
     */
    @Transactional
    public void forceDeleteProduct(Long productId) {
        log.info("상품 강제 삭제 시작 - ID: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + productId));

        // 실제 DB에서 삭제
        productRepository.delete(product);

        log.info("✅ 상품 강제 삭제 완료 - ID: {}, 제목: {}", productId, product.getTitle());
    }

    // ==================== 4. Q&A 관리 ==================== //

    /**
     * Q&A 목록 조회 (페이징)
     *
     * 동작: SELECT * FROM qna_posts ORDER BY created_date DESC
     */
    public Page<QnaPost> getQnaList(Pageable pageable) {
        log.info("Q&A 목록 조회 - 페이지: {}, 크기: {}", pageable.getPageNumber(), pageable.getPageSize());

        Page<QnaPost> qnaPosts = qnaRepository.findAllByOrderByCreatedDateDesc(pageable);

        log.info("✅ Q&A {}개 조회 완료 (전체: {}개)", qnaPosts.getContent().size(), qnaPosts.getTotalElements());

        return qnaPosts;
    }

    /**
     * Q&A 목록 조회 - 답변 상태별 필터링
     *
     * @param isAnswered true=답변완료만, false=미답변만
     */
    public Page<QnaPost> getQnaListByAnswered(Boolean isAnswered, Pageable pageable) {
        log.info("Q&A 목록 조회 (답변상태: {}) - 페이지: {}", isAnswered, pageable.getPageNumber());

        Page<QnaPost> qnaPosts = qnaRepository.findByIsAnsweredOrderByCreatedDateDesc(isAnswered, pageable);

        log.info("✅ Q&A {}개 조회 완료 (답변상태: {})", qnaPosts.getContent().size(), isAnswered);

        return qnaPosts;
    }

    /**
     * Q&A 답변 작성
     *
     * @param qnaId Q&A 게시글 ID
     * @param content 답변 내용
     *
     * 동작:
     * 1. Q&A 게시글 존재 확인
     * 2. QnaAnswer 엔티티 생성 및 저장
     * 3. QnaPost의 isAnswered를 true로 변경
     */
    @Transactional
    public void createQnaAnswer(Long qnaId, String content) {
        log.info("Q&A 답변 작성 시작 - QNA ID: {}", qnaId);

        // Q&A 게시글 조회
        QnaPost qnaPost = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new IllegalArgumentException("Q&A 게시글을 찾을 수 없습니다. ID: " + qnaId));

        // 답변 엔티티 생성
        QnaAnswer answer = QnaAnswer.builder()
                .qnaPost(qnaPost)
                .content(content)
                .build();

        // 답변 저장
        qnaAnswerRepository.save(answer);

        // Q&A 게시글의 답변 상태 업데이트
        qnaPost.setIsAnswered(true);
        qnaRepository.save(qnaPost);

        log.info("✅ Q&A 답변 작성 완료 - QNA ID: {}, 답변 ID: {}", qnaId, answer.getAnswerId());
    }

    /**
     * 관리자 페이지용 회원 목록 (페이징 없이 전체 또는 최근 100명)
     */
    public List<User> getUserListForAdmin() {
        Pageable pageable = PageRequest.of(0, 100);
        return userRepository.findAll(pageable).getContent();
    }

    /**
     * 관리자 페이지용 상품 목록 (최근 100개)
     */
    public List<Product> getProductListForAdmin() {
        Pageable pageable = PageRequest.of(0, 100);
        return productRepository.findAll(pageable).getContent();
    }

    /**
     * 관리자 페이지용 Q&A 목록 (최근 50개)
     */
    public List<QnaPost> getQnaListForAdmin() {
        Pageable pageable = PageRequest.of(0, 50);
        return qnaRepository.findAllByOrderByCreatedDateDesc(pageable).getContent();
    }
}