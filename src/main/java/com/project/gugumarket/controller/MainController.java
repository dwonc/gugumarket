package com.project.gugumarket.controller;

import com.project.gugumarket.dto.ProductForm;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class MainController {

    private final UserRepository userRepository;

    public MainController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 메인 페이지
    @GetMapping("/main")
    public String main(Model model) {
        // 현재 로그인한 사용자 정보 가져오기

    /**
     * 메인 페이지 (페이징 + 검색 + 카테고리 필터)
     */
    @GetMapping("/main")
    public String main(
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword
    ) {
        System.out.println("========== 메인 페이지 시작 ==========");
        System.out.println("📄 페이지: " + page + ", 사이즈: " + size);
        System.out.println("📂 카테고리: " + categoryId);
        System.out.println("🔍 검색어: " + keyword);


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        System.out.println("로그인 성공 - 사용자: " + username);

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

        List<Category> categories = categoryRepository.findAll();
        System.out.println("📂 카테고리 " + categories.size() + "개 로드");

        model.addAttribute("products", products);
        model.addAttribute("categories", categories);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", products.getTotalPages());
        model.addAttribute("totalElements", products.getTotalElements());
        model.addAttribute("keyword", keyword);

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
