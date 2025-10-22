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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Category category = categoryService.getCategoryById(productDto.getCategoryId());

        product.setCategory(category);
        product.setTitle(productDto.getTitle());
        product.setPrice(productDto.getPrice());
        product.setContent(productDto.getContent());
        product.setBankName(productDto.getBankName());
        product.setAccountNumber(productDto.getAccountNumber());
        product.setAccountHolder(productDto.getAccountHolder());

        if (productDto.getMainImage() != null && !productDto.getMainImage().isEmpty()) {
            if (!productDto.getMainImage().equals(product.getMainImage())) {
                if (product.getMainImage() != null) {
                    try {
                        String oldFileName = product.getMainImage().substring(product.getMainImage().lastIndexOf("/") + 1);
                        fileService.deleteFile(oldFileName);
                        System.out.println("✅ 기존 이미지 삭제 완료: " + oldFileName);
                    } catch (IOException e) {
                        System.err.println("⚠️ 기존 이미지 삭제 실패: " + e.getMessage());
                    }
                }
                product.setMainImage(productDto.getMainImage());
            }
        }

        productRepository.save(product);
    }

    @Transactional
    public void incrementViewCount(Long productId) {
        Product product = getProduct(productId);
        product.setViewCount(product.getViewCount() + 1);
        productRepository.save(product);
    }

    @Transactional
    public void delete(Product product) {
        product.setIsDeleted(true);
        productRepository.save(product);
    }

    @Transactional
    public void changeStatus(Long productId, String status) {
        Product product = getProduct(productId);
        product.setStatus(ProductStatus.valueOf(status));
        productRepository.save(product);
    }

    public void save(Product product) {
        productRepository.save(product);
    }

    @Transactional
    public Product create(ProductForm productForm, User seller) {
        Category category = categoryService.getCategoryById(productForm.getCategoryId());

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
                .status(ProductStatus.SALE)
                .build();

        Product savedProduct = productRepository.save(product);

        System.out.println("✅ 상품 등록 완료: " + savedProduct.getTitle());

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

    /**
     * 메인 페이지용 - 전체 상품 목록 조회 (페이징 + 검색)
     */
    public Page<ProductForm> getProductList(String keyword, Pageable pageable) {
        Page<Product> products;

        if (keyword != null && !keyword.trim().isEmpty()) {
            products = productRepository.findByTitleContainingAndIsDeletedFalseOrderByCreatedDateDesc(keyword, pageable);
            System.out.println("🔍 검색어: '" + keyword + "' - " + products.getTotalElements() + "개 검색됨");
        } else {
            products = productRepository.findByIsDeletedFalseOrderByCreatedDateDesc(pageable);
            System.out.println("📦 전체 상품 조회 - " + products.getTotalElements() + "개");
        }

        return products.map(ProductForm::fromEntity);
    }

    /**
     * 카테고리별 상품 조회 (페이징 + 검색)
     */
    public Page<ProductForm> getProductsByCategory(Long categoryId, String keyword, Pageable pageable) {
        Page<Product> products;

        if (keyword != null && !keyword.trim().isEmpty()) {
            products = productRepository.findByTitleContainingAndCategory_CategoryIdAndIsDeletedFalseOrderByCreatedDateDesc(
                    keyword, categoryId, pageable);
            System.out.println("🔍 카테고리 " + categoryId + " + 검색어 '" + keyword + "' - " + products.getTotalElements() + "개 검색됨");
        } else {
            products = productRepository.findByCategory_CategoryIdAndIsDeletedFalseOrderByCreatedDateDesc(categoryId, pageable);
            System.out.println("📂 카테고리 " + categoryId + " - " + products.getTotalElements() + "개");
        }

        return products.map(ProductForm::fromEntity);
    }
}