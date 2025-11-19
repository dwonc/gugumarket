package com.project.gugumarket.controller;

import com.project.gugumarket.NotificationType;
import com.project.gugumarket.dto.CategoryDto;
import com.project.gugumarket.dto.ProductForm;
import com.project.gugumarket.entity.Product;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.service.CategoryService;
import com.project.gugumarket.service.LikeService;
import com.project.gugumarket.service.ProductService;
import com.project.gugumarket.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Controller
public class ProductController {

    private final ProductService productService;
    private final UserService userService;
    private final LikeService likeService;
    private final CategoryService categoryService;

    /**
     * 상품 등록 폼 페이지
     */
    @GetMapping("/product/new")
    public String createForm(Model model, Principal principal) {
        // 로그인 확인
        if (principal == null) {
            return "redirect:/login";
        }

        // 현재 로그인한 사용자 정보
        User user = userService.getUser(principal.getName());

        // 카테고리 목록 조회
        List<CategoryDto> categories = categoryService.getAllCategories();

        // 빈 ProductForm 객체
        ProductForm productForm = new ProductForm();

        // Model에 추가
        model.addAttribute("productDto", productForm);
        model.addAttribute("categories", categories);
        model.addAttribute("user", user);

        return "products/writeForm";
    }

    /**
     * 상품 등록 처리
     */
    @PostMapping("/product/write")
    public String create(
            @Valid @ModelAttribute("productDto") ProductForm productForm,
            BindingResult bindingResult,
            Principal principal,
            Model model) {

        // 로그인 확인 -- 1.로그인 여부
        if (principal == null) {
            return "redirect:/login";
        }

        // 유효성 검증 실패 시 -- 2.유효성 체크
        if (bindingResult.hasErrors()) {
            List<CategoryDto> categories = categoryService.getAllCategories();
            User user = userService.getUser(principal.getName());
            model.addAttribute("categories", categories);
            model.addAttribute("user", user);
            return "products/writeForm";
        }

        try {
            // 현재 사용자 정보
            User user = userService.getUser(principal.getName());

            // 상품 등록
            Product product = productService.create(productForm, user);

            // 상세 페이지로 리다이렉트
            return "redirect:/product/" + product.getProductId();

        } catch (Exception e) {
            // 에러 발생 시
            bindingResult.reject("createError", "상품 등록 중 오류가 발생했습니다: " + e.getMessage());

            List<CategoryDto> categories = categoryService.getAllCategories();
            User user = userService.getUser(principal.getName());
            model.addAttribute("categories", categories);
            model.addAttribute("user", user);
            return "products/writeForm";
        }
    }

    /**
     * 상품 수정 폼 페이지 (✅ 개선)
     */
    @GetMapping("/product/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, Principal principal) {
        // 로그인 확인
        if (principal == null) {
            return "redirect:/login";
        }

        String currentUser = principal.getName();
        User user = userService.getUser(currentUser);
        Product product = productService.getProduct(id);

        // 권한 확인
        if (!product.getSeller().equals(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "수정권한이 없습니다.");
        }

        // ✅ ProductForm에 모든 데이터 설정
        ProductForm productDto = new ProductForm();
        productDto.setProductId(product.getProductId());
        productDto.setCategoryId(product.getCategory().getCategoryId());
        productDto.setTitle(product.getTitle());
        productDto.setPrice(product.getPrice());
        productDto.setContent(product.getContent());
        productDto.setMainImage(product.getMainImage());
        productDto.setBankName(product.getBankName());
        productDto.setAccountNumber(product.getAccountNumber());
        productDto.setAccountHolder(product.getAccountHolder());

        // ✅ 카테고리 목록 추가
        List<CategoryDto> categories = categoryService.getAllCategories();

        model.addAttribute("productDto", productDto);
        model.addAttribute("categories", categories);
        model.addAttribute("user", user);
        model.addAttribute("isEdit", true); // 수정 모드 플래그

        return "products/writeForm";
    }

