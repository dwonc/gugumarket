package com.project.gugumarket.controller;

import com.project.gugumarket.dto.CategoryDto;
import com.project.gugumarket.dto.ProductForm;
import com.project.gugumarket.dto.ProductStatusRequest;
import com.project.gugumarket.entity.Category;
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

/**
 * 상품 관련 요청을 처리하는 컨트롤러
 * 상품 등록, 조회, 수정, 삭제, 상태 변경 등의 기능을 담당
 */
@RequiredArgsConstructor  // final 필드에 대한 생성자 자동 생성
@Controller
public class ProductController {

    private final ProductService productService;  // 상품 비즈니스 로직 처리
    private final UserService userService;  // 사용자 관련 로직 처리
    private final LikeService likeService;  // 찜(좋아요) 기능 처리
    private final CategoryService categoryService;  // 카테고리 관련 로직 처리

    /**
     * 상품 등록 폼 페이지 표시
     * GET /product/new
     * 빈 폼과 카테고리 목록을 사용자에게 제공
     */
    @GetMapping("/product/new")
    public String createForm(Model model, Principal principal) {
        // 로그인하지 않은 경우 로그인 페이지로 리다이렉트
        if (principal == null) {
            return "redirect:/login";
        }

        // 현재 로그인한 사용자 정보 조회
        User user = userService.getUser(principal.getName());

        // 상품 카테고리 목록 조회 (예: 전자제품, 의류, 도서 등)
        List<CategoryDto> categories = categoryService.getAllCategories();

        // 빈 ProductForm 객체 생성 (폼 바인딩용)
        ProductForm productForm = new ProductForm();

        // 모델에 데이터 추가하여 뷰로 전달
        model.addAttribute("productDto", productForm);
        model.addAttribute("categories", categories);
        model.addAttribute("user", user);

        return "products/writeForm";  // 상품 등록 폼 페이지
    }

    /**
     * 상품 등록 처리
     * POST /product/write
     * 사용자가 입력한 상품 정보를 검증하고 데이터베이스에 저장
     */
    @PostMapping("/product/write")
    public String create(
            @Valid @ModelAttribute("productDto") ProductForm productForm,  // 유효성 검증
            BindingResult bindingResult,  // 검증 결과
            Principal principal,
            Model model) {

        // 로그인 확인 -- 1.로그인 여부
        if (principal == null) {
            return "redirect:/login";
        }

<<<<<<< HEAD
        // 유효성 검증 실패 시 (필수 필드 누락, 형식 오류 등)
=======
        // 유효성 검증 실패 시 -- 2.유효성 체크
>>>>>>> 28cebc40083f14c3d32f93518519a56ce9ec8b8a
        if (bindingResult.hasErrors()) {
            // 에러가 있으면 카테고리 목록과 사용자 정보를 다시 설정
            List<CategoryDto> categories = categoryService.getAllCategories();
            User user = userService.getUser(principal.getName());
            model.addAttribute("categories", categories);
            model.addAttribute("user", user);
            return "products/writeForm";  // 폼으로 돌아가서 에러 표시
        }

        try {
            // 현재 로그인한 사용자 정보 (판매자)
            User user = userService.getUser(principal.getName());

            // 상품 등록 (ProductService를 통해 비즈니스 로직 처리)
            Product product = productService.create(productForm, user);

            // 등록 성공 시 상품 상세 페이지로 리다이렉트
            return "redirect:/product/" + product.getProductId();

        } catch (Exception e) {
            // 예외 발생 시 에러 메시지 추가
            bindingResult.reject("createError", "상품 등록 중 오류가 발생했습니다: " + e.getMessage());

            // 폼에 필요한 데이터 다시 설정
            List<CategoryDto> categories = categoryService.getAllCategories();
            User user = userService.getUser(principal.getName());
            model.addAttribute("categories", categories);
            model.addAttribute("user", user);
            return "products/writeForm";
        }
    }

    /**
     * 상품 수정 폼 페이지 표시
     * GET /product/{id}/edit
     * 기존 상품 정보를 폼에 미리 채워서 보여줌
     * 판매자만 접근 가능
     */
    @GetMapping("/product/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, Principal principal) {
        // 로그인 확인
        if (principal == null) {
            return "redirect:/login";
        }

        // 현재 사용자 및 상품 정보 조회
        String currentUser = principal.getName();
        User user = userService.getUser(currentUser);
        Product product = productService.getProduct(id);

        // 권한 확인: 상품 판매자만 수정 가능
        if (!product.getSeller().equals(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "수정권한이 없습니다.");
        }

        // 기존 상품 정보를 ProductForm에 설정
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

        // 카테고리 목록 조회
        List<CategoryDto> categories = categoryService.getAllCategories();

        model.addAttribute("productDto", productDto);
        model.addAttribute("categories", categories);
        model.addAttribute("user", user);
        model.addAttribute("isEdit", true);  // 수정 모드 플래그 (폼에서 UI 구분용)

        return "products/writeForm";  // 등록 폼과 같은 템플릿 재사용
    }

