package com.project.gugumarket.controller;

import com.project.gugumarket.dto.ProductForm;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.UserRepository;
<<<<<<< HEAD
=======
import com.project.gugumarket.service.LikeService;
import com.project.gugumarket.service.NotificationService;
import com.project.gugumarket.service.ProductService;
import com.project.gugumarket.service.UserService;
import lombok.RequiredArgsConstructor;
>>>>>>> 9b8c76477af207ef0a169f0af00a0a3be54e39b0
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

<<<<<<< HEAD
=======
import java.security.Principal;
import java.util.List;
>>>>>>> 28cebc40083f14c3d32f93518519a56ce9ec8b8a
import java.util.Optional;

@Controller
public class MainController {

    private final UserRepository userRepository;
<<<<<<< HEAD

    public MainController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 메인 페이지
    @GetMapping("/main")
    public String main(Model model) {
        // 현재 로그인한 사용자 정보 가져오기
=======
    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    private final NotificationService notificationService;
    private final LikeService likeService;  // 🔥 LikeService 추가
<<<<<<< HEAD
>>>>>>> 9b8c76477af207ef0a169f0af00a0a3be54e39b0
=======
    private final UserService userService;
>>>>>>> 28cebc40083f14c3d32f93518519a56ce9ec8b8a

    /**
     * 메인 페이지 (페이징 + 검색 + 카테고리 필터)
     */
    @GetMapping("/main")
    public String main(
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            Principal principal
    ) {
        System.out.println("========== 메인 페이지 시작 ==========");
        System.out.println("📄 페이지: " + page + ", 사이즈: " + size);
        System.out.println("📂 카테고리: " + categoryId);
        System.out.println("🔍 검색어: " + keyword);


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

<<<<<<< HEAD
<<<<<<< HEAD
        System.out.println("로그인 성공 - 사용자: " + username);
=======
=======
        User user = userService.getUser(principal.getName());

        long unreadCount = notificationService.getUnreadCount(user);

>>>>>>> 28cebc40083f14c3d32f93518519a56ce9ec8b8a
        User currentUser = null;

        if (!"anonymousUser".equals(username)) {
            System.out.println("👤 로그인 사용자: " + username);
            Optional<User> userOpt = userRepository.findByUserName(username);
            if (userOpt.isPresent()) {
                currentUser = userOpt.get();
                model.addAttribute("user", currentUser);
                System.out.println("✅ 사용자 정보 로드: " + currentUser.getNickname());
            }
        } else {
            System.out.println("👥 비로그인 상태");
        }
>>>>>>> 9b8c76477af207ef0a169f0af00a0a3be54e39b0

        model.addAttribute("username", username);
        // 사용자 정보를 DB에서 가져오기
        Optional<User> userOpt = userRepository.findByUserName(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            model.addAttribute("user", user);
            model.addAttribute("username", user.getUserName());
            System.out.println("✅ 사용자 정보 로드 완료: " + user.getNickname());
        }
        Pageable pageable = PageRequest.of(page, size);

        Page<ProductForm> products;

        if (categoryId != null) {
            products = productService.getProductsByCategory(categoryId, keyword, pageable);
            model.addAttribute("selectedCategoryId", categoryId);
        } else {
            products = productService.getProductList(keyword, pageable);
        }

        // 🔥 로그인한 사용자가 찜한 상품 ID 목록 조회 (final로 선언)
        final List<Long> likedProductIds;
        if (currentUser != null) {
            likedProductIds = likeService.getLikedProductIds(currentUser);
            System.out.println("❤️ 찜한 상품: " + likedProductIds.size() + "개");
        } else {
            likedProductIds = List.of();  // 빈 리스트
        }

        // 🔥 각 상품에 찜 여부 설정
        if (!likedProductIds.isEmpty()) {
            products.getContent().forEach(product -> {
                if (likedProductIds.contains(product.getProductId())) {
                    product.setIsLiked(true);
                    System.out.println("❤️ 상품 ID " + product.getProductId() + " 찜됨 표시");
                }
            });
        }

        List<Category> categories = categoryRepository.findAll();
        System.out.println("📂 카테고리 " + categories.size() + "개 로드");

        model.addAttribute("products", products);
        model.addAttribute("categories", categories);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", products.getTotalPages());
        model.addAttribute("totalElements", products.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("unreadCount", unreadCount);

        System.out.println("✅ 상품 " + products.getContent().size() + "개 조회 완료");
        System.out.println("📊 전체 상품: " + products.getTotalElements() + "개");
        System.out.println("📄 현재 페이지: " + (page + 1) + " / " + products.getTotalPages());
        System.out.println("========================================");
        return "main";
    }

    // 홈 페이지 (로그인 전)
    @GetMapping("/")
    public String home() {
        return "index";
    }
}