    /**
     * 상품 수정 처리 (✅ 개선)
     */
    @PostMapping("/product/{id}/edit")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("productDto") ProductForm productDto,
            BindingResult bindingResult,
            Principal principal,
            Model model) {

        // 로그인 확인
        if (principal == null) {
            return "redirect:/login";
        }

        // 유효성 검증 실패 시
        if (bindingResult.hasErrors()) {
            List<CategoryDto> categories = categoryService.getAllCategories();
            User user = userService.getUser(principal.getName());
            model.addAttribute("categories", categories);
            model.addAttribute("user", user);
            model.addAttribute("isEdit", true);
            return "products/writeForm";
        }

        try {
            String currentUser = principal.getName();
            User user = userService.getUser(currentUser);
            Product product = productService.getProduct(id);

            // 권한 확인
            if (!product.getSeller().equals(user)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "수정권한이 없습니다.");
            }

            // ✅ 이미지 URL로 수정 (MultipartFile이 아님)
            // productService.modify(product, productDto);
            productService.modify(id, productDto, user);

            // ✅ 올바른 경로로 리다이렉트
            return "redirect:/product/" + id;

        } catch (Exception e) {
            bindingResult.reject("updateError", "상품 수정 중 오류가 발생했습니다: " + e.getMessage());

            List<CategoryDto> categories = categoryService.getAllCategories();
            User user = userService.getUser(principal.getName());
            model.addAttribute("categories", categories);
            model.addAttribute("user", user);
            model.addAttribute("isEdit", true);
            return "products/writeForm";
        }
    }

    /**
     * 상품 삭제 (✅ 개선 - JSON 응답)
     */
    @DeleteMapping("/product/{id}")
    @ResponseBody
    public ResponseEntity<?> delete(@PathVariable Long id, Principal principal) {
        // 로그인 확인
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }

        try {
            String currentUser = principal.getName();
            User user = userService.getUser(currentUser);
            Product product = productService.getProduct(id);

            // 권한 확인
            if (!product.getSeller().equals(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "삭제권한이 없습니다."));
            }

            productService.delete(product);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "상품이 삭제되었습니다."
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "삭제 중 오류가 발생했습니다."));
        }
    }

    /**
     * 상태 변경
     */
    @PutMapping("/product/{id}/status")
    @ResponseBody
    public ResponseEntity<?> changeStatus(
            @PathVariable Long id,
            @RequestBody NotificationType.ProductStatusRequest request,
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }

        try {
            String currentUser = principal.getName();
            User user = userService.getUser(currentUser);
            Product product = productService.getProduct(id);

            // 판매자 권한 확인
            if (!product.getSeller().equals(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "권한이 없습니다."));
            }

            productService.changeStatus(id, request.getStatus());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "상태가 변경되었습니다.",
                    "status", request.getStatus()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "상태 변경에 실패했습니다."));
        }
    }

    /**
     * 상품 상세 페이지 (✅ 개선 - 조회수 추가)
     */
    @GetMapping("/product/{id}")
    public String detail(@PathVariable Long id, Model model, Principal principal) {
        Product product = productService.getProduct(id);

        // ✅ 조회수 증가
        productService.incrementViewCount(id);

        model.addAttribute("product", product);

        // 좋아요 개수
        Long likeCount = likeService.getLikeCount(product);
        model.addAttribute("likeCount", likeCount);

        // 로그인한 사용자의 좋아요 여부 및 구매 희망자 목록
        if (principal != null) {
            User currentUser = userService.getUser(principal.getName());
            boolean isLiked = likeService.isLiked(currentUser, product);
            model.addAttribute("isLiked", isLiked);

            // 판매자인 경우 구매 희망자 목록 추가
            if (product.getSeller().equals(currentUser)) {
                List<User> interestedBuyers = likeService.getUsersWhoLikedProduct(id);
                model.addAttribute("interestedBuyers", interestedBuyers);
            }
        } else {
            // 비로그인 사용자
            model.addAttribute("isLiked", false);
        }

        return "products/detail";
    }
}