    /**
     * 상품 수정 처리
     * POST /product/{id}/edit
     * 수정된 상품 정보를 검증하고 데이터베이스에 업데이트
     */
    @PostMapping("/product/{id}/edit")
    public String update(
            @PathVariable Long id,  // URL 경로에서 상품 ID 추출
            @Valid @ModelAttribute("productDto") ProductForm productDto,  // 수정된 정보
            BindingResult bindingResult,  // 유효성 검증 결과
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
            // 현재 사용자 및 상품 정보 조회
            String currentUser = principal.getName();
            User user = userService.getUser(currentUser);
            Product product = productService.getProduct(id);

            // 권한 확인: 판매자만 수정 가능
            if (!product.getSeller().equals(user)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "수정권한이 없습니다.");
            }

<<<<<<< HEAD
            // 상품 정보 수정 (이미지 URL로 수정, MultipartFile이 아님)
            productService.modify(product, productDto);
=======
            // ✅ 이미지 URL로 수정 (MultipartFile이 아님)
            // productService.modify(product, productDto);
            productService.modify(id, productDto, user);
>>>>>>> 28cebc40083f14c3d32f93518519a56ce9ec8b8a

            // 수정 완료 후 상품 상세 페이지로 리다이렉트
            return "redirect:/product/" + id;

        } catch (Exception e) {
            // 에러 발생 시 처리
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
     * 상품 삭제
     * DELETE /product/{id}
     * AJAX 요청으로 상품을 삭제하고 JSON 응답 반환
     * 판매자만 삭제 가능
     */
    @DeleteMapping("/product/{id}")
    @ResponseBody  // JSON 형태로 응답
    public ResponseEntity<?> delete(@PathVariable Long id, Principal principal) {
        // 로그인 확인
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }

        try {
            // 현재 사용자 및 상품 정보 조회
            String currentUser = principal.getName();
            User user = userService.getUser(currentUser);
            Product product = productService.getProduct(id);

            // 권한 확인: 판매자만 삭제 가능
            if (!product.getSeller().equals(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "삭제권한이 없습니다."));
            }

            // 상품 삭제
            productService.delete(product);

            // 성공 응답 반환
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "상품이 삭제되었습니다."
            ));

        } catch (Exception e) {
            // 에러 발생 시
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "삭제 중 오류가 발생했습니다."));
        }
    }

    /**
     * 상품 상태 변경
     * PUT /product/{id}/status
     * AJAX 요청으로 상품 상태를 변경 (예: 판매중 → 예약중 → 판매완료)
     * 판매자만 변경 가능
     */
    @PutMapping("/product/{id}/status")
    @ResponseBody  // JSON 응답
    public ResponseEntity<?> changeStatus(
            @PathVariable Long id,
            @RequestBody ProductStatusRequest request,  // JSON 데이터에서 상태 정보 추출
            Principal principal) {

        // 로그인 확인
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }

        try {
            // 현재 사용자 및 상품 정보 조회
            String currentUser = principal.getName();
            User user = userService.getUser(currentUser);
            Product product = productService.getProduct(id);

            // 판매자 권한 확인
            if (!product.getSeller().equals(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "권한이 없습니다."));
            }

            // 상태 변경 (예: "판매중" → "예약중")
            productService.changeStatus(id, request.getStatus());

            // 성공 응답 반환
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "상태가 변경되었습니다.",
                    "status", request.getStatus()  // 변경된 상태 반환
            ));

        } catch (Exception e) {
            // 에러 발생 시
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "상태 변경에 실패했습니다."));
        }
    }

    /**
     * 상품 상세 페이지 표시
     * GET /product/{id}
     * 상품 정보, 찜 개수, 판매자의 경우 구매 희망자 목록 표시
     * 페이지 조회 시 조회수 자동 증가
     */
    @GetMapping("/product/{id}")
    public String detail(@PathVariable Long id, Model model, Principal principal) {
        // 상품 정보 조회
        Product product = productService.getProduct(id);

        // 조회수 증가 (중복 조회 방지 로직은 서비스에서 처리)
        productService.incrementViewCount(id);

        model.addAttribute("product", product);

        // 이 상품의 총 찜(좋아요) 개수
        Long likeCount = likeService.getLikeCount(product);
        model.addAttribute("likeCount", likeCount);

        // 로그인한 사용자의 경우 추가 정보 제공
        if (principal != null) {
            User currentUser = userService.getUser(principal.getName());

            // 현재 사용자가 이 상품을 찜했는지 여부
            boolean isLiked = likeService.isLiked(currentUser, product);
            model.addAttribute("isLiked", isLiked);

            // 판매자인 경우 구매 희망자(찜한 사용자) 목록 표시
            if (product.getSeller().equals(currentUser)) {
                List<User> interestedBuyers = likeService.getUsersWhoLikedProduct(id);
                model.addAttribute("interestedBuyers", interestedBuyers);
            }
        } else {
            // 비로그인 사용자는 찜 기능 사용 불가
            model.addAttribute("isLiked", false);
        }

        return "products/detail";  // 상품 상세 페이지
    }
}