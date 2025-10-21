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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

    // ← 추가
import org.springframework.data.domain.Pageable;  // ← 추가

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryService categoryService;
    private final FileService fileService;

    public Product getProduct(Long id) {
        Optional<Product> product = this.productRepository.findById(id);

        if(product.isPresent())
            return product.get();
        else
            throw new DataNotFoundException("Product not found");
    }

    @Transactional
    public void modify(Product product, ProductForm productDto) {
        // ✅ categoryId로 Category 조회
        Category category = categoryService.getCategoryById(productDto.getCategoryId());

        // 기본 정보 수정
        product.setCategory(category);
        product.setTitle(productDto.getTitle());
        product.setPrice(productDto.getPrice());
        product.setContent(productDto.getContent());
        product.setBankName(productDto.getBankName());
        product.setAccountNumber(productDto.getAccountNumber());
        product.setAccountHolder(productDto.getAccountHolder());

        // 메인 이미지 변경
        if (productDto.getMainImage() != null && !productDto.getMainImage().isEmpty()) {
            if (!productDto.getMainImage().equals(product.getMainImage())) {
                // 기존 이미지 삭제
                if (product.getMainImage() != null) {
                    try {
                        String oldFileName = product.getMainImage().substring(product.getMainImage().lastIndexOf("/") + 1);
                        fileService.deleteFile(oldFileName);
                        System.out.println("✅ 기존 이미지 삭제 완료: " + oldFileName);
                    } catch (IOException e) {
                        System.err.println("⚠️ 기존 이미지 삭제 실패: " + e.getMessage());
                        // 이미지 삭제 실패해도 계속 진행
                    }
                }
                product.setMainImage(productDto.getMainImage());
            }
        }

        productRepository.save(product);
    }

    // ✅ 조회수 증가
    @Transactional
    public void incrementViewCount(Long productId) {
        Product product = getProduct(productId);
        product.setViewCount(product.getViewCount() + 1);
        productRepository.save(product);
    }

    // ✅ 삭제 (soft delete)
    @Transactional
    public void delete(Product product) {
        // Soft delete
        product.setIsDeleted(true);
        productRepository.save(product);

        // 🔥 이미지 파일도 삭제하려면 아래 주석 해제
        /*
        try {
            // 메인 이미지 삭제
            if (product.getMainImage() != null) {
                String fileName = product.getMainImage().substring(product.getMainImage().lastIndexOf("/") + 1);
                fileService.deleteFile(fileName);
            }

            // 추가 이미지 삭제
            if (product.getImages() != null) {
                for (ProductImage image : product.getImages()) {
                    String fileName = image.getImageUrl().substring(image.getImageUrl().lastIndexOf("/") + 1);
                    fileService.deleteFile(fileName);
                }
            }
        } catch (IOException e) {
            System.err.println("⚠️ 이미지 삭제 중 오류: " + e.getMessage());
        }
        */
    }

    // ✅ 상태 변경
    @Transactional
    public void changeStatus(Long productId, String status) {
        Product product = getProduct(productId);
        product.setStatus(ProductStatus.valueOf(status));
        productRepository.save(product);
    }

    public void save(Product product) {
        productRepository.save(product);
    }

    /**
     * 상품 등록
     */
    @Transactional
    public Product create(ProductForm productForm, User seller) {
        // 카테고리 조회
        Category category = categoryService.getCategoryById(productForm.getCategoryId());

        // Product 엔티티 생성
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
                .viewCount(0)
                .isDeleted(false)
                .status(ProductStatus.SALE)  // 기본 상태: 판매중
                .build();

        // 상품 저장
        Product savedProduct = productRepository.save(product);

        System.out.println("✅ 상품 등록 완료: " + savedProduct.getTitle());

        // 추가 이미지가 있다면 저장
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
            System.out.println("✅ 추가 이미지 " + productImages.size() + "개 저장 완료");
        }

        return savedProduct;
    }
    public Page<ProductForm> getProductList(Pageable pageable) {
        Page<Product> products = productRepository.findByIsDeletedFalseOrderByCreatedDateDesc(pageable);
        return products.map(ProductForm::fromEntity);
    }

    /**
     * 카테고리별 상품 조회 (페이징)
     * 특정 카테고리의 삭제되지 않은 상품을 최신순으로 조회
     */
    public Page<ProductForm> getProductsByCategory(Long categoryId, Pageable pageable) {
        Page<Product> products = productRepository.findByCategory_CategoryIdAndIsDeletedFalseOrderByCreatedDateDesc(categoryId, pageable);
        return products.map(ProductForm::fromEntity);
    }
}
