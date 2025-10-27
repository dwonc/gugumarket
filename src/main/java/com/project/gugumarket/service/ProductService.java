package com.project.gugumarket.service;

import com.project.gugumarket.DataNotFoundException;
import com.project.gugumarket.ProductStatus;
import com.project.gugumarket.dto.ProductForm;
import com.project.gugumarket.entity.Category;
import com.project.gugumarket.entity.Product;
import com.project.gugumarket.entity.ProductImage;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.ProductImageRepository;
import com.project.gugumarket.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
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

/**
 * 상품 관련 비즈니스 로직을 처리하는 서비스
 * 상품 등록, 조회, 수정, 삭제, 상태 변경 등의 핵심 기능을 담당
 */
@Service
@RequiredArgsConstructor
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

<<<<<<< HEAD
    /**
     * 상품 정보 수정
     * 카테고리, 제목, 가격, 내용, 계좌정보, 메인 이미지를 업데이트
     * 메인 이미지 변경 시 기존 이미지 파일 삭제 처리
     * @param product 수정할 상품 엔티티
     * @param productDto 수정할 정보가 담긴 DTO
     */
    @Transactional  // 트랜잭션 처리 - 오류 시 롤백
    public void modify(Product product, ProductForm productDto) {
        // 카테고리 조회 및 설정
        Category category = categoryService.getCategoryById(productDto.getCategoryId());

        // 기본 정보 업데이트
        product.setCategory(category);
=======
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
>>>>>>> 28cebc40083f14c3d32f93518519a56ce9ec8b8a
        product.setTitle(productDto.getTitle());
        product.setPrice(productDto.getPrice());
        product.setContent(productDto.getContent());
        product.setBankName(productDto.getBankName());
        product.setAccountNumber(productDto.getAccountNumber());
        product.setAccountHolder(productDto.getAccountHolder());

<<<<<<< HEAD
        // 메인 이미지 변경 처리
=======
        Category category = categoryService.getCategoryById(productDto.getCategoryId());
        product.setCategory(category);

>>>>>>> 28cebc40083f14c3d32f93518519a56ce9ec8b8a
        if (productDto.getMainImage() != null && !productDto.getMainImage().isEmpty()) {
            // 새 이미지가 기존 이미지와 다른 경우
            if (!productDto.getMainImage().equals(product.getMainImage())) {
                // 기존 이미지가 있으면 파일 삭제
                if (product.getMainImage() != null) {
                    try {
<<<<<<< HEAD
                        // URL에서 파일명 추출
                        String oldFileName = product.getMainImage().substring(product.getMainImage().lastIndexOf("/") + 1);
=======
                        String oldFileName = product.getMainImage().substring(
                                product.getMainImage().lastIndexOf("/") + 1);
>>>>>>> 28cebc40083f14c3d32f93518519a56ce9ec8b8a
                        fileService.deleteFile(oldFileName);
                    } catch (IOException e) {
                        System.err.println("⚠️ 기존 이미지 삭제 실패: " + e.getMessage());
                    }
                }
                // 새 이미지 URL 설정
                product.setMainImage(productDto.getMainImage());
            }
        }

<<<<<<< HEAD
        // 변경사항 저장
        productRepository.save(product);
=======
        // ✅ save() 호출 제거! Dirty Checking으로 자동 업데이트
>>>>>>> 28cebc40083f14c3d32f93518519a56ce9ec8b8a
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

        System.out.println("✅ 상품 등록 완료: " + savedProduct.getTitle());

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
            System.out.println("✅ 추가 이미지 " + productImages.size() + "개 저장 완료");
        }

        return savedProduct;
    }

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
            System.out.println("🔍 검색어: '" + keyword + "' - " + products.getTotalElements() + "개 검색됨");
        } else {
            // 검색어가 없으면 전체 상품 조회 (삭제되지 않은 것만)
            products = productRepository.findByIsDeletedFalseOrderByCreatedDateDesc(pageable);
            System.out.println("📦 전체 상품 조회 - " + products.getTotalElements() + "개");
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
            System.out.println("🔍 카테고리 " + categoryId + " + 검색어 '" + keyword + "' - " + products.getTotalElements() + "개 검색됨");
        } else {
            // 특정 카테고리의 전체 상품 조회
            products = productRepository.findByCategory_CategoryIdAndIsDeletedFalseOrderByCreatedDateDesc(categoryId, pageable);
            System.out.println("📂 카테고리 " + categoryId + " - " + products.getTotalElements() + "개");
        }

        // Entity를 DTO로 변환하여 반환
        return products.map(ProductForm::fromEntity);
    }
}