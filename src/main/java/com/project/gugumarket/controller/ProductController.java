package com.project.gugumarket.controller;

import com.project.gugumarket.dto.CategoryDto;
import com.project.gugumarket.dto.ProductDetailResponse;
import com.project.gugumarket.dto.ProductForm;
import com.project.gugumarket.dto.ProductStatusRequest;
import com.project.gugumarket.dto.UserSimpleResponse;
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
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor  //생성자 자동 주입 
@RestController          
@RequestMapping("/api")     //API 명시
public class ProductController {

    private final AdminController adminController;

    private final ProductService productService;
    private final UserService userService;
    private final LikeService likeService;
    private final CategoryService categoryService;

    /**
     * 상품 등록 폼 페이지
     */
    @GetMapping("/products/new")
    public ResponseEntity<?> createForm(Principal principal) {
        
        if (principal == null) {        // 로그인 확인 로그인 안되어있으면 로그인 창으로 이동
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of(
                    "success", false,
                    "message", "로그인이 필요합니다.",
                    "needLogin", true
            ));
        }
        try {
            
        // 현재 로그인한 사용자 정보
        User user = userService.getUser(principal.getName());

        //User entity -> DTO 변환
        UserSimpleResponse userDTO = UserSimpleResponse.from(user);
        

        // 카테고리 목록 조회
        List<CategoryDto> categories = categoryService.getAllCategories();

        // 빈 ProductForm 객체
        ProductForm productForm = new ProductForm();

        return ResponseEntity.ok(Map.of(
            "success", true,
            "productDto", productForm,
            "categories", categories,
            "user", userDTO
        ));

    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of(
                    "success", false,
                    "message", "오류가 발생했습니다: " + e.getMessage()
            ));
     }
    }

    /**
     * 상품 등록 처리
     */
    @PostMapping("/products/write")
    public ResponseEntity<?> create(
            @Valid @RequestBody ProductForm productForm,
            BindingResult bindingResult,
            Principal principal) {

        // 로그인 확인 -- 1.로그인 여부
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of(
                    "success", false,
                    "message", "로그인이 필요합니다.",
                    "needLogin", true
            ));
}

        // 유효성 검증 실패 시 -- 2.유효성 체크
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", "입력값이 올바르지 않습니다.",
                            "errors", errors
                    ));
        }

        try {
            // 현재 사용자 정보
            User user = userService.getUser(principal.getName());

            // 상품 등록
            Product product = productService.create(productForm, user);

            // 상세 페이지로 리다이렉트
            return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of(
                    "success", true,
                    "message", "상품이 등록되었습니다.",
                    "productId", product.getProductId()
            ));

            } catch (Exception e) {
                 return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of(
                    "success", false,
                    "message", "상품 등록 중 오류가 발생했습니다: " + e.getMessage()
                    ));
                }
        }

    /**
     * 상품 수정 폼 데이터 조회
     */
    @GetMapping("/products/{id}/edit")
    public ResponseEntity<?> editForm(@PathVariable Long id, Principal principal) {
        // 로그인 확인
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "message", "로그인이 필요합니다.",
                            "needLogin", true
                    ));
        }

        try{
        String currentUser = principal.getName();
        User user = userService.getUser(currentUser);
        Product product = productService.getProduct(id);

        // 권한 확인
        if (!product.getSeller().equals(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "success", false,
                            "message", "수정 권한이 없습니다."
                    ));
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

        //User Entity -> DTO변환
        UserSimpleResponse userDto = UserSimpleResponse.from(user);

        return ResponseEntity.ok(Map.of(
                    "success", true,
                    "productDto", productDto,
                    "categories", categories,
                    "user", user,
                    "isEdit", true
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "상품 정보 조회 중 오류가 발생했습니다: " + e.getMessage()
                    ));
        }
    }

    /**
     * 상품 수정 처리
     */
    @PutMapping("/products/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductForm productDto,
            BindingResult bindingResult,
            Principal principal) {

        // 로그인 확인
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "message", "로그인이 필요합니다.",
                            "needLogin", true
                    ));
        }

        // 유효성 검증 실패 시
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
            );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of(
                "success", false,
                "message", "입력값이 올바르지 않습니다.",
                "errors", errors
            ));
        }

        try {
            String currentUser = principal.getName();
            User user = userService.getUser(currentUser);
            Product product = productService.getProduct(id);

            // 권한 확인
            if (!product.getSeller().equals(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "success", false,
                                "message", "수정 권한이 없습니다."
                        ));
            }

            //상품 수정
            productService.modify(id, productDto, user);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "상품이 수정되었습니다.",
                    "productId", id
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "상품 수정 중 오류가 발생했습니다: " + e.getMessage()
                    ));
        }
    }

    /**
     * 상품 삭제
     */
    @DeleteMapping("/products/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Principal principal) {
        // 로그인 확인
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "message", "로그인이 필요합니다.",
                            "needLogin", true
                    ));
        }

        try {
            String currentUser = principal.getName();
            User user = userService.getUser(currentUser);
            Product product = productService.getProduct(id);

            // 권한 확인
            if (!product.getSeller().equals(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "success", false,
                                "message", "삭제 권한이 없습니다."
                        ));
            }

            productService.delete(product);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "상품이 삭제되었습니다."
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "삭제 중 오류가 발생했습니다: " + e.getMessage()
                    ));
        }
    }

    /**
     * 상태 변경
     */
    @PutMapping("/products/{id}/status")
    public ResponseEntity<?> changeStatus(
            @PathVariable Long id,
            @RequestBody ProductStatusRequest request,
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "message", "로그인이 필요합니다.",
                            "needLogin", true
                    ));
        }

        try {
            String currentUser = principal.getName();
            User user = userService.getUser(currentUser);
            Product product = productService.getProduct(id);

            // 판매자 권한 확인
            if (!product.getSeller().equals(user)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "success", false,
                                "message", "권한이 없습니다."
                        ));
            }

            productService.changeStatus(id, request.getStatus());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "상태가 변경되었습니다.",
                    "status", request.getStatus()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "상태 변경에 실패했습니다: " + e.getMessage()
                    ));
        }
    }

    /**
     * 상품 상세 조회
     */
    @GetMapping("/products/{id}")  // ⭐ products로 변경!
    public ResponseEntity<?> detail(@PathVariable Long id, Principal principal) {
        
        try {
            Product product = productService.getProduct(id);

            // 조회수 증가
            productService.incrementViewCount(id);

            //Entity ->DTO 변환
            ProductDetailResponse productDto = ProductDetailResponse.from(product);

            // 좋아요 개수
            Long likeCount = likeService.getLikeCount(product);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("product", product);
            response.put("likeCount", likeCount);

            // 로그인한 사용자의 좋아요 여부 및 구매 희망자 목록
            if (principal != null) {
                User currentUser = userService.getUser(principal.getName());
                boolean isLiked = likeService.isLiked(currentUser, product);
                response.put("isLiked", isLiked);

               // 판매자인 경우 구매 희망자 목록
               if (product.getSeller().equals(currentUser)) {
                List<User> interestedBuyers = likeService.getUsersWhoLikedProduct(id);

                // ⭐ User Entity → DTO 변환 (무한 순환 참조 방지!)
                List<UserSimpleResponse> buyerList = interestedBuyers.stream()
                        .map(UserSimpleResponse::from)
                        .collect(Collectors.toList());

                response.put("interestedBuyers", buyerList);
            }
        } else {
            response.put("isLiked", false);
        }

        return ResponseEntity.ok(response);

    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "success", false,
                        "message", "상품 조회 중 오류가 발생했습니다: " + e.getMessage()
                ));
    }
}
}