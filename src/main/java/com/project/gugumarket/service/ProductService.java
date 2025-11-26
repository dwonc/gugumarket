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
import java.util.Map;
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

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryService categoryService;
    private final FileService fileService;
    private final KakaoMapService kakaoMapService;  // 🗺️ 추가

    /**
     * 상품 ID로 상품 조회
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
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        if (!product.getSeller().equals(currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "수정권한이 없습니다.");
        }

        product.setTitle(productDto.getTitle());
        product.setPrice(productDto.getPrice());
        product.setContent(productDto.getContent());
        product.setBankName(productDto.getBankName());
        product.setAccountNumber(productDto.getAccountNumber());
        product.setAccountHolder(productDto.getAccountHolder());

        Category category = categoryService.getCategoryById(productDto.getCategoryId());
        product.setCategory(category);

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

        if (productDto.getAdditionalImages() != null) {
            log.info("🔄 추가 이미지 업데이트 시작");
            log.info("📥 새로운 이미지 개수: {}", productDto.getAdditionalImages().size());

            List<ProductImage> existingImages = productImageRepository.findByProduct(product);
            log.info("📦 기존 이미지 개수: {}", existingImages.size());

            List<String> existingUrls = existingImages.stream()
                    .map(ProductImage::getImageUrl)
                    .collect(Collectors.toList());

            List<String> newUrls = productDto.getAdditionalImages();

            List<String> urlsToDelete = existingUrls.stream()
                    .filter(url -> !newUrls.contains(url))
                    .collect(Collectors.toList());

            log.info("🗑️ 삭제할 이미지: {}", urlsToDelete.size());

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

            if (!existingImages.isEmpty()) {
                productImageRepository.deleteAll(existingImages);
                log.info("✅ DB에서 기존 이미지 {}개 삭제 완료", existingImages.size());
            }

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

        productRepository.save(product);
        log.info("✅ 상품 수정 완료: {}", product.getTitle());
    }

    /**
     * 상품 조회수 증가
     */
    @Transactional
    public void incrementViewCount(Long productId) {
        Product product = getProduct(productId);
        product.setViewCount(product.getViewCount() + 1);
        productRepository.save(product);
    }

    /**
     * 상품 삭제 (논리적 삭제)
     */
    @Transactional
    public void delete(Product product) {
        product.setIsDeleted(true);
        productRepository.save(product);
    }

    /**
     * 상품 판매 상태 변경
     */
    @Transactional
    public void changeStatus(Long productId, String status) {
        Product product = getProduct(productId);
        product.setStatus(ProductStatus.valueOf(status));
        productRepository.save(product);
    }

    /**
     * 상품 저장
     */
    public void save(Product product) {
        productRepository.save(product);
    }

    /**
     * 새 상품 등록
     */
    @Transactional
    public Product create(ProductForm productForm, User seller) {
        Category category = categoryService.getCategoryById(productForm.getCategoryId());

        // 🗺️ 판매자 주소로 좌표 얻기
        Double latitude = null;
        Double longitude = null;

        if (seller.getAddress() != null) {
            Map<String, Double> coordinates = kakaoMapService.getCoordinatesFromAddress(seller.getAddress());
            if (coordinates != null) {
                latitude = coordinates.get("latitude");
                longitude = coordinates.get("longitude");
                log.info("🗺️ 상품 등록 시 좌표 설정: ({}, {})", latitude, longitude);
            }
        }

        Product product = Product.builder()
                .seller(seller)
                .category(category)
                .title(productForm.getTitle())
                .price(productForm.getPrice())
                .content(productForm.getContent())
                .mainImage(productForm.getMainImage())
                .bankName(productForm.getBankName())
                .accountNumber(productForm.getAccountNumber())
                .accountHolder(productForm.getAccountHolder())
                .latitude(latitude)  // 🗺️ 추가
                .longitude(longitude)  // 🗺️ 추가
                .viewCount(0)
                .isDeleted(false)
                .status(ProductStatus.SALE)
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("✅ 상품 등록 완료: {}", savedProduct.getTitle());

        if (productForm.getAdditionalImages() != null && !productForm.getAdditionalImages().isEmpty()) {
            List<ProductImage> productImages = new ArrayList<>();

            for (int i = 0; i < productForm.getAdditionalImages().size(); i++) {
                String imageUrl = productForm.getAdditionalImages().get(i);

                ProductImage productImage = ProductImage.builder()
                        .product(savedProduct)
                        .imageUrl(imageUrl)
                        .imageOrder(i + 1)
                        .build();

                productImages.add(productImage);
            }

            productImageRepository.saveAll(productImages);
            log.info("✅ 추가 이미지 {}개 저장 완료", productImages.size());
        }

        return savedProduct;
    }

    /**
     * 메인 페이지용 - 전체 상품 목록 조회
     */
    public Page<ProductForm> getProductList(String keyword, Pageable pageable) {
        Page<Product> products;

        if (keyword != null && !keyword.trim().isEmpty()) {
            products = productRepository.findByTitleContainingAndIsDeletedFalse(keyword, pageable);
            log.info("🔍 검색어: '{}' - {}개 검색됨", keyword, products.getTotalElements());
        } else {
            products = productRepository.findByIsDeletedFalse(pageable);
            log.info("📦 전체 상품 조회 - {}개", products.getTotalElements());
        }

        return products.map(ProductForm::fromEntity);
    }

    /**
     * 카테고리별 상품 조회
     */
    public Page<ProductForm> getProductsByCategory(Long categoryId, String keyword, Pageable pageable) {
        Page<Product> products;

        if (keyword != null && !keyword.trim().isEmpty()) {
            products = productRepository.findByTitleContainingAndCategory_CategoryIdAndIsDeletedFalse(
                    keyword, categoryId, pageable);
            log.info("🔍 카테고리 {} + 검색어 '{}' - {}개 검색됨", categoryId, keyword, products.getTotalElements());
        } else {
            products = productRepository.findByCategory_CategoryIdAndIsDeletedFalse(categoryId, pageable);
            log.info("📂 카테고리 {} - {}개", categoryId, products.getTotalElements());
        }

        return products.map(ProductForm::fromEntity);
    }

    /**
     * 전체 상품 목록 조회 (ProductDto 반환 - REST API용)
     */
    @Transactional(readOnly = true)
    public Page<ProductDto> getProductListDto(String keyword, Pageable pageable) {
        Page<Product> products;

        if (keyword != null && !keyword.trim().isEmpty()) {
            products = productRepository.findByTitleContainingAndIsDeletedFalse(keyword, pageable);
            log.info("🔍 검색어: '{}' - {}개 검색됨", keyword, products.getTotalElements());
        } else if(keyword == null){
            products = productRepository.findByIsDeletedFalse(pageable);
            log.info("📦 전체 상품 조회 - {}개", products.getTotalElements());
        } else {
            products = productRepository.findByIsDeletedFalse(pageable);
            log.info("📦 전체 상품 조회 - {}개", products.getTotalElements());
        }


        return products.map(ProductDto::fromEntity);
    }

    /**
     * 카테고리별 상품 조회 (ProductDto 반환 - REST API용)
     */
    @Transactional(readOnly = true)
    public Page<ProductDto> getProductsByCategoryDto(Long categoryId, String keyword, Pageable pageable) {
        Page<Product> products;

        if (keyword != null && !keyword.trim().isEmpty()) {
            products = productRepository.findByTitleContainingAndCategory_CategoryIdAndIsDeletedFalse(
                    keyword, categoryId, pageable);
            log.info("🔍 카테고리 {} + 검색어 '{}' - {}개 검색됨", categoryId, keyword, products.getTotalElements());
        } else {
            products = productRepository.findByCategory_CategoryIdAndIsDeletedFalse(categoryId, pageable);
            log.info("📂 카테고리 {} - {}개", categoryId, products.getTotalElements());
        }

        return products.map(ProductDto::fromEntity);
    }

    /**
     * 상품 상세 조회 (ProductDto 반환 - REST API용)
     */
    @Transactional
    public ProductDto getProductDetailDto(Long productId, User currentUser) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));

        product.setViewCount(product.getViewCount() + 1);
        productRepository.save(product);

        ProductDto dto = ProductDto.fromEntity(product);

        log.info("✅ 상품 상세 조회: {} (조회수: {})", product.getTitle(), product.getViewCount());

        return dto;
    }

    /**
     * 특정 판매자가 등록한 모든 상품 조회
     */
    @Transactional(readOnly = true)
    public List<Product> getProductsBySeller(User seller) {
        return productRepository.findBySellerAndIsDeletedFalseOrderByCreatedDateDesc(seller);
    }

    /**
     * 🔥 지역 + 카테고리 + 검색어 + 정렬 필터링
     */
    @Transactional(readOnly = true)
    public Page<ProductDto> getProductsWithFilters(
            String district,
            Long categoryId,
            String keyword,
            Pageable pageable) {

        Page<Product> products;

        log.info("🔍 정렬 정보: {}", pageable.getSort());

        if (district != null && categoryId != null && keyword != null && !keyword.trim().isEmpty()) {
            products = productRepository.findByDistrictAndCategoryAndKeywordAndIsDeletedFalse(
                    district, categoryId, keyword, pageable);
            log.info("🔍 필터: 구={}, 카테고리={}, 검색어={} - {}개",
                    district, categoryId, keyword, products.getTotalElements());

        } else if (district != null && categoryId != null) {
            products = productRepository.findByDistrictAndCategoryAndIsDeletedFalse(
                    district, categoryId, pageable);
            log.info("🔍 필터: 구={}, 카테고리={} - {}개",
                    district, categoryId, products.getTotalElements());

        } else if (district != null && keyword != null && !keyword.trim().isEmpty()) {
            products = productRepository.findByDistrictAndKeywordAndIsDeletedFalse(
                    district, keyword, pageable);
            log.info("🔍 필터: 구={}, 검색어={} - {}개",
                    district, keyword, products.getTotalElements());

        } else if (district != null) {
            products = productRepository.findByDistrictAndIsDeletedFalse(district, pageable);
            log.info("🔍 필터: 구={} - {}개", district, products.getTotalElements());

        } else if (categoryId != null && keyword != null && !keyword.trim().isEmpty()) {
            products = productRepository.findByTitleContainingAndCategory_CategoryIdAndIsDeletedFalse(
                    keyword, categoryId, pageable);
            log.info("🔍 필터: 카테고리={}, 검색어={} - {}개",
                    categoryId, keyword, products.getTotalElements());

        } else if (categoryId != null) {
            products = productRepository.findByCategory_CategoryIdAndIsDeletedFalse(categoryId, pageable);
            log.info("🔍 필터: 카테고리={} - {}개", categoryId, products.getTotalElements());

        } else if (keyword != null && !keyword.trim().isEmpty()) {
            products = productRepository.findByTitleContainingAndIsDeletedFalse(keyword, pageable);
            log.info("🔍 필터: 검색어={} - {}개", keyword, products.getTotalElements());

        } else if (keyword == null){
            products = productRepository.findByIsDeletedFalse(pageable);
            log.info("📦 전체 상품 조회 - {}개", products.getTotalElements());
        } else {
            products = productRepository.findByIsDeletedFalse(pageable);
            log.info("📦 전체 상품 조회 - {}개", products.getTotalElements());
        }

        return products.map(ProductDto::fromEntity);
    }

    /**
     * 🔥 지역(구) 목록 조회
     */
    public List<String> getDistinctDistricts() {
        List<String> districts = productRepository.findDistinctDistricts();
        log.info("📍 지역 목록 조회 - {}개 구 발견", districts.size());
        return districts;
    }

    // ========== 🗺️ 지도 기능 관련 메서드 ==========

    /**
     * 지도에 표시할 모든 상품 조회 (좌표가 있는 상품만)
     */
    @Transactional(readOnly = true)
    public List<ProductDto> getProductsForMap() {
        List<Product> products = productRepository.findAllWithCoordinates();
        log.info("🗺️ 지도용 상품 조회: {}개", products.size());
        return products.stream()
                .map(ProductDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 특정 범위 내의 상품 조회 (지도 영역 기준)
     */
    @Transactional(readOnly = true)
    public List<ProductDto> getProductsInBounds(Double minLat, Double maxLat, Double minLng, Double maxLng) {
        List<Product> products = productRepository.findProductsInBounds(minLat, maxLat, minLng, maxLng);
        log.info("🗺️ 범위 내 상품 조회: {}개", products.size());
        return products.stream()
                .map(ProductDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 기존 상품들의 좌표 일괄 업데이트 (최초 1회 실행용)
     */
    @Transactional
    public void updateProductCoordinates() {
        List<Product> products = productRepository.findProductsWithoutCoordinates();
        log.info("🗺️ 좌표 업데이트 대상: {}개", products.size());

        int successCount = 0;
        int failCount = 0;

        for (Product product : products) {
            if (product.getSeller() != null && product.getSeller().getAddress() != null) {
                String address = product.getSeller().getAddress();
                Map<String, Double> coordinates = kakaoMapService.getCoordinatesFromAddress(address);

                if (coordinates != null) {
                    product.updateCoordinates(
                            coordinates.get("latitude"),
                            coordinates.get("longitude")
                    );
                    productRepository.save(product);
                    successCount++;
                    log.info("✅ 상품 #{} 좌표 업데이트 완료", product.getProductId());
                } else {
                    failCount++;
                    log.warn("⚠️ 상품 #{} 좌표 변환 실패: {}", product.getProductId(), address);
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        log.info("🗺️ 좌표 업데이트 완료 - 성공: {}개, 실패: {}개", successCount, failCount);
    }

    // service/ProductService.java 에 추가

    /**
     * 🔥 지도용 상품 조회 (가격 필터 포함)
     */
    @Transactional(readOnly = true)
    public List<ProductDto> getProductsForMapWithPrice(Integer maxPrice) {
        List<Product> products;

        if (maxPrice != null && maxPrice > 0) {
            products = productRepository.findAllWithCoordinatesAndMaxPrice(maxPrice);
            log.info("🗺️ 지도용 상품 조회 ({}원 이하): {}개", maxPrice, products.size());
        } else {
            products = productRepository.findAllWithCoordinates();
            log.info("🗺️ 지도용 상품 조회 (전체): {}개", products.size());
        }

        return products.stream()
                .map(ProductDto::fromEntity)
                .collect(Collectors.toList());
    }
}