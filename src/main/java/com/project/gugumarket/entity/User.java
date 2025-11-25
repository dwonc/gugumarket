package com.project.gugumarket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "USERS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USER_ID")
    private Long userId;

    @Column(name = "USER_NAME", length = 50, nullable = false, unique = true)
    private String userName;

    @JsonIgnore
    @Column(name = "PASSWORD", length = 255, nullable = false)
    private String password;

    @Column(name = "EMAIL", length = 100, nullable = false, unique = true)
    private String email;

    @Column(name = "NICKNAME", length = 30, nullable = false)
    private String nickname;

    @Column(name = "PHONE", length = 20)
    private String phone;

    @Column(name = "ADDRESS", length = 255, nullable = false)
    private String address;

    @Column(name = "ADDRESS_DETAIL", length = 100, nullable = false)
    private String addressDetail;

    @Column(name = "POSTAL_CODE", length = 10, nullable = false)
    private String postalCode;

    @Column(name = "PROFILE_IMAGE", length = 500)
    private String profileImage;

    @CreationTimestamp
    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "IS_ACTIVE")
    private Boolean isActive = true;

    @Column(name = "ROLE", length = 20)
    private String role = "USER";

    // 🆕🆕🆕 회원 등급 시스템 필드 추가 🆕🆕🆕
    @Enumerated(EnumType.STRING)
    @Column(name = "USER_LEVEL")
    @Builder.Default
    private UserLevel userLevel = UserLevel.EGG;

    @Column(name = "TRANSACTION_COUNT")
    @Builder.Default
    private Integer transactionCount = 0;

    @Column(name = "SELLER_RATING")
    private Double sellerRating;

    @Column(name = "BUYER_RATING")
    private Double buyerRating;
    // 🆕🆕🆕 회원 등급 필드 끝 🆕🆕🆕

    // 🔥🔥🔥 명시적 Getter 추가 (Lombok 보완) 🔥🔥🔥
    public UserLevel getUserLevel() {
        return userLevel != null ? userLevel : UserLevel.EGG;
    }

    public Integer getTransactionCount() {
        return transactionCount != null ? transactionCount : 0;
    }
    // 🔥🔥🔥 명시적 Getter 끝 🔥🔥🔥

    public String getProfileImageOrDefault() {
        if (profileImage == null || profileImage.isEmpty()) {
            return "/images/default-profile.png";
        }

        if (profileImage.startsWith("http://") || profileImage.startsWith("https://")) {
            return profileImage;
        }

        return profileImage;
    }

    // 🆕🆕🆕 회원 등급 관련 메서드 추가 🆕🆕🆕
    /**
     * 거래 완료 시 호출 - 거래 횟수 증가 및 등급 자동 업데이트
     */
    public void completeTransaction() {
        if (this.transactionCount == null) {
            this.transactionCount = 0;
        }
        this.transactionCount++;
        this.userLevel = UserLevel.fromTransactionCount(this.transactionCount);
    }

    /**
     * 등급 표시명 조회
     */
    public String getLevelDisplayName() {
        if (this.userLevel == null) {
            return UserLevel.EGG.getDisplayName();
        }
        return this.userLevel.getDisplayName();
    }

    /**
     * 등급 이모지 조회
     */
    public String getLevelEmoji() {
        if (this.userLevel == null) {
            return UserLevel.EGG.getEmoji();
        }
        return this.userLevel.getEmoji();
    }

    /**
     * 다음 등급까지 필요한 거래 횟수
     */
    public int getTransactionsToNextLevel() {
        if (this.userLevel == null) {
            return UserLevel.EGG.getTransactionsToNextLevel(0);
        }
        if (this.transactionCount == null) {
            return this.userLevel.getTransactionsToNextLevel(0);
        }
        return this.userLevel.getTransactionsToNextLevel(this.transactionCount);
    }
    // 🆕🆕🆕 회원 등급 메서드 끝 🆕🆕🆕

    // 연관관계 (기존 그대로 유지)
    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL)
    private List<Product> products = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Like> likes = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<QnaPost> qnaPosts = new ArrayList<>();

    @OneToMany(mappedBy = "receiver", cascade = CascadeType.ALL)
    private List<Notification> notifications = new ArrayList<>();

    // 🆕 거래 관계 추가 (Transaction 엔티티 사용 시)
    @OneToMany(mappedBy = "buyer", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Transaction> buyerTransactions = new ArrayList<>();

    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Transaction> sellerTransactions = new ArrayList<>();
}