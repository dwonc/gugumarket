package com.project.gugumarket.service;

import com.project.gugumarket.entity.Product;
import com.project.gugumarket.entity.QnaAnswer;
import com.project.gugumarket.entity.QnaPost;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.ProductRepository;
import com.project.gugumarket.repository.QnaAnswerRepository;
import com.project.gugumarket.repository.QnaPostRepository;
import com.project.gugumarket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final QnaPostRepository qnaPostRepository;
    private final QnaAnswerRepository qnaAnswerRepository;

    // ===== 통계 관련 =====

    /**
     * 전체 회원 수 조회
     */
    public long getTotalUsersCount() {
        return userRepository.count();
    }

    /**
     * 전체 상품 수 조회
     */
    public long getTotalProductsCount() {
        return productRepository.count();
    }

    /**
     * 미답변 Q&A 수 조회
     */
    public long getUnansweredQnaCount() {
        return qnaPostRepository.countByIsAnswered(Boolean.FALSE);
    }

    // ===== 회원 관리 =====

    /**
     * 전체 회원 목록 조회
     */
    public List<User> getAllUsers() {
        return userRepository.findAllByOrderByCreatedDateDesc();
    }

    /**
     * 회원 검색
     */
    public List<User> searchUsers(String keyword) {
        return userRepository.findByUserNameContainingOrNicknameContainingOrEmailContaining(
                keyword, keyword, keyword
        );
    }

    /**
     * 회원 상세 조회
     */
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
    }

    /**
     * 회원 상태 토글 (활성/정지)
     */
    @Transactional
    public boolean toggleUserStatus(Long userId) {
        User user = getUserById(userId);
        boolean currentStatus = Boolean.TRUE.equals(user.getIsActive());
        user.setIsActive(!currentStatus);
        userRepository.save(user);
        log.info("회원 상태 변경: userId={}, isActive={}", userId, user.getIsActive());
        return Boolean.TRUE.equals(user.getIsActive());
    }

    /**
     * 회원 삭제
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = getUserById(userId);

        // 해당 회원의 상품들을 삭제 처리
        List<Product> products = productRepository.findBySellerUserId(userId);
        products.forEach(product -> {
            product.setIsDeleted(true);
            productRepository.save(product);
        });

        // 회원 삭제
        userRepository.delete(user);
        log.info("회원 삭제 완료: userId={}", userId);
    }

    // ===== 상품 관리 =====

    /**
     * 전체 상품 목록 조회
     */
    public List<Product> getAllProducts() {
        return productRepository.findAllByOrderByCreatedDateDesc();
    }

    /**
     * 상품 검색
     */
    public List<Product> searchProducts(String keyword) {
        return productRepository.findByTitleContainingOrContentContaining(keyword, keyword);
    }

    /**
     * 삭제 상태별 상품 조회
     */
    public List<Product> getProductsByDeletedStatus(Boolean isDeleted) {
        return productRepository.findByIsDeletedOrderByCreatedDateDesc(isDeleted);
    }

    /**
     * 특정 회원의 상품 목록 조회
     */
    public List<Product> getProductsByUser(Long userId) {
        return productRepository.findBySellerUserIdOrderByCreatedDateDesc(userId);
    }

    /**
     * 상품 삭제
     */
    @Transactional
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        product.setIsDeleted(true);
        productRepository.save(product);
        log.info("상품 삭제 완료: productId={}", productId);
    }

    // ===== Q&A 관리 =====

    /**
     * 전체 Q&A 목록 조회 (미답변 우선)
     */
    public List<QnaPost> getAllQnaPostsSortedByAnswered() {
        return qnaPostRepository.findAllByOrderByIsAnsweredAscCreatedDateDesc();
    }

    /**
     * 특정 회원의 Q&A 목록 조회
     */
    public List<QnaPost> getQnaPostsByUser(Long userId) {
        return qnaPostRepository.findByUserUserIdOrderByCreatedDateDesc(userId);
    }

    /**
     * Q&A 답변 등록
     */
    @Transactional
    public void answerQna(Long qnaId, String content) {
        QnaPost qnaPost = qnaPostRepository.findById(qnaId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 Q&A입니다."));

        if (Boolean.TRUE.equals(qnaPost.getIsAnswered())) {
            throw new IllegalStateException("이미 답변된 Q&A입니다.");
        }

        // 현재 로그인한 관리자 정보
        String adminUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User admin = userRepository.findByUserName(adminUsername)
                .orElseThrow(() -> new IllegalStateException("관리자 정보를 찾을 수 없습니다."));

        // 답변 생성
        QnaAnswer answer = QnaAnswer.builder()
                .qnaPost(qnaPost)
                .admin(admin)  // ✅ admin 필드 사용
                .content(content)
                .createdDate(LocalDateTime.now())
                .build();

        qnaAnswerRepository.save(answer);

        // Q&A 답변 완료 상태로 변경
        qnaPost.setIsAnswered(true);  // ✅ setIsAnswered 사용
        qnaPostRepository.save(qnaPost);

        log.info("Q&A 답변 등록 완료: qnaId={}, adminId={}", qnaId, admin.getUserId());
    }
}