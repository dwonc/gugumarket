package com.project.gugumarket.controller;

import com.project.gugumarket.dto.CategoryDto;
import com.project.gugumarket.dto.ProductDetailResponse;
import com.project.gugumarket.dto.ProductDto;
import com.project.gugumarket.dto.ProductForm;
import com.project.gugumarket.dto.ProductStatusRequest;
import com.project.gugumarket.dto.UserSimpleResponse;
import com.project.gugumarket.entity.Product;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class ProductController {

    private final ProductService productService;
    private final UserService userService;
    private final LikeService likeService;
    private final CategoryService categoryService;
    private final ReportService reportService;

    /**
     * 상품 등록 폼 페이지
     */
    @GetMapping("/products/new")
    public ResponseEntity<?> createForm(Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "message", "로그인이 필요합니다.",
                            "needLogin", true
                    ));
        }
        try {

            User user = userService.getUser(principal.getName());
            UserSimpleResponse userDTO = UserSimpleResponse.from(user);
            List<CategoryDto> categories = categoryService.getAllCategories();
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

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "message", "로그인이 필요합니다.",
                            "needLogin", true
                    ));
        }

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
            User user = userService.getUser(principal.getName());
            Product product = productService.create(productForm, user);

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

            // ✅ 권한 확인 - 판매자이거나 관리자인 경우만 수정 가능
            if (!product.getSeller().equals(user) && !"ADMIN".equals(user.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "success", false,
                                "message", "수정 권한이 없습니다."
                        ));
            }

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

            List<CategoryDto> categories = categoryService.getAllCategories();
            UserSimpleResponse userDto = UserSimpleResponse.from(user);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "productDto", productDto,
                    "categories", categories,
                    "user", userDto,
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

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "message", "로그인이 필요합니다.",
                            "needLogin", true
                    ));
        }

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

            // ✅ 권한 확인 - 판매자이거나 관리자인 경우만 수정 가능
            if (!product.getSeller().equals(user) && !"ADMIN".equals(user.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "success", false,
                                "message", "수정 권한이 없습니다."
                        ));
            }

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

            // ✅ 권한 확인 - 판매자이거나 관리자인 경우만 삭제 가능
            if (!product.getSeller().equals(user) && !"ADMIN".equals(user.getRole())) {
                log.warn("⚠️ 삭제 권한 없음 - 사용자: {}, 판매자: {}, 역할: {}",
                        user.getUserName(),
                        product.getSeller().getUserName(),
                        user.getRole());

                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "success", false,
                                "message", "삭제 권한이 없습니다."
                        ));
            }

            log.info("✅ 상품 삭제 - ID: {}, 삭제자: {} (역할: {})",
                    id, user.getUserName(), user.getRole());

            productService.delete(product);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "상품이 삭제되었습니다."
            ));

        } catch (Exception e) {
            log.error("❌ 상품 삭제 실패: {}", e.getMessage());
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

            // ✅ 권한 확인 - 판매자이거나 관리자인 경우만 상태 변경 가능
            if (!product.getSeller().equals(user) && !"ADMIN".equals(user.getRole())) {
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
    @GetMapping("/products/{id}")
    public ResponseEntity<?> detail(@PathVariable Long id, Principal principal) {

        try {
            Product product = productService.getProduct(id);

            productService.incrementViewCount(id);

            ProductDetailResponse productDto = ProductDetailResponse.from(product);

            Long likeCount = likeService.getLikeCount(product);
            long reportCount = reportService.getReportCountByProduct(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("product", productDto);
            response.put("likeCount", likeCount);
            response.put("reportCount", reportCount);

            if (principal != null) {
                User currentUser = userService.getUser(principal.getName());
                boolean isLiked = likeService.isLiked(currentUser, product);
                response.put("isLiked", isLiked);

                if (product.getSeller().equals(currentUser)) {
                    List<User> interestedBuyers = likeService.getUsersWhoLikedProduct(id);

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

    /**
     * 🔥 상품 목록 조회 (필터링 + 정렬)
     */
    @GetMapping("/products/list")
    public ResponseEntity<?> getProductList(
            @RequestParam(required = false) String district,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdDate,desc") String[] sort) {

        try {
            Sort.Order order;

            if (sort.length == 2) {
                String property = sort[0];
                String direction = sort[1];

                order = direction.equalsIgnoreCase("asc")
                        ? Sort.Order.asc(property)
                        : Sort.Order.desc(property);
            } else {
                order = Sort.Order.desc("createdDate");
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by(order));

            Page<ProductDto> products = productService.getProductsWithFilters(
                    district, categoryId, keyword, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("content", products.getContent());
            response.put("currentPage", products.getNumber());
            response.put("totalPages", products.getTotalPages());
            response.put("totalElements", products.getTotalElements());
            response.put("size", products.getSize());
            response.put("first", products.isFirst());
            response.put("last", products.isLast());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ 상품 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "상품 목록 조회 중 오류가 발생했습니다: " + e.getMessage()
                    ));
        }
    }

    /**
     * 🔥 지역(구) 목록 조회
     */
    @GetMapping("/districts")
    public ResponseEntity<?> getDistricts() {
        try {
            List<String> districts = productService.getDistinctDistricts();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "districts", districts
            ));

        } catch (Exception e) {
            log.error("❌ 지역 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "지역 목록 조회 중 오류가 발생했습니다: " + e.getMessage()
                    ));
        }
    }

    // ========== 🗺️ 지도 기능 API ==========

    // 🗺️ 지도에 표시할 모든 상품 조회 (인증 불필요)
    @GetMapping("/products/map")
    public ResponseEntity<?> getProductsForMap(
            @RequestParam(required = false) Integer maxPrice) {
        try {
            List<ProductDto> products = maxPrice != null
                    ? productService.getProductsForMapWithPrice(maxPrice)
                    : productService.getProductsForMap();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("products", products);
            response.put("count", products.size());

            log.info("🗺️ 지도용 상품 조회 API 호출 - {}개 (가격필터: {})",
                    products.size(), maxPrice != null ? maxPrice + "원 이하" : "전체");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ 지도용 상품 조회 실패", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "상품 조회 실패: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 지도 범위 내 상품 조회
     * GET /api/products/map/bounds?minLat=37.4&maxLat=37.6&minLng=126.9&maxLng=127.1
     */
    @GetMapping("/products/map/bounds")
    public ResponseEntity<?> getProductsInBounds(
            @RequestParam Double minLat,
            @RequestParam Double maxLat,
            @RequestParam Double minLng,
            @RequestParam Double maxLng) {
        try {
            List<ProductDto> products = productService.getProductsInBounds(minLat, maxLat, minLng, maxLng);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "products", products,
                    "count", products.size()
            ));

        } catch (Exception e) {
            log.error("❌ 범위 내 상품 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "범위 내 상품 조회 중 오류가 발생했습니다: " + e.getMessage()
                    ));
        }
    }

    /**
     * 🔧 관리자용: 기존 상품 좌표 일괄 업데이트
     * POST /api/products/map/update-coordinates
     */
    @PostMapping("/products/map/update-coordinates")
    public ResponseEntity<?> updateProductCoordinates(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "message", "로그인이 필요합니다."
                    ));
        }

        try {
            productService.updateProductCoordinates();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "상품 좌표 업데이트가 완료되었습니다."
            ));

        } catch (Exception e) {
            log.error("❌ 좌표 업데이트 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "좌표 업데이트 중 오류가 발생했습니다: " + e.getMessage()
                    ));
        }
    }
}