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
    /**
     * 사용자 등급을 반환하며, null인 경우 기본값(EGG) 반환
     * Lombok의 @Getter를 보완하여 null 안정성 보장
     * @return 사용자 등급 (null이면 EGG 등급)
     */
    public UserLevel getUserLevel() {
        return userLevel != null ? userLevel : UserLevel.EGG;
    }
    /**
     * 거래 횟수를 반환하며, null인 경우 0 반환
     * Lombok의 @Getter를 보완하여 null 안정성 보장
     * @return 거래 횟수 (null이면 0)
     */
    public Integer getTransactionCount() {
        return transactionCount != null ? transactionCount : 0;
    }

    /**
     * 프로필 이미지 URL을 반환하며, 없는 경우 기본 이미지 경로 반환
     * 프론트엔드에서 항상 유효한 이미지 경로를 받을 수 있도록 보장
     * @return 프로필 이미지 URL 또는 기본 이미지 경로
     */
    public String getProfileImageOrDefault() {
        // 프로필 이미지가 없거나 빈 문자열인 경우 기본 이미지 반환
        if (profileImage == null || profileImage.isEmpty()) {
            return "/images/default-profile.png";
        }
        // 이미 완전한 URL인 경우 그대로 반환 (외부 저장소 이미지)
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
        // 증가된 거래 횟수에 따라 등급 자동 업데이트
        this.userLevel = UserLevel.fromTransactionCount(this.transactionCount);
    }

    /**
     * 현재 사용자 등급의 표시명을 조회
     * 화면에 보여줄 등급 이름 반환
     * @return 등급 표시명
     */
    public String getLevelDisplayName() {
        // 등급이 null인 경우 기본 등급(EGG)의 표시명 반환
        if (this.userLevel == null) {
            return UserLevel.EGG.getDisplayName();
        }
        return this.userLevel.getDisplayName();
    }

    /**
     * 등급 이모지 조회
     * * UI에 표시할 시각적 요소 제공
     */
    public String getLevelEmoji() {
        if (this.userLevel == null) {
            return UserLevel.EGG.getEmoji();
        }
        return this.userLevel.getEmoji();
    }

    /**
     * 다음 등급까지 필요한 거래 횟수
     * * 사용자에게 "앞으로 N번 거래하면 등급 업!" 같은 정보 제공
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
    /* mappedBy = "seller": Product 엔티티의 seller 필드가 관계의 주인
     * cascade = CascadeType.ALL: 사용자 삭제 시 등록한 모든 상품도 함께 삭제
     */
    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL)
    private List<Product> products = new ArrayList<>();
    // 사용자의 찜한 목록 삭제
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Like> likes = new ArrayList<>();
    //사용자의 댓글 목록
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Comment> comments = new ArrayList<>();
    //qna 목록
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<QnaPost> qnaPosts = new ArrayList<>();
    //알림 목록
    @OneToMany(mappedBy = "receiver", cascade = CascadeType.ALL)
    private List<Notification> notifications = new ArrayList<>();

    // 🆕 거래 관계 추가 (Transaction 엔티티 사용 시)
    //mappedBy=양방향 연관관계 설정 연관관계의 주인이 아닌(Owner) 쪽 엔티티에 명시되어, 주인이 누구인지를 JPA에게 알려주는 역할
    /**
     * 사용자가 구매자로 참여한 거래 목록
     * mappedBy = "buyer": Transaction 엔티티의 buyer 필드가 관계의 주인
     * @Builder.Default: 빌더 패턴 사용 시에도 빈 리스트로 초기화하여 NullPointerException 방지
     */
    @OneToMany(mappedBy = "buyer", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Transaction> buyerTransactions = new ArrayList<>();

    /**
     * 사용자가 판매자로 참여한 거래 목록
     * mappedBy = "seller": Transaction 엔티티의 seller 필드가 관계의 주인
     * @Builder.Default: 빌더 패턴 사용 시에도 빈 리스트로 초기화하여 NullPointerException 방지
     */
    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Transaction> sellerTransactions = new ArrayList<>();
}