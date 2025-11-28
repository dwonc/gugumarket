package com.project.gugumarket.controller;

import com.project.gugumarket.dto.*;
import com.project.gugumarket.entity.*;
import com.project.gugumarket.repository.NotificationRepository;
import com.project.gugumarket.repository.UserRepository;
import com.project.gugumarket.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors; // ✅ 추가

/**
 * 마이페이지 관련 API를 처리하는 컨트롤러
 * 사용자 정보 조회, 수정, 찜 목록, 구매/판매 내역, 알림 관리 등의 기능 제공
 */
@RequiredArgsConstructor // final 필드에 대한 생성자 자동 생성
@RestController // REST API 컨트롤러 선언
@RequestMapping("/mypage") // 기본 URL 경로 설정
public class MypageController {

    // 비밀번호 암호화를 위한 인코더
    private final PasswordEncoder passwordEncoder;
    // 사용자 정보 데이터베이스 접근을 위한 레포지토리
    private final UserRepository userRepository;
    // 마이페이지 관련 비즈니스 로직 처리 서비스
    private final MypageService mypageService;
    // 찜(좋아요) 기능 처리 서비스
    private final LikeService likeService;
    // 거래 내역 처리 서비스
    private final TransactionService transactionService;
    // 알림 정보 데이터베이스 접근을 위한 레포지토리
    private final NotificationRepository notificationRepository;
    // 알림 관련 비즈니스 로직 처리 서비스
    private final NotificationService notificationService;
    // 사용자 관련 비즈니스 로직 처리 서비스
    private final UserService userService;
    // 상품 관련 비즈니스 로직 처리 서비스
    private final ProductService productService;

    /**
     * 마이페이지 메인 화면 데이터 조회
     * @param principal 현재 로그인한 사용자 정보
     * @return 사용자 정보, 찜 목록, 구매/판매 내역, 등록 상품, 알림 등의 데이터
     * 뼈대를 구축 해서 필터 체인 초기화,stateless 상태에서는 필수적 이다.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> mypage(Principal principal) {
        Map<String, Object> response = new HashMap<>();

        // 로그인 여부 확인
        if (principal == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // 로그인한 사용자 정보 조회
        String userName = principal.getName();
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 디버깅을 위한 콘솔 출력
        System.out.println("=== 마이페이지 로드 ===");
        System.out.println("사용자: " + userName);
        System.out.println("프로필 이미지: " + user.getProfileImage());
        System.out.println("프로필 이미지 (기본값 포함): " + user.getProfileImageOrDefault());

        // 사용자 관련 데이터 조회
        //List=인덱스(순서)를 가지며 이를 통해 객체에 접근 하거나 삽입/삭제를 한다
        List<Like> likes = likeService.getUserLikes(user); // 찜한 상품 목록
        List<Transaction> purchases = transactionService.findByBuyer(user); // 구매 내역
        List<Transaction> sales = transactionService.findBySeller(user); // 판매 내역
        List<Notification> recentNotifications = notificationService.getRecentNotifications(user, 5); // 최근 알림 5개
        long unreadCount = notificationService.getUnreadCount(user); // 읽지 않은 알림 개수
        List<Product> products = productService.getProductsBySeller(user); // 사용자가 등록한 모든 상품

        // Entity를 DTO로 변환 (보안 및 필요한 데이터만 전송)
        //Stream은 컬렉션의 요소를 함수형 스타일로 처리할 수 있도록 해주는 도구
        //.map()은 Stream 내의 각 요소를 다른 형태의 요소로 변환하는 중간 연산자 (LikeResponseDto::fromEntity(dto에 해당하는 entity))
        //map을 통해 모은 객체들을 모아서 List 객체화
        List<LikeResponseDto> likeDtos = likes.stream()
                .map(LikeResponseDto::fromEntity)
                .collect(Collectors.toList());
        List<TransactionResponseDto> purchaseDtos = purchases.stream()
                .map(TransactionResponseDto::fromEntity)
                .collect(Collectors.toList());
        List<TransactionResponseDto> salesDtos = sales.stream()
                .map(TransactionResponseDto::fromEntity)
                .collect(Collectors.toList());
        List<NotificationResponseDto> notificationDtos = recentNotifications.stream()
                .map(NotificationResponseDto::fromEntity)
                .collect(Collectors.toList());
        List<ProductDetailResponse> productListDtos = products.stream()
                .map(ProductDetailResponse::from)
                .collect(Collectors.toList());

        // 응답 데이터 구성
        response.put("success", true);
        response.put("user", UserResponseDto.fromEntity(user));
        response.put("likes", likeDtos);
        response.put("purchases", purchaseDtos);
        response.put("sales", salesDtos);
        response.put("recentNotifications", notificationDtos);
        response.put("products", productListDtos);
        response.put("unreadCount", unreadCount);

        // 캐시 방지 헤더를 추가하여 항상 최신 데이터 조회
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache().noStore().mustRevalidate())
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(response);
    }

    /**
     * 프로필 수정 화면 데이터 조회
     * @param principal 현재 로그인한 사용자 정보
     * @return 수정 가능한 사용자 정보
     */
    @GetMapping("/edit")
    public ResponseEntity<Map<String, Object>> editForm(Principal principal) {
        Map<String, Object> response = new HashMap<>();

        // 로그인 여부 확인
        if (principal == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // 사용자 정보 조회
        String userName = principal.getName();
        User user = mypageService.getUserByUserName(userName);

        // 수정 폼에 표시할 DTO 생성 (기존 값으로 초기화)
        UserUpdateDto userDto = new UserUpdateDto();
        userDto.setNickname(user.getNickname());
        userDto.setEmail(user.getEmail());
        userDto.setPhone(user.getPhone());
        userDto.setPostalCode(user.getPostalCode());
        userDto.setAddress(user.getAddress());
        userDto.setAddressDetail(user.getAddressDetail());

        response.put("success", true);
        response.put("user", UserResponseDto.fromEntity(user));
        response.put("userDto", userDto);

        // 캐시 방지
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache().noStore().mustRevalidate())
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(response);
    }

