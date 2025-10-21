// src/main/java/com/project/gugumarket/service/LikeService.java
package com.project.gugumarket.service;

import com.project.gugumarket.entity.Like;
import com.project.gugumarket.entity.Product;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.LikeRepository;
import com.project.gugumarket.repository.ProductRepository;
import com.project.gugumarket.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    /** Principal.getName()으로 넘어온 문자열(username/email/id)을 해석해 User 조회 */
    private User getUserByPrincipalName(String principalName) {
        if (principalName == null || principalName.isBlank()) {
            throw new EntityNotFoundException("로그인 정보가 없습니다.");
        }

        // 1) username으로 조회
        var byUsername = userRepository.findByUserName(principalName);
        if (byUsername.isPresent()) return byUsername.get();

        // 2) email로 조회
        var byEmail = userRepository.findByEmail(principalName);
        if (byEmail.isPresent()) return byEmail.get();

        // 3) 숫자형 id로 조회
        try {
            Long id = Long.parseLong(principalName);
            return userRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("사용자 없음: " + principalName));
        } catch (NumberFormatException e) {
            throw new EntityNotFoundException("사용자를 찾을 수 없습니다: " + principalName);
        }
    }

    /** 상품 존재 여부 확인용 */
    private Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("상품 없음: " + productId));
    }

    /** 찜 추가/취소 (toggleLike) */
    @Transactional
    public ToggleResult toggleByPrincipal(Long productId, String principalName) {
        User user = getUserByPrincipalName(principalName);
        Product product = getProduct(productId);

        return likeRepository.findByUserAndProduct(user, product)
                .map(existing -> { // 이미 찜 → 취소
                    likeRepository.delete(existing);
                    long count = likeRepository.countByProduct(product);
                    return new ToggleResult(false, count);
                })
                .orElseGet(() -> { // 미찜 → 추가
                    likeRepository.save(Like.builder()
                            .user(user)
                            .product(product)
                            .build());
                    long count = likeRepository.countByProduct(product);
                    return new ToggleResult(true, count);
                });
    }

    /** 로그인 사용자의 찜 목록 (페이징) */
    @Transactional(readOnly = true)
    public Page<Like> myLikesByPrincipal(String principalName, Pageable pageable) {
        User user = getUserByPrincipalName(principalName);
        return likeRepository.findAllByUser(user, pageable);
    }

    /** 특정 상품의 좋아요 개수 */
    @Transactional(readOnly = true)
    public long countLikes(Long productId) {
        return likeRepository.countByProduct(getProduct(productId));
    }

    /** 응답 DTO (JSON 변환용) */
    public record ToggleResult(boolean liked, long likeCount) {}
}
