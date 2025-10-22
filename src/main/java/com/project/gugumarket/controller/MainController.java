package com.project.gugumarket.controller;

import com.project.gugumarket.dto.ProductForm;
import com.project.gugumarket.entity.Category;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.CategoryRepository;
import com.project.gugumarket.repository.UserRepository;
import com.project.gugumarket.service.ProductService;
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

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final UserRepository userRepository;
    private final ProductService productService;
    private final CategoryRepository categoryRepository;

    /**
     * 메인 페이지 (페이징 기능 포함)
     *
     * @param model 뷰에 전달할 데이터
     * @param page 현재 페이지 번호 (0부터 시작, 기본값 0)
     * @param size 한 페이지당 상품 개수 (기본값 12)
     * @param categoryId 선택된 카테고리 ID (선택사항)
     */
    @GetMapping("/main")
    public String main(
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) Long categoryId
    ) {
        System.out.println("========== 메인 페이지 시작 ==========");
        System.out.println("📄 페이지: " + page + ", 사이즈: " + size + ", 카테고리: " + categoryId);

        // 🔐 현재 로그인한 사용자 정보
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        if (!"anonymousUser".equals(username)) {
            System.out.println("👤 로그인 사용자: " + username);
            Optional<User> userOpt = userRepository.findByUserName(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                model.addAttribute("user", user);
                System.out.println("✅ 사용자 정보 로드: " + user.getNickname());
            }
        } else {
            System.out.println("👥 비로그인 상태");
        }

        // 📦 페이징 설정
        Pageable pageable = PageRequest.of(page, size);

        // 🛒 상품 목록 조회
        Page<ProductForm> products;
        if (categoryId != null) {
            System.out.println("🔍 카테고리 " + categoryId + "번 상품 조회");
            products = productService.getProductsByCategory(categoryId, pageable);
            model.addAttribute("selectedCategoryId", categoryId);
        } else {
            System.out.println("🔍 전체 상품 조회");
            products = productService.getProductList(pageable);
        }

        // 📂 카테고리 목록 조회
        List<Category> categories = categoryRepository.findAll();
        System.out.println("📂 카테고리 " + categories.size() + "개 로드");

        // 📊 Model에 데이터 추가
        model.addAttribute("products", products);
        model.addAttribute("categories", categories);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", products.getTotalPages());
        model.addAttribute("totalElements", products.getTotalElements());

        System.out.println("✅ 상품 " + products.getContent().size() + "개 조회 완료");
        System.out.println("📊 전체 상품: " + products.getTotalElements() + "개");
        System.out.println("📄 현재 페이지: " + (page + 1) + " / " + products.getTotalPages());
        System.out.println("========================================");

        return "main";
    }

    /**
     * 홈 페이지 (기존 유지)
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/main";
    }
}