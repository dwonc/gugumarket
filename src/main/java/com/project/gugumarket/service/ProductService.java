package com.project.gugumarket.service;

import com.project.gugumarket.DataNotFoundException;
import com.project.gugumarket.ProductStatus;
import com.project.gugumarket.dto.ProductDto;
import com.project.gugumarket.dto.ProductForm;
import com.project.gugumarket.entity.Category;
import com.project.gugumarket.entity.Product;
import com.project.gugumarket.entity.ProductImage;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.ProductImageRepository;
import com.project.gugumarket.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 상품 관련 비즈니스 로직을 처리하는 서비스
 * 상품 등록, 조회, 수정, 삭제, 상태 변경 등의 핵심 기능을 담당
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;  // 상품 데이터베이스 접근
    private final ProductImageRepository productImageRepository;  // 상품 이미지 데이터베이스 접근
    private final CategoryService categoryService;  // 카테고리 관련 로직
    private final FileService fileService;  // 파일 업로드/삭제 처리

    /**
     * 상품 ID로 상품 조회
     * @param id 조회할 상품 ID
     * @return Product 엔티티
     * @throws DataNotFoundException 상품을 찾을 수 없을 때
     */
    public Product getProduct(Long id) {
        Optional<Product> product = this.productRepository.findById(id);

        if(product.isPresent())
            return product.get();
        else
            throw new DataNotFoundException("Product not found");
    }

/**
 * 상품 수정
 */
