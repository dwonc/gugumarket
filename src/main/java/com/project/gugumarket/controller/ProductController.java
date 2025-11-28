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

@Slf4j  // 로그를 사용할 수 있게 해줌 (log.info(), log.error() 등)
@RequiredArgsConstructor  // final 필드들을 자동으로 생성자 주입해줌
@RestController  // 이 클래스가 REST API 컨트롤러임을 선언 (JSON 응답 자동 변환)
@RequestMapping("/api")  // 모든 요청 URL 앞에 /api가 붙음
public class ProductController {

        private final ProductService productService; // 상품 관련 비즈니스 로직
        private final UserService userService;      // 사용자 관련 비즈니스 로직
        private final LikeService likeService;      // 좋아요 관련 비즈니스 로직
        private final CategoryService categoryService; // 카테고리 관련 비즈니스 로직
        private final ReportService reportService;  // 신고 관련 비즈니스 로직

    /**
     * 상품 등록 폼 데이터 조회
     */
    @GetMapping("/products/new")        // GET 요청 매핑
    public ResponseEntity<?> createForm(Principal principal) {
        // Principal: Spring Security가 제공하는 현재 로그인한 사용자 정보
        // JWT 토큰을 파싱해서 사용자 이메일을 principal.getName()으로 가져올 수 있음

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)       // 401 상태 코드
                    .body(Map.of(
                            "success", false,
                            "message", "로그인이 필요합니다.",
                            "needLogin", true   // 프론트엔드에서 로그인 페이지로 리다이렉트할 때 사용
                    ));
        }
        try {   // 현재 로그인한 사용자 정보 가져오기
            User user = userService.getUser(principal.getName());
            // principal.getName() = 이메일
            UserSimpleResponse userDTO = UserSimpleResponse.from(user);
            // 엔티티 → DTO 변환
            

            List<CategoryDto> categories = categoryService.getAllCategories();
            // 카테고리 목록 가져오기 (드롭다운에 표시할 용도)

            ProductForm productForm = new ProductForm();
            // 빈 폼 객체 생성 (프론트엔드에서 폼 초기화용)

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "productDto", productForm,      // 빈 폼
                    "categories", categories,        // 카테고리 목록
                    "user", userDTO                  // 현재 사용자 정보
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)      // 500 에러
                    .body(Map.of(
                            "success", false,
                            "message", "오류가 발생했습니다: " + e.getMessage()
                    ));
        }
    }

    /**
     * 상품 등록 처리
     */
    @PostMapping("/products/write")     // POST 매핑 요청
    public ResponseEntity<?> create(
            @Valid @RequestBody ProductForm productForm, 
            // @Valid: 유효성 검사, @RequestBody: JSON → 객체 변환
            BindingResult bindingResult,
            // 유효성 검사 결과를 담는 객체
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
            Map<String, String> errors = new HashMap<>();       // 에러가 있으면 에러 메시지들을 Map으로 만듦
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
            User user = userService.getUser(principal.getName());       // 현재 사용자
            Product product = productService.create(productForm, user); // DB에 저장

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "success", true,
                            "message", "상품이 등록되었습니다.",
                            "productId", product.getProductId() // 생성된 상품의 ID 반환
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
    public ResponseEntity<?> editForm(@PathVariable Long id, //URL 에서 상품 ID 추출
                                        Principal principal) {
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
            User user = userService.getUser(currentUser);  // 현재 사용자
            Product product = productService.getProduct(id);    // 수정할 상품

            // ✅ 권한 확인 - 판매자이거나 관리자인 경우만 수정 가능
            if (!product.getSeller().equals(user) && !"ADMIN".equals(user.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "success", false,
                                "message", "수정 권한이 없습니다."
                        ));
            }

            // 엔티티 -> DTO 로 변환 ( 프론트엔드가 받기 편한 형태로 변환 )
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
                    "productDto", productDto,        // 기존 상품 정보
                    "categories", categories,        // 카테고리 목록
                    "user", userDto,                 // 사용자 정보
                    "isEdit", true                   // 수정 모드임을 알림
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
    @PutMapping("/products/{id}")       // PUT 매핑 요청
    public ResponseEntity<?> update(
            @PathVariable Long id,      // 수정할 상품 ID
            @Valid @RequestBody ProductForm productDto, // 수정된 데이터
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

        if (bindingResult.hasErrors()) {        // 유효성 검사
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

        try {   // 권한 확인
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

            productService.modify(id, productDto, user); // 상품  수정

            return ResponseEntity.ok(Map.of(    // 성공 응답
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
    @DeleteMapping("/products/{id}")    // DELETE 매핑 요청
    public ResponseEntity<?> delete(@PathVariable Long id,      // 삭제할 상품 ID
                                        Principal principal) {
        
        if (principal == null) {      //로그인 확인  
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "message", "로그인이 필요합니다.",
                            "needLogin", true
                    ));
        }

        try {   // 권한확인 - 판매자이거나 관리자인 경우만 삭제 가능
            String currentUser = principal.getName();
            User user = userService.getUser(currentUser);
            Product product = productService.getProduct(id);
 
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

            //  로그 기록
            log.info("✅ 상품 삭제 - ID: {}, 삭제자: {} (역할: {})",
                    id, user.getUserName(), user.getRole());

            // 상품 삭제
            productService.delete(product);

            // 성공 응답
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
    @PutMapping("/products/{id}/status")        // PUT 매핑 요청
    public ResponseEntity<?> changeStatus(
            @PathVariable Long id,    
            @RequestBody ProductStatusRequest request,
            Principal principal) {

        if (principal == null) {        // 로그인 확인
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "message", "로그인이 필요합니다.",
                            "needLogin", true
                    ));
        }

        try {   // 권한 확인
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

            // 상태 변경
            productService.changeStatus(id, request.getStatus());

            //  성공 응답
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
    @GetMapping("/products/{id}")       // GET 요청 매핑
    public ResponseEntity<?> detail(@PathVariable Long id, Principal principal) {

        try {
                // 상품 정보 조회
            Product product = productService.getProduct(id);

                // 조회수 증가 함수
            productService.incrementViewCount(id);

                // product 엔티티 -> DTO 변환
            ProductDetailResponse productDto = ProductDetailResponse.from(product);

                // 추가 정보 조회
            Long likeCount = likeService.getLikeCount(product); // 좋아요 개수
            long reportCount = reportService.getReportCountByProduct(id);  // 신고 건 수 

                // 응답 데이터 구성
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("product", productDto);
            response.put("likeCount", likeCount);
            response.put("reportCount", reportCount);

                // 로그인  사용자 추가 정보 처리
            if (principal != null) {    // 로그인한 경우
                User currentUser = userService.getUser(principal.getName());

                // 현재 사용자가 이 상품을 찜했는지 확인
                boolean isLiked = likeService.isLiked(currentUser, product);
                response.put("isLiked", isLiked);

                // 현재 사용자가 판매자인 경우
                if (product.getSeller().equals(currentUser)) {

                        //이 상품에 관심 표시한 구매자 목록 가져오기
                    List<User> interestedBuyers = likeService.getUsersWhoLikedProduct(id);

                    // User 엔티티 -> DTO 변환
                    List<UserSimpleResponse> buyerList = interestedBuyers.stream()
                            .map(UserSimpleResponse::from)
                            .collect(Collectors.toList());

                    response.put("interestedBuyers", buyerList);
                }
            } else {    // 비로그인 사용자
                response.put("isLiked", false);
            }

                // 응답 반환
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
    @GetMapping("/products/list")       // GET 매핑 요청
    public ResponseEntity<?> getProductList(
            @RequestParam(required = false) String district,    // 선택적 파라미터
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page, // 기본값이 있는 파라미터
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdDate,desc") String[] sort,     // 배열로 받음
                Principal principal) {

        try {   
            Sort.Order order;   // 정렬 옵션 파싱

            if (sort.length == 2) {     // ["createDate", "desc"] 형태
                String property = sort[0];      // 정렬 기준 컬럼
                String direction = sort[1];     // asc 또는 desc

                        // 방향에 따라 Sort.Order 생성
                order = direction.equalsIgnoreCase("asc")
                        ? Sort.Order.asc(property)
                        : Sort.Order.desc(property);
            } else {
                order = Sort.Order.desc("createdDate");
                // 정렬 파라미터가 이상하면 기본값 사용
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by(order));
            // Pageable : 페이지 번호 + 크기 + 정렬 정보를 담는 객체

            Page<ProductDto> products = productService.getProductsWithFilters(
                // DB에서 상품 조회 ( 필터 + 페이징 + 정렬 )
                    district, categoryId, keyword, pageable);

        // 🔥 로그인한 사용자의 찜 여부 설정
        if (principal != null) {
                try {
                    User user = userService.getUser(principal.getName());
                    List<Long> likedProductIds = likeService.getLikedProductIds(user);
                    // 현재 사용자가 찜한 상품 ID 리스트 가져오기
                    
                    log.info("❤️ 로그인 사용자: {} (ID: {})", user.getUserName(), user.getUserId());
                    log.info("❤️ 찜한 상품 {}개: {}", likedProductIds.size(), likedProductIds);
    
                    // 각 상품에 찜 여부 설정
                    products.getContent().forEach(productDto -> {
                        boolean isLiked = likedProductIds.contains(productDto.getProductId());
                        // 찜한 상품 목록에 현재 상품 ID가 있는지 확인
                        
                        productDto.setIsLiked(isLiked);
                        
                        if (isLiked) {
                            log.info("❤️ 상품 ID {} 찜됨 표시", productDto.getProductId());
                        }
                    });
                } catch (Exception e) {
                    log.error("❌ 찜 여부 설정 실패: {}", e.getMessage());
                    // 찜 여부 설정 실패해도 상품 목록은 반환
                }
            } else {
                log.info("⚠️ 비로그인 사용자 - 모든 상품 isLiked = false");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("content", products.getContent());           // 상품 목록
            response.put("currentPage", products.getNumber());        // 현재 페이지 (0부터 시작)
            response.put("totalPages", products.getTotalPages());     // 전체 페이지 수
            response.put("totalElements", products.getTotalElements()); // 전체 상품 수
            response.put("size", products.getSize());                 // 페이지 크기
            response.put("first", products.isFirst());                // 첫 페이지 여부
            response.put("last", products.isLast());                  // 마지막 페이지 여부


            return ResponseEntity.ok(response); // 성공 응답 반환

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