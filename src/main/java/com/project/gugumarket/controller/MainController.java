package com.project.gugumarket.controller;

import com.project.gugumarket.dto.ProductForm;
import com.project.gugumarket.entity.Category;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.CategoryRepository;
import com.project.gugumarket.repository.UserRepository;
import com.project.gugumarket.service.LikeService;
import com.project.gugumarket.service.NotificationService;
import com.project.gugumarket.service.ProductService;
import com.project.gugumarket.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

/**
 * 메인 페이지를 처리하는 컨트롤러
 * 상품 목록 표시, 페이징, 검색, 카테고리 필터링 기능을 담당
 */
@Controller
@RequiredArgsConstructor  // final 필드에 대한 생성자 자동 생성
public class MainController {

    private final UserRepository userRepository;  // 사용자 데이터베이스 접근
    private final ProductService productService;  // 상품 관련 비즈니스 로직
    private final CategoryRepository categoryRepository;  // 카테고리 데이터베이스 접근
    private final NotificationService notificationService;  // 알림 관련 로직
    private final LikeService likeService;  // 찜 기능 처리
    private final UserService userService;  // 사용자 관련 로직

    /**
     * 메인 페이지 표시
     * GET /main
     * 상품 목록을 페이징 처리하여 표시하고, 검색 및 카테고리 필터 기능 제공
     * 로그인한 사용자의 찜 목록과 읽지 않은 알림 개수도 함께 표시
     *
     * @param model 뷰로 전달할 데이터
     * @param page 현재 페이지 번호 (기본값: 0)
     * @param size 한 페이지에 표시할 상품 개수 (기본값: 12)
     * @param categoryId 필터링할 카테고리 ID (선택, null이면 전체)
     * @param keyword 검색어 (선택, null이면 검색 안 함)
     * @param principal 현재 로그인한 사용자 정보
     * @return "main" - main.html 템플릿
     */
    @GetMapping("/main")
    public String main(
            Model model,
            @RequestParam(defaultValue = "0") int page,  // URL에 없으면 0 (첫 페이지)
            @RequestParam(defaultValue = "12") int size,  // URL에 없으면 12개씩 표시
            @RequestParam(required = false) Long categoryId,  // 선택적 파라미터
            @RequestParam(required = false) String keyword,  // 선택적 파라미터
            Principal principal  // Spring Security의 인증 정보
    ) {
        // 디버깅용 로그 시작
        System.out.println("========== 메인 페이지 시작 ==========");
        System.out.println("📄 페이지: " + page + ", 사이즈: " + size);
        System.out.println("📂 카테고리: " + categoryId);
        System.out.println("🔍 검색어: " + keyword);

        // Spring Security의 인증 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        // 현재 로그인한 사용자 정보 조회
        User user = userService.getUser(principal.getName());

        // 읽지 않은 알림 개수 조회
        long unreadCount = notificationService.getUnreadCount(user);

        User currentUser = null;

        // 로그인 상태 확인
        // Spring Security는 비로그인 사용자를 "anonymousUser"로 표시
        if (!"anonymousUser".equals(username)) {
            System.out.println("👤 로그인 사용자: " + username);
            // 데이터베이스에서 사용자 정보 조회
            Optional<User> userOpt = userRepository.findByUserName(username);
            if (userOpt.isPresent()) {
                currentUser = userOpt.get();
                model.addAttribute("user", currentUser);
                System.out.println("✅ 사용자 정보 로드: " + currentUser.getNickname());
            }
        } else {
            System.out.println("👥 비로그인 상태");
        }

        // 페이징 정보 생성 (페이지 번호, 페이지 크기)
        Pageable pageable = PageRequest.of(page, size);

        // 상품 목록 조회 (카테고리 필터 또는 전체)
        Page<ProductForm> products;
        if (categoryId != null) {
            // 특정 카테고리의 상품만 조회 (검색어 포함 가능)
            products = productService.getProductsByCategory(categoryId, keyword, pageable);
            model.addAttribute("selectedCategoryId", categoryId);  // 선택된 카테고리 표시용
        } else {
            // 전체 상품 조회 (검색어 포함 가능)
            products = productService.getProductList(keyword, pageable);
        }

        // 로그인한 사용자가 찜한 상품 ID 목록 조회
        // final로 선언하여 람다식 내부에서 사용 가능하게 함
        final List<Long> likedProductIds;
        if (currentUser != null) {
            // 사용자가 찜한 모든 상품의 ID 목록
            likedProductIds = likeService.getLikedProductIds(currentUser);
            System.out.println("❤️ 찜한 상품: " + likedProductIds.size() + "개");
        } else {
            // 비로그인 사용자는 빈 리스트
            likedProductIds = List.of();
        }

        // 각 상품에 찜 여부 표시
        // 상품 목록을 순회하면서 찜한 상품이면 isLiked를 true로 설정
        if (!likedProductIds.isEmpty()) {
            products.getContent().forEach(product -> {
                // 현재 상품 ID가 찜 목록에 있는지 확인
                if (likedProductIds.contains(product.getProductId())) {
                    product.setIsLiked(true);  // 찜 상태 설정
                    System.out.println("❤️ 상품 ID " + product.getProductId() + " 찜됨 표시");
                }
            });
        }

        // 모든 카테고리 목록 조회 (카테고리 필터 UI 표시용)
        List<Category> categories = categoryRepository.findAll();
        System.out.println("📂 카테고리 " + categories.size() + "개 로드");

        // 모델에 데이터 추가 (뷰로 전달)
        model.addAttribute("products", products);  // 상품 목록
        model.addAttribute("categories", categories);  // 카테고리 목록
        model.addAttribute("currentPage", page);  // 현재 페이지 번호
        model.addAttribute("totalPages", products.getTotalPages());  // 전체 페이지 수
        model.addAttribute("totalElements", products.getTotalElements());  // 전체 상품 개수
        model.addAttribute("keyword", keyword);  // 검색어 (검색창에 유지용)
        model.addAttribute("unreadCount", unreadCount);  // 읽지 않은 알림 개수

        // 완료 로그 출력
        System.out.println("✅ 상품 " + products.getContent().size() + "개 조회 완료");
        System.out.println("📊 전체 상품: " + products.getTotalElements() + "개");
        System.out.println("📄 현재 페이지: " + (page + 1) + " / " + products.getTotalPages());
        System.out.println("========================================");

        return "main";  // main.html 템플릿 반환
    }

    /**
     * 루트 경로 접속 시 메인 페이지로 리다이렉트
     * GET /
     * 사용자가 도메인만 입력했을 때 자동으로 메인 페이지로 이동
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/main";  // /main 경로로 리다이렉트
    }
}