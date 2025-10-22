package com.project.gugumarket.entity;

import com.project.gugumarket.ProductStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "PRODUCTS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PRODUCT_ID")
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SELLER_ID", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CATEGORY_ID")
    private Category category;

    @Column(name = "TITLE", length = 100, nullable = false)
    private String title;

    @Column(name = "CONTENT", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "PRICE", nullable = false)
    private Integer price;

    @Column(name = "MAIN_IMAGE", length = 255)
    private String mainImage;

    @Column(name = "VIEW_COUNT")
    private Integer viewCount = 0;

    @CreationTimestamp
    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "UPDATED_DATE")
    private LocalDateTime updatedDate;

    @Column(name = "IS_DELETED")
    private Boolean isDeleted = false;

    // 무통장입금 계좌 정보
    @Column(name = "BANK_NAME", length = 50)
    private String bankName;

    @Column(name = "ACCOUNT_NUMBER", length = 50)
    private String accountNumber;

    @Column(name = "ACCOUNT_HOLDER", length = 50)
    private String accountHolder;

    // 판매 상태

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 20)
    private ProductStatus status = ProductStatus.SALE; // 기본값: 판매중

    // 연관관계
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> productImages = new ArrayList<>();
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Like> likes = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<Transaction> transactions = new ArrayList<>();

    // 🔥 상태 변경 메서드 추가
    public void updateStatus(ProductStatus status) {
        this.status = status;
    }

    // 🔥 판매중으로 변경
    public void markAsSale() {
        this.status = ProductStatus.SALE;
    }

    // 🔥 예약중으로 변경
    public void markAsReserved() {
        this.status = ProductStatus.RESERVED;
    }

    // 🔥 판매완료로 변경
    public void markAsSoldOut() {
        this.status = ProductStatus.SOLD_OUT;
    }

    // 🔥 조회수 증가
    public void increaseViewCount() {
        this.viewCount = (this.viewCount == null ? 0 : this.viewCount) + 1;
    }

    // 🔥 상품 정보 수정
    public void update(String title, Integer price, String content, Category category, String mainImage) {
        this.title = title;
        this.price = price;
        this.content = content;
        this.category = category;
        this.mainImage = mainImage;
    }

    @PrePersist
    protected void onCreate() {
        this.createdDate = LocalDateTime.now();
        if (this.status == null) {
            this.status = ProductStatus.SALE;
        }
        if (this.viewCount == null) {
            this.viewCount = 0;
        }
    }
}