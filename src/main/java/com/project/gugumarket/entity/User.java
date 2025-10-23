package com.project.gugumarket.entity;


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

//    public String getProfileImageOrDefault() {
//        return (profileImage == null || profileImage.isEmpty())
//                ? "/images/default-profile.png"
//                : profileImage;
//    }
        public String getProfileImageOrDefault() {
        if (profileImage != null && !profileImage.isEmpty()) {
            return profileImage;
        }
        return "/images/default-profile.png";
    }


    // 연관관계
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
}