    /**
     * 프로필 정보 수정 처리
     * @param nickname 닉네임
     * @param email 이메일
     * @param phone 전화번호 (선택)
     * @param postalCode 우편번호
     * @param address 주소
     * @param addressDetail 상세주소
     * @param currentPassword 현재 비밀번호 (비밀번호 변경 시 필수)
     * @param newPassword 새 비밀번호 (선택)
     * @param confirmPassword 새 비밀번호 확인 (선택)
     * @param profileImage 프로필 이미지 파일 (선택)
     * @param deleteProfileImage 프로필 이미지 삭제 여부
     * @param principal 현재 로그인한 사용자 정보
     * @return 수정 결과
     */
    @PostMapping("/edit")
    public ResponseEntity<Map<String, Object>> editProfile(
            @RequestParam String nickname,
            @RequestParam String email,
            @RequestParam(required = false) String phone,
            @RequestParam String postalCode,
            @RequestParam String address,
            @RequestParam String addressDetail,
            @RequestParam(required = false) String currentPassword,
            @RequestParam(required = false) String newPassword,
            @RequestParam(required = false) String confirmPassword,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
            @RequestParam(value = "deleteProfileImage", required = false) String deleteProfileImage,
            Principal principal) {

        Map<String, Object> response = new HashMap<>();

        // 디버깅 로그 시작
        System.out.println("\n========================================");
        System.out.println("🚀 프로필 수정 요청 시작!");
        System.out.println("========================================");

        // 로그인 여부 확인
        if (principal == null) {
            System.out.println("❌ Principal이 null입니다!");
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // 사용자 정보 조회
        String userName = principal.getName();
        System.out.println("✅ 로그인 사용자: " + userName);

        User user = mypageService.getUserByUserName(userName);
        System.out.println("✅ 사용자 정보 조회 완료: " + user.getNickname());

        // 받은 데이터 확인 (디버깅용)
        System.out.println("\n🔥 받은 데이터:");
        System.out.println("  - 닉네임: " + nickname);
        System.out.println("  - 이메일: " + email);
        System.out.println("  - 전화번호: " + phone);
        System.out.println("  - 주소: " + address);
        System.out.println("  - 상세주소: " + addressDetail);
        System.out.println("  - 우편번호: " + postalCode);

        // 필수 항목 유효성 검증 //trim=공백 제거
        if (nickname == null || nickname.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "닉네임은 필수 항목입니다.");
            return ResponseEntity.badRequest().body(response);
        }
        if (email == null || !email.contains("@")) {
            response.put("success", false);
            response.put("message", "올바른 이메일 형식이 아닙니다.");
            return ResponseEntity.badRequest().body(response);
        }

        System.out.println("✅ 유효성 검사 통과!");

        try {
            System.out.println("\n📄 데이터 업데이트 시작...");

            // 1️⃣ 프로필 이미지 처리
            //if가 발동 되면 elseif는 생략
            if ("true".equals(deleteProfileImage)) {
                // 프로필 이미지 삭제
                System.out.println("🗑️ 프로필 이미지 삭제");
                user.setProfileImage(null);
            } else if (profileImage != null && !profileImage.isEmpty()) {
                // 새 프로필 이미지 업로드
                System.out.println("📤 프로필 이미지 업로드 시작");

                // 파일 크기 검증 (5MB 제한)
                if (profileImage.getSize() > 5 * 1024 * 1024) {
                    response.put("success", false);
                    response.put("message", "파일 크기는 5MB 이하여야 합니다.");
                    return ResponseEntity.badRequest().body(response);
                }

                // 파일 형식 검증 (이미지 파일만 허용)
                String contentType = profileImage.getContentType();
                if (contentType == null ||
                        (!contentType.equals("image/jpeg") &&
                                !contentType.equals("image/jpg") &&
                                !contentType.equals("image/png") &&
                                !contentType.equals("image/gif"))) {
                    response.put("success", false);
                    response.put("message", "JPG, PNG, GIF 형식의 이미지만 업로드 가능합니다.");
                    return ResponseEntity.badRequest().body(response);
                }

                // 파일 업로드 및 URL 저장
                String imageUrl = mypageService.uploadProfileImage(profileImage, userName);
                user.setProfileImage(imageUrl);
                System.out.println("✅ 프로필 이미지 업로드 완료: " + imageUrl);
            }

            // 2️⃣ 비밀번호 변경 처리
            // 비밀번호 관련 필드 중 하나라도 입력되었는지 확인
            boolean passwordChangeRequested =
                    (currentPassword != null && !currentPassword.isEmpty()) ||
                            (newPassword != null && !newPassword.isEmpty()) ||
                            (confirmPassword != null && !confirmPassword.isEmpty());

            if (passwordChangeRequested) {
                // 현재 비밀번호 입력 확인
                if (currentPassword == null || currentPassword.isEmpty()) {
                    response.put("success", false);
                    response.put("message", "현재 비밀번호를 입력해주세요.");
                    return ResponseEntity.badRequest().body(response);
                }

                // 현재 비밀번호 일치 여부 확인
                if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                    response.put("success", false); //(key,value)
                    response.put("message", "현재 비밀번호가 일치하지 않습니다.");
                    return ResponseEntity.badRequest().body(response);
                }

                // 새 비밀번호 입력 확인
                if (newPassword == null || newPassword.isEmpty()) {
                    response.put("success", false);
                    response.put("message", "새 비밀번호를 입력해주세요.");
                    return ResponseEntity.badRequest().body(response);
                }

                // 새 비밀번호 일치 여부 확인
                if (!newPassword.equals(confirmPassword)) {
                    response.put("success", false);
                    response.put("message", "새 비밀번호가 일치하지 않습니다.");
                    return ResponseEntity.badRequest().body(response);
                }

                // 비밀번호 암호화 후 저장
                user.setPassword(passwordEncoder.encode(newPassword));
                System.out.println("🔐 비밀번호 변경 완료");
            }

            // 3️⃣ 기본 정보 업데이트
            System.out.println("\n📝 사용자 정보 업데이트 중...");
            System.out.println("  - 기존 닉네임: " + user.getNickname() + " → 새 닉네임: " + nickname);
            System.out.println("  - 기존 이메일: " + user.getEmail() + " → 새 이메일: " + email);

            user.setNickname(nickname);
            user.setEmail(email);
            user.setPhone(phone);
            user.setAddress(address);
            user.setAddressDetail(addressDetail);
            user.setPostalCode(postalCode);

            // 4️⃣ 모든 변경사항을 데이터베이스에 저장
            System.out.println("\n💾 데이터베이스에 저장 중...");
            User savedUser = userRepository.save(user);

            // 저장 완료 로그
            System.out.println("✅ 모든 정보 저장 완료!");
            System.out.println("   - 저장된 닉네임: " + savedUser.getNickname());
            System.out.println("   - 저장된 이메일: " + savedUser.getEmail());
            System.out.println("   - 저장된 프로필 이미지: " + savedUser.getProfileImage());
            System.out.println("========================================\n");

            // 성공 응답
            response.put("success", true);
            response.put("message", "회원정보가 수정되었습니다.");
            response.put("user", UserResponseDto.fromEntity(savedUser));

            // 캐시 방지
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noCache().noStore().mustRevalidate())
                    .header("Pragma", "no-cache")
                    .header("Expires", "0")
                    .body(response);

        } catch (IOException e) {
            // 파일 처리 중 오류 발생 시
            System.err.println("❌ 파일 처리 중 오류: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "프로필 이미지 업로드 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 찜(좋아요) 목록 조회
     * @param principal 현재 로그인한 사용자 정보
     * @return 사용자가 찜한 상품 목록
     */
    @GetMapping("/likes")
    public ResponseEntity<Map<String, Object>> likeList(Principal principal) {
        Map<String, Object> response = new HashMap<>();

        // 로그인 여부 확인
        if (principal == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // 사용자 정보 조회
        String userName = principal.getName();
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 찜 목록 조회
        List<Like> likeList = likeService.getUserLikes(user);

        // Entity를 DTO로 변환
        List<LikeResponseDto> likeDtos = likeList.stream()
                .map(LikeResponseDto::fromEntity)
                .collect(Collectors.toList());

        response.put("success", true);
        response.put("likeList", likeDtos);
        response.put("user", UserResponseDto.fromEntity(user));

        return ResponseEntity.ok(response);
    }

    /**
     * 구매 내역 조회
     * @param principal 현재 로그인한 사용자 정보
     * @return 사용자의 구매 내역 목록
     */
    @GetMapping("/purchases")
    public ResponseEntity<Map<String, Object>> purchaseList(Principal principal) {
        Map<String, Object> response = new HashMap<>();

        // 로그인 여부 확인
        if (principal == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // 사용자 정보 조회
        String userName = principal.getName();
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 구매 내역 조회
        List<Transaction> purchases = transactionService.getPurchasesByBuyer(user);

        // Entity를 DTO로 변환
        List<TransactionResponseDto> purchaseDtos = purchases.stream()
                .map(TransactionResponseDto::fromEntity)
                .collect(Collectors.toList());

        response.put("success", true);
        response.put("user", UserResponseDto.fromEntity(user));
        response.put("purchases", purchaseDtos);

        return ResponseEntity.ok(response);
    }

    /**
     * 판매 내역 조회
     * @param principal 현재 로그인한 사용자 정보
     * @return 사용자의 판매 내역 목록
     */
    @GetMapping("/sales")
    public ResponseEntity<Map<String, Object>> salesList(Principal principal) {
        Map<String, Object> response = new HashMap<>();

        // 로그인 여부 확인
        if (principal == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // 사용자 정보 조회
        String userName = principal.getName();
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 판매 내역 조회
        List<Transaction> sales = transactionService.getSalesBySeller(user);

        // Entity를 DTO로 변환
        //Stream 연산은 원본 데이터 소스를 직접 변경x 항상 새로운 결과를 생성 연속 작업에 용이 최종연산이 호출 될 때 한번에 처리 됨
        List<TransactionResponseDto> salesDtos = sales.stream()
                .map(TransactionResponseDto::fromEntity)
                .collect(Collectors.toList());

        response.put("success", true);
        response.put("user", UserResponseDto.fromEntity(user));
        response.put("sales", salesDtos);

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 사용자의 모든 알림 조회 (내부 사용)
     * @param user 사용자 엔티티
     * @return 알림 목록 (생성일 기준 내림차순)
     */
    public List<Notification> getNotificationsByUser(User user) {
        return notificationRepository.findByReceiverOrderByCreatedDateDesc(user);
    }

    /**
     * 알림 전체 목록 조회
     * @param principal 현재 로그인한 사용자 정보
     * @return 사용자의 모든 알림 목록과 읽지 않은 알림 개수
     */
    @GetMapping("/notifications")
    public ResponseEntity<Map<String, Object>> notificationsPage(Principal principal) {
        Map<String, Object> response = new HashMap<>();

        // 로그인 여부 확인
        if (principal == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // 사용자 정보 및 알림 조회
        User user = userService.getUserByUserName(principal.getName());
        List<Notification> notifications = notificationService.getUserNotifications(user);
        long unreadCount = notificationService.getUnreadCount(user);

        // Entity를 DTO로 변환
        List<NotificationResponseDto> notificationDtos = notifications.stream()
                .map(NotificationResponseDto::fromEntity)
                .collect(Collectors.toList());

        response.put("success", true);
        response.put("user", UserResponseDto.fromEntity(user));
        response.put("notifications", notificationDtos);
        response.put("unreadCount", unreadCount);

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 알림을 읽음 처리
     * @param id 알림 ID
     * @param principal 현재 로그인한 사용자 정보
     * @return 처리 결과
     */
    @PostMapping("/notifications/{id}/read")
    public ResponseEntity<Map<String, Object>> markNotificationAsRead(@PathVariable Long id, Principal principal) {
        Map<String, Object> response = new HashMap<>();

        // 로그인 여부 확인
        if (principal == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // 알림 읽음 처리
        User user = userService.getUserByUserName(principal.getName());
        notificationService.markAsRead(id, user);

        response.put("success", true);
        response.put("message", "알림을 읽음 처리했습니다.");
        return ResponseEntity.ok(response);
    }

    /**
     * 모든 알림을 읽음 처리
     * @param principal 현재 로그인한 사용자 정보
     * @return 처리 결과
     */
    @PostMapping("/notifications/read-all")
    public ResponseEntity<Map<String, Object>> markAllNotificationsAsRead(Principal principal) {
        Map<String, Object> response = new HashMap<>();

        // 로그인 여부 확인
        if (principal == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // 모든 알림 읽음 처리
        User user = userService.getUserByUserName(principal.getName());
        notificationService.markAllAsRead(user);

        response.put("success", true);
        response.put("message", "모든 알림을 읽음 처리했습니다.");
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 알림 삭제
     * @param id 알림 ID
     * @param principal 현재 로그인한 사용자 정보
     * @return 처리 결과
     */
    @PostMapping("/notifications/{id}/delete")
    public ResponseEntity<Map<String, Object>> deleteNotification(@PathVariable Long id, Principal principal) {
        Map<String, Object> response = new HashMap<>();

        // 로그인 여부 확인
        if (principal == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // 알림 삭제
        User user = userService.getUserByUserName(principal.getName());
        notificationService.deleteNotification(id, user);

        response.put("success", true);
        response.put("message", "알림이 삭제되었습니다.");
        return ResponseEntity.ok(response);
    }


}