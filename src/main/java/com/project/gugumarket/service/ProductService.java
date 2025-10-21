package com.project.gugumarket.service;

import com.project.gugumarket.DataNotFoundException;
import com.project.gugumarket.ProductStatus;
import com.project.gugumarket.dto.ProductForm;
import com.project.gugumarket.entity.Category;
import com.project.gugumarket.entity.Product;
import com.project.gugumarket.entity.ProductImage;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.CategoryRepository;
import com.project.gugumarket.repository.ProductImageRepository;
import com.project.gugumarket.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryService categoryService;
    private final FileService fileService;


    // 파일 저장 경로 설정 (application.properties에서 관리하는 것을 권장)
    @Value("${file.upload-dir:uploads/products}")
    private String uploadDir;

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
                    String oldFileName = product.getMainImage().substring(product.getMainImage().lastIndexOf("/") + 1);
                    fileService.deleteFile(oldFileName);
                }
                product.setMainImage(productDto.getMainImage());
            }
        }

        productRepository.save(product);
    }

    // 파일 저장 메서드
    private String saveFile(MultipartFile file) throws IOException {
        // 업로드 디렉토리가 없으면 생성
        File uploadPath = new File(uploadDir);
        if (!uploadPath.exists()) {
            uploadPath.mkdirs();
        }

        // 원본 파일명
        String originalFilename = file.getOriginalFilename();

        // 파일명 중복 방지를 위해 UUID 사용
        String uuid = UUID.randomUUID().toString();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String savedFileName = uuid + extension;

        // 파일 저장 경로
        String filePath = uploadDir + File.separator + savedFileName;

        // 파일 저장
        File dest = new File(filePath);
        file.transferTo(dest);

        return savedFileName; // DB에는 파일명만 저장
    }

    // 기존 이미지 삭제 메서드
    private void deleteOldImage(String fileName) {
        if (fileName != null && !fileName.isEmpty()) {
            try {
                File file = new File(uploadDir + File.separator + fileName);
                if (file.exists()) {
                    file.delete();
                }
            } catch (Exception e) {
                // 로그만 남기고 계속 진행
                System.err.println("이미지 삭제 실패: " + e.getMessage());
            }
        }
    }

    // ✅ 조회수 증가
    @Transactional
    public void incrementViewCount(Long productId) {
        Product product = getProduct(productId);
        product.setViewCount(product.getViewCount() + 1);
        productRepository.save(product);
    }

    // ✅ 삭제 (soft delete 권장)
    @Transactional
    public void delete(Product product) {
        // Hard delete 대신 soft delete 권장
        product.setIsDeleted(true);
        productRepository.save(product);

        // 또는 Hard delete
        // productRepository.delete(product);
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
        }

        return savedProduct;
    }
}