@Transactional
public void modify(Long productId, ProductForm productDto, User currentUser) {
    // Service 안에서 조회 (영속 상태 유지)
    Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

    // 권한 확인
    if (!product.getSeller().equals(currentUser)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "수정권한이 없습니다.");
    }

    // 필드 수정
    product.setTitle(productDto.getTitle());
    product.setPrice(productDto.getPrice());
    product.setContent(productDto.getContent());
    product.setBankName(productDto.getBankName());
    product.setAccountNumber(productDto.getAccountNumber());
    product.setAccountHolder(productDto.getAccountHolder());

    // 카테고리 변경
    Category category = categoryService.getCategoryById(productDto.getCategoryId());
    product.setCategory(category);

    // 메인 이미지 변경 처리
    if (productDto.getMainImage() != null && !productDto.getMainImage().isEmpty()) {
        if (!productDto.getMainImage().equals(product.getMainImage())) {
            if (product.getMainImage() != null) {
                try {
                    String oldFileName = product.getMainImage().substring(
                            product.getMainImage().lastIndexOf("/") + 1);
                    fileService.deleteFile(oldFileName);
                } catch (IOException e) {
                    log.error("⚠️ 기존 메인 이미지 삭제 실패: {}", e.getMessage());
                }
            }
            product.setMainImage(productDto.getMainImage());
        }
    }

    // ✅ 추가 이미지 업데이트 (스마트 업데이트)
    if (productDto.getAdditionalImages() != null) {
        log.info("🔄 추가 이미지 업데이트 시작");
        log.info("📥 새로운 이미지 개수: {}", productDto.getAdditionalImages().size());

        // 1. 기존 추가 이미지 조회
        List<ProductImage> existingImages = productImageRepository.findByProduct(product);
        log.info("📦 기존 이미지 개수: {}", existingImages.size());

        // 2. 기존 이미지 URL 목록
        List<String> existingUrls = existingImages.stream()
                .map(ProductImage::getImageUrl)
                .collect(Collectors.toList());

        // 3. 새로운 이미지 URL 목록
        List<String> newUrls = productDto.getAdditionalImages();

        // 4. 삭제할 이미지 찾기 (기존에는 있지만 새 목록에는 없는 것)
        List<String> urlsToDelete = existingUrls.stream()
                .filter(url -> !newUrls.contains(url))
                .collect(Collectors.toList());

        log.info("🗑️ 삭제할 이미지: {}", urlsToDelete.size());

        // 5. 삭제할 이미지만 파일 삭제
        if (!urlsToDelete.isEmpty()) {
            for (String urlToDelete : urlsToDelete) {
                try {
                    String fileName = urlToDelete.substring(
                            urlToDelete.lastIndexOf("/") + 1);
                    fileService.deleteFile(fileName);
                    log.info("🗑️ 파일 삭제: {}", fileName);
                } catch (IOException e) {
                    log.error("⚠️ 파일 삭제 실패: {}", e.getMessage());
                }
            }
        }

        // 6. DB에서 기존 이미지 모두 삭제 (재정렬을 위해)
        if (!existingImages.isEmpty()) {
            productImageRepository.deleteAll(existingImages);
            log.info("✅ DB에서 기존 이미지 {}개 삭제 완료", existingImages.size());
        }

        // 7. 새로운 이미지 목록 전체 저장 (순서 유지)
        if (!newUrls.isEmpty()) {
            List<ProductImage> newImages = new ArrayList<>();

            for (int i = 0; i < newUrls.size(); i++) {
                String imageUrl = newUrls.get(i);

                ProductImage productImage = ProductImage.builder()
                        .product(product)
                        .imageUrl(imageUrl)
                        .imageOrder(i + 1)
                        .build();

                newImages.add(productImage);
            }

            productImageRepository.saveAll(newImages);
            log.info("✅ 새로운 추가 이미지 {}개 저장 완료", newImages.size());
        }
    }

    // 변경사항 저장
    productRepository.save(product);
    log.info("✅ 상품 수정 완료: {}", product.getTitle());
}
    /**
     * 상품 조회수 증가
     * 상품 상세 페이지 조회 시 호출됨
     * @param productId 상품 ID
     */
    @Transactional
    public void incrementViewCount(Long productId) {
        Product product = getProduct(productId);
        // 현재 조회수에 +1
        product.setViewCount(product.getViewCount() + 1);
        productRepository.save(product);
    }

    /**
     * 상품 삭제 (논리적 삭제)
     * 실제 데이터를 삭제하지 않고 isDeleted 플래그를 true로 변경
     * 데이터 복구 및 통계 목적으로 논리적 삭제 사용
     * @param product 삭제할 상품 엔티티
     */
    @Transactional
    public void delete(Product product) {
        product.setIsDeleted(true);  // 삭제 플래그 설정
        productRepository.save(product);
    }

    /**
     * 상품 판매 상태 변경
     * 판매중 → 예약중 → 판매완료 등의 상태 전환
     * @param productId 상품 ID
     * @param status 변경할 상태 (SALE, RESERVED, SOLD 등)
     */
    @Transactional
    public void changeStatus(Long productId, String status) {
        Product product = getProduct(productId);
        // 문자열을 ProductStatus Enum으로 변환하여 설정
        product.setStatus(ProductStatus.valueOf(status));
        productRepository.save(product);
    }

    /**
     * 상품 저장
     * 단순 저장 메서드 (생성/수정에 범용적으로 사용)
     * @param product 저장할 상품 엔티티
     */
    public void save(Product product) {
        productRepository.save(product);
    }

    /**
     * 새 상품 등록
     * 메인 이미지와 추가 이미지를 함께 저장
     * @param productForm 등록할 상품 정보
     * @param seller 판매자 정보
     * @return 저장된 Product 엔티티
     */
    @Transactional
    public Product create(ProductForm productForm, User seller) {
        // 카테고리 조회
        Category category = categoryService.getCategoryById(productForm.getCategoryId());

        // Product 엔티티 생성 (Builder 패턴)
        Product product = Product.builder()
                .seller(seller)  // 판매자
                .category(category)  // 카테고리
                .title(productForm.getTitle())  // 제목
                .price(productForm.getPrice())  // 가격
                .content(productForm.getContent())  // 상세 설명
                .mainImage(productForm.getMainImage())  // 메인 이미지 URL
                .bankName(productForm.getBankName())  // 은행명
                .accountNumber(productForm.getAccountNumber())  // 계좌번호
                .accountHolder(productForm.getAccountHolder())  // 예금주
                .viewCount(0)  // 조회수 초기값
                .isDeleted(false)  // 삭제 여부 초기값
                .status(ProductStatus.SALE)  // 초기 상태: 판매중
                .build();

        // 상품 저장
        Product savedProduct = productRepository.save(product);

        log.info("✅ 상품 등록 완료: {}", savedProduct.getTitle());

        // 추가 이미지가 있는 경우 처리
        if (productForm.getAdditionalImages() != null && !productForm.getAdditionalImages().isEmpty()) {
            List<ProductImage> productImages = new ArrayList<>();

            // 각 추가 이미지를 ProductImage 엔티티로 변환
            for (int i = 0; i < productForm.getAdditionalImages().size(); i++) {
                String imageUrl = productForm.getAdditionalImages().get(i);

                ProductImage productImage = ProductImage.builder()
                        .product(savedProduct)  // 상품과 연결
                        .imageUrl(imageUrl)  // 이미지 URL
                        .imageOrder(i + 1)  // 이미지 순서 (1부터 시작)
                        .build();

                productImages.add(productImage);
            }

            // 모든 추가 이미지를 한 번에 저장
            productImageRepository.saveAll(productImages);
            log.info("✅ 추가 이미지 {}개 저장 완료", productImages.size());
        }

        return savedProduct;
    }

    // ========== 기존 메서드 (ProductForm 반환) ==========

    /**
     * 메인 페이지용 - 전체 상품 목록 조회
     * 페이징 처리와 검색 기능 지원
     * 삭제되지 않은 상품만 최신순으로 조회
     * @param keyword 검색어 (제목 검색, null 가능)
     * @param pageable 페이징 정보 (페이지 번호, 크기, 정렬)
     * @return Page<ProductForm> 페이징된 상품 목록
     */
    public Page<ProductForm> getProductList(String keyword, Pageable pageable) {
        Page<Product> products;

        // 검색어가 있는 경우
        if (keyword != null && !keyword.trim().isEmpty()) {
            // 제목에 검색어가 포함되고, 삭제되지 않은 상품을 최신순으로 조회
            products = productRepository.findByTitleContainingAndIsDeletedFalseOrderByCreatedDateDesc(keyword, pageable);
            log.info("🔍 검색어: '{}' - {}개 검색됨", keyword, products.getTotalElements());
        } else {
            // 검색어가 없으면 전체 상품 조회 (삭제되지 않은 것만)
            products = productRepository.findByIsDeletedFalseOrderByCreatedDateDesc(pageable);
            log.info("📦 전체 상품 조회 - {}개", products.getTotalElements());
        }

        // Entity를 DTO로 변환하여 반환
        return products.map(ProductForm::fromEntity);
    }

    /**
     * 카테고리별 상품 조회
     * 특정 카테고리의 상품만 페이징 처리하여 조회
     * 검색 기능도 함께 지원
     * @param categoryId 카테고리 ID
     * @param keyword 검색어 (제목 검색, null 가능)
     * @param pageable 페이징 정보
     * @return Page<ProductForm> 페이징된 상품 목록
     */
    public Page<ProductForm> getProductsByCategory(Long categoryId, String keyword, Pageable pageable) {
        Page<Product> products;

        // 검색어가 있는 경우
        if (keyword != null && !keyword.trim().isEmpty()) {
            // 특정 카테고리 + 제목 검색 + 삭제되지 않은 상품
            products = productRepository.findByTitleContainingAndCategory_CategoryIdAndIsDeletedFalseOrderByCreatedDateDesc(
                    keyword, categoryId, pageable);
            log.info("🔍 카테고리 {} + 검색어 '{}' - {}개 검색됨", categoryId, keyword, products.getTotalElements());
        } else {
            // 특정 카테고리의 전체 상품 조회
            products = productRepository.findByCategory_CategoryIdAndIsDeletedFalseOrderByCreatedDateDesc(categoryId, pageable);
            log.info("📂 카테고리 {} - {}개", categoryId, products.getTotalElements());
        }

        // Entity를 DTO로 변환하여 반환
        return products.map(ProductForm::fromEntity);
    }

    // ========== 🔥 NEW: REST API용 DTO 변환 메서드 추가 ==========

    /**
     * 전체 상품 목록 조회 (ProductDto 반환 - REST API용)
     * 무한 재귀 문제 방지를 위해 DTO로 변환
     */
    @Transactional(readOnly = true)
    public Page<ProductDto> getProductListDto(String keyword, Pageable pageable) {
        Page<Product> products;

        if (keyword != null && !keyword.trim().isEmpty()) {
            products = productRepository.findByTitleContainingAndIsDeletedFalseOrderByCreatedDateDesc(keyword, pageable);
            log.info("🔍 검색어: '{}' - {}개 검색됨", keyword, products.getTotalElements());
        } else {
            products = productRepository.findByIsDeletedFalseOrderByCreatedDateDesc(pageable);
            log.info("📦 전체 상품 조회 - {}개", products.getTotalElements());
        }

        // Entity를 ProductDto로 변환
        return products.map(ProductDto::fromEntity);
    }

    /**
     * 카테고리별 상품 조회 (ProductDto 반환 - REST API용)
     * 무한 재귀 문제 방지를 위해 DTO로 변환
     */
    @Transactional(readOnly = true)
    public Page<ProductDto> getProductsByCategoryDto(Long categoryId, String keyword, Pageable pageable) {
        Page<Product> products;

        if (keyword != null && !keyword.trim().isEmpty()) {
            products = productRepository.findByTitleContainingAndCategory_CategoryIdAndIsDeletedFalseOrderByCreatedDateDesc(
                    keyword, categoryId, pageable);
            log.info("🔍 카테고리 {} + 검색어 '{}' - {}개 검색됨", categoryId, keyword, products.getTotalElements());
        } else {
            products = productRepository.findByCategory_CategoryIdAndIsDeletedFalseOrderByCreatedDateDesc(categoryId, pageable);
            log.info("📂 카테고리 {} - {}개", categoryId, products.getTotalElements());
        }

        // Entity를 ProductDto로 변환
        return products.map(ProductDto::fromEntity);
    }

    /**
     * 상품 상세 조회 (ProductDto 반환 - REST API용)
     * 찜 여부, 찜 개수, 댓글 개수 포함
     */
    @Transactional
    public ProductDto getProductDetailDto(Long productId, User currentUser) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));

        // 조회수 증가
        product.setViewCount(product.getViewCount() + 1);
        productRepository.save(product);

        // 기본 DTO 변환
        ProductDto dto = ProductDto.fromEntity(product);

        // 추가 정보 설정 (필요시 LikeService, CommentService 주입 필요)
        // dto.setIsLiked(likeService.isLiked(currentUser, product));
        // dto.setLikeCount(likeService.getLikeCount(product));
        // dto.setCommentCount(commentService.getCommentCount(product));

        log.info("✅ 상품 상세 조회: {} (조회수: {})", product.getTitle(), product.getViewCount());

        return dto;
    }
    /**

     * ✅ [추가] 특정 판매자가 등록한 모든 상품 (삭제되지 않은 것만)을 최신순으로 조회합니다.
     * MypageController에서 "판매 내역"에 판매 중인 상품을 포함하기 위해 사용됩니다.
     * @param seller 조회할 사용자 (판매자)
     * @return 등록된 모든 Product 목록
     */
    @Transactional(readOnly = true) // 조회 전용 트랜잭션
    public List<Product> getProductsBySeller(User seller) {
        // ProductRepository에 정의한 쿼리 메서드를 호출하여 등록 상품 목록을 가져옵니다.
        return productRepository.findBySellerAndIsDeletedFalseOrderByCreatedDateDesc(seller);
    }
        /**
     * 🔥 지역 + 카테고리 + 검색어 + 정렬 필터링
     * @param district 구 이름 (null 가능)
     * @param categoryId 카테고리 ID (null 가능)
     * @param keyword 검색어 (null 가능)
     * @param pageable 페이징 + 정렬 정보
     * @return 필터링된 상품 목록
     */
    @Transactional(readOnly = true)
    public Page<ProductDto> getProductsWithFilters(
            String district,
            Long categoryId,
            String keyword,
            Pageable pageable) {

        Page<Product> products;

        // 🔥 모든 필터 조합 처리
        if (district != null && categoryId != null && keyword != null && !keyword.trim().isEmpty()) {
            // 지역 + 카테고리 + 검색어
            products = productRepository.findByDistrictAndCategoryAndKeywordAndIsDeletedFalse(
                    district, categoryId, keyword, pageable);
            log.info("🔍 필터: 구={}, 카테고리={}, 검색어={} - {}개",
                    district, categoryId, keyword, products.getTotalElements());

        } else if (district != null && categoryId != null) {
            // 지역 + 카테고리
            products = productRepository.findByDistrictAndCategoryAndIsDeletedFalse(
                    district, categoryId, pageable);
            log.info("🔍 필터: 구={}, 카테고리={} - {}개",
                    district, categoryId, products.getTotalElements());

        } else if (district != null && keyword != null && !keyword.trim().isEmpty()) {
            // 지역 + 검색어
            products = productRepository.findByDistrictAndKeywordAndIsDeletedFalse(
                    district, keyword, pageable);
            log.info("🔍 필터: 구={}, 검색어={} - {}개",
                    district, keyword, products.getTotalElements());

        } else if (district != null) {
            // 지역만
            products = productRepository.findByDistrictAndIsDeletedFalse(district, pageable);
            log.info("🔍 필터: 구={} - {}개", district, products.getTotalElements());

        } else if (categoryId != null && keyword != null && !keyword.trim().isEmpty()) {
            // 카테고리 + 검색어 (기존 메서드 활용)
            products = productRepository.findByTitleContainingAndCategory_CategoryIdAndIsDeletedFalseOrderByCreatedDateDesc(
                    keyword, categoryId, pageable);
            log.info("🔍 필터: 카테고리={}, 검색어={} - {}개",
                    categoryId, keyword, products.getTotalElements());

        } else if (categoryId != null) {
            // 카테고리만 (기존 메서드 활용)
            products = productRepository.findByCategoryCategoryIdAndIsDeletedFalse(categoryId, pageable);
            log.info("🔍 필터: 카테고리={} - {}개", categoryId, products.getTotalElements());

        } else if (keyword != null && !keyword.trim().isEmpty()) {
            // 검색어만 (기존 메서드 활용)
            products = productRepository.findByTitleContainingAndIsDeletedFalse(keyword, pageable);
            log.info("🔍 필터: 검색어={} - {}개", keyword, products.getTotalElements());

        } else {
            // 필터 없음 - 전체 조회
            products = productRepository.findByIsDeletedFalseOrderByCreatedDateDesc(pageable);
            log.info("📦 전체 상품 조회 - {}개", products.getTotalElements());
        }

        return products.map(ProductDto::fromEntity);
    }

    /**
     * 🔥 지역(구) 목록 조회
     * @return 구 목록 (중복 제거)
     */
    public List<String> getDistinctDistricts() {
        List<String> districts = productRepository.findDistinctDistricts();
        log.info("📍 지역 목록 조회 - {}개 구 발견", districts.size());
        return districts;

    }
}
