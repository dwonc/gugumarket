package com.project.gugumarket.controller;

import com.project.gugumarket.dto.ProductDto;
import com.project.gugumarket.dto.ResponseDto;
import com.project.gugumarket.dto.CategoryDto;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.service.LikeService;
import com.project.gugumarket.service.NotificationService;
import com.project.gugumarket.service.ProductService;
import com.project.gugumarket.service.UserService;
import com.project.gugumarket.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;

/**
 * 메인 페이지 API 컨트롤러 (REST API)
 * React SPA용 - 상품 목록, 검색, 카테고리 필터링, 페이징 기능 제공
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class MainController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final NotificationService notificationService;
    private final LikeService likeService;
    private final UserService userService;

    /**
     * 메인 페이지 데이터 조회 API
     * GET /api/main
     *
     * 쿼리 파라미터:
     * - page: 페이지 번호 (기본값: 0)
     * - size: 페이지 크기 (기본값: 12)
     * - categoryId: 카테고리 필터 (선택)
     * - keyword: 검색어 (선택)
     */
    @GetMapping("/main")
    public ResponseEntity<ResponseDto<Map<String, Object>>> getMainPageData(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            Principal principal
    ) {
        try {
            log.info("========== 메인 페이지 데이터 조회 ==========");
            log.info("📄 페이지: {}, 사이즈: {}", page, size);
            log.info("📂 카테고리: {}", categoryId);
            log.info("🔍 검색어: {}", keyword);

            // 응답 데이터를 담을 Map
            Map<String, Object> responseData = new HashMap<>();

            // 1. 현재 로그인한 사용자 정보 조회
            User currentUser = getCurrentUser(principal);

            if (currentUser != null) {
                log.info("👤 로그인 사용자: {}", currentUser.getUserName());

                // 사용자 기본 정보 (민감 정보 제외)
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("userId", currentUser.getUserId());
                userInfo.put("userName", currentUser.getUserName());
                userInfo.put("nickname", currentUser.getNickname());
                userInfo.put("profileImageUrl", currentUser.getProfileImage());
                responseData.put("user", userInfo);

                // 읽지 않은 알림 개수
                long unreadCount = notificationService.getUnreadCount(currentUser);
                responseData.put("unreadNotificationCount", unreadCount);
                log.info("🔔 읽지 않은 알림: {}개", unreadCount);
            } else {
                log.info("👥 비로그인 상태");
                responseData.put("user", null);
                responseData.put("unreadNotificationCount", 0);
            }

            // 2. 페이징 설정
            Pageable pageable = PageRequest.of(page, size);

            // 3. 상품 목록 조회 (DTO로 변환)
            Page<ProductDto> products;
            if (categoryId != null) {
                // 카테고리 필터 적용
                products = productService.getProductsByCategoryDto(categoryId, keyword, pageable);
                responseData.put("selectedCategoryId", categoryId);
            } else {
                // 전체 상품 조회
                products = productService.getProductListDto(keyword, pageable);
            }

            // 4. 로그인한 사용자의 찜 목록 조회
            if (currentUser != null) {
                List<Long> likedProductIds = likeService.getLikedProductIds(currentUser);
                log.info("❤️ 찜한 상품 ID 목록: {}", likedProductIds);  // 👈 ID 리스트 출력
                log.info("❤️ 찜한 상품: {}개", likedProductIds.size());

                // 각 상품에 찜 여부 설정
                products.getContent().forEach(product -> {
                    boolean isLiked = likedProductIds.contains(product.getProductId());
                    log.info("🔍 상품 ID {}: 찜 여부 = {}", product.getProductId(), isLiked);  // 👈 추가
                    
                    if (likedProductIds.contains(product.getProductId())) {
                        product.setIsLiked(true);
                        log.debug("❤️ 상품 ID {} 찜됨 표시", product.getProductId());
                    }
                });

                responseData.put("likedProductIds", likedProductIds);
            } else {
                responseData.put("likedProductIds", Collections.emptyList());
            }

            // 5. 카테고리 목록 조회 (DTO로 변환)
            List<CategoryDto> categories = categoryService.getAllCategories();
            log.info("📂 카테고리 {}개 로드", categories.size());

            // 6. 페이징 정보
            Map<String, Object> pagination = new HashMap<>();
            pagination.put("content", products.getContent());
            pagination.put("currentPage", page);
            pagination.put("totalPages", products.getTotalPages());
            pagination.put("totalElements", products.getTotalElements());
            pagination.put("size", size);
            pagination.put("numberOfElements", products.getNumberOfElements());
            pagination.put("first", products.isFirst());
            pagination.put("last", products.isLast());

            // 7. 최종 응답 데이터 구성
            responseData.put("products", pagination);
            responseData.put("categories", categories);
            responseData.put("keyword", keyword);

            log.info("✅ 상품 {}개 조회 완료", products.getContent().size());
            log.info("📊 전체 상품: {}개", products.getTotalElements());
            log.info("📄 현재 페이지: {} / {}", page + 1, products.getTotalPages());
            log.info("========================================");

            return ResponseEntity.ok(
                    ResponseDto.success("메인 페이지 데이터 조회 성공", responseData)
            );

        } catch (Exception e) {
            log.error("메인 페이지 데이터 조회 중 오류 발생", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDto.fail("데이터 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 현재 로그인한 사용자 조회
     *
     * @param principal Spring Security Principal
     * @return 로그인한 사용자 또는 null
     */
    private User getCurrentUser(Principal principal) {
        if (principal == null) {
            return null;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        // "anonymousUser"는 Spring Security의 비로그인 사용자
        if ("anonymousUser".equals(username)) {
            return null;
        }

        try {
            return userService.getUser(username);
        } catch (Exception e) {
            log.warn("사용자 조회 실패: {}", username, e);
            return null;
        }
    }

    /**
     * 헬스체크 API
     * GET /api/health
     */
    @GetMapping("/health")
    public ResponseEntity<ResponseDto<String>> healthCheck() {
        return ResponseEntity.ok(
                ResponseDto.success("서버 정상 작동 중", "OK")
        );
    }
}
