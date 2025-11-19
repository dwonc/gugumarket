package com.project.gugumarket.controller;

import com.project.gugumarket.dto.UserDto;
import com.project.gugumarket.dto.UserUpdateDto;
import com.project.gugumarket.entity.Like;
import com.project.gugumarket.entity.Notification;
import com.project.gugumarket.entity.Transaction;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.NotificationRepository;
import com.project.gugumarket.repository.UserRepository;
import com.project.gugumarket.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/mypage")
public class MypageController {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final MypageService mypageService;
    private final LikeService likeService;
    private final TransactionService transactionService;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> mypage(Principal principal) {
        Map<String, Object> response = new HashMap<>();

        if (principal == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        String userName = principal.getName();
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        System.out.println("=== 마이페이지 로드 ===");
        System.out.println("사용자: " + userName);
        System.out.println("프로필 이미지: " + user.getProfileImage());
        System.out.println("프로필 이미지 (기본값 포함): " + user.getProfileImageOrDefault());

        List<Like> likes = likeService.getUserLikes(user);
        // ✅ 구매내역
        List<Transaction> purchases = transactionService.findByBuyer(user);
        // ✅ 판매내역
        List<Transaction> sales = transactionService.findBySeller(user);
        List<Notification> recentNotifications = notificationService.getRecentNotifications(user, 5);
        long unreadCount = notificationService.getUnreadCount(user);

        response.put("success", true);
        response.put("user", user);
        response.put("likes", likes);
        response.put("purchases", purchases);
        response.put("sales", sales);
        response.put("recentNotifications", recentNotifications);
        response.put("unreadCount", unreadCount);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/edit")
    public ResponseEntity<Map<String, Object>> editForm(Principal principal) {
        Map<String, Object> response = new HashMap<>();

        if (principal == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        String userName = principal.getName();
        User user = mypageService.getUserByUserName(userName);

        // ✅ UserUpdateDto 생성 (User 엔티티에서 값 복사)
        UserUpdateDto userDto = new UserUpdateDto();
        userDto.setNickname(user.getNickname());
        userDto.setEmail(user.getEmail());
        userDto.setPhone(user.getPhone());
        userDto.setPostalCode(user.getPostalCode());
        userDto.setAddress(user.getAddress());
        userDto.setAddressDetail(user.getAddressDetail());

        response.put("success", true);
        response.put("user", user);
        response.put("userDto", userDto);

        return ResponseEntity.ok(response);
    }

    // ✅ 단순화된 프로필 수정 처리
    @PostMapping("/edit")
    public ResponseEntity<Map<String, Object>> editProfile(
            @Valid @RequestPart("userDto") UserUpdateDto userDto,
            BindingResult bindingResult,
            @RequestParam(required = false) String currentPassword,
            @RequestParam(required = false) String newPassword,
            @RequestParam(required = false) String confirmPassword,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
            @RequestParam(value = "deleteProfileImage", required = false) String deleteProfileImage,
            Principal principal) {

        Map<String, Object> response = new HashMap<>();

        // 🔥 디버깅 로그 추가
        System.out.println("\n========================================");
        System.out.println("🚀 프로필 수정 요청 시작!");
        System.out.println("========================================");

        if (principal == null) {
            System.out.println("❌ Principal이 null입니다!");
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        String userName = principal.getName();
        System.out.println("✅ 로그인 사용자: " + userName);

        User user = mypageService.getUserByUserName(userName);
        System.out.println("✅ 사용자 정보 조회 완료: " + user.getNickname());

        // 🔥 받은 데이터 확인
        System.out.println("\n🔥 받은 데이터:");
        System.out.println("  - 닉네임: " + userDto.getNickname());
        System.out.println("  - 이메일: " + userDto.getEmail());
        System.out.println("  - 전화번호: " + userDto.getPhone());
        System.out.println("  - 주소: " + userDto.getAddress());
        System.out.println("  - 상세주소: " + userDto.getAddressDetail());
        System.out.println("  - 우편번호: " + userDto.getPostalCode());

        System.out.println("\n🔍 유효성 검사:");
        System.out.println("  - 에러 있음: " + bindingResult.hasErrors());

        // 유효성 검증 실패 시 먼저 처리
        if (bindingResult.hasErrors()) {
            System.out.println("❌ 유효성 검사 실패!");
            Map<String, String> errors = bindingResult.getFieldErrors().stream()
                    .collect(Collectors.toMap(
                            FieldError::getField,
                            error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : ""
                    ));
            bindingResult.getAllErrors().forEach(error -> {
                System.out.println("    * " + error.getDefaultMessage());
            });
            response.put("success", false);
            response.put("errors", errors);
            return ResponseEntity.badRequest().body(response);
        }

        System.out.println("✅ 유효성 검사 통과!");

        try {
            System.out.println("\n📄 데이터 업데이트 시작...");

            // 1️⃣ 프로필 이미지 처리
            if ("true".equals(deleteProfileImage)) {
                System.out.println("🗑️ 프로필 이미지 삭제");
                user.setProfileImage(null);
            } else if (profileImage != null && !profileImage.isEmpty()) {
                System.out.println("📤 프로필 이미지 업로드 시작");

                // 파일 크기 체크 (5MB)
                if (profileImage.getSize() > 5 * 1024 * 1024) {
                    response.put("success", false);
                    response.put("message", "파일 크기는 5MB 이하여야 합니다.");
                    return ResponseEntity.badRequest().body(response);
                }

                // 파일 형식 체크
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

                // MypageService를 통한 파일 업로드
                String imageUrl = mypageService.uploadProfileImage(profileImage, userName);
                user.setProfileImage(imageUrl);
                System.out.println("✅ 프로필 이미지 업로드 완료: " + imageUrl);
            }

            // 2️⃣ 비밀번호 변경 처리
            boolean passwordChangeRequested =
                    (currentPassword != null && !currentPassword.isEmpty()) ||
                            (newPassword != null && !newPassword.isEmpty()) ||
                            (confirmPassword != null && !confirmPassword.isEmpty());

            if (passwordChangeRequested) {
                if (currentPassword == null || currentPassword.isEmpty()) {
                    response.put("success", false);
                    response.put("message", "현재 비밀번호를 입력해주세요.");
                    return ResponseEntity.badRequest().body(response);
                }

                if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                    response.put("success", false);
                    response.put("message", "현재 비밀번호가 일치하지 않습니다.");
                    return ResponseEntity.badRequest().body(response);
                }

                if (newPassword == null || newPassword.isEmpty()) {
                    response.put("success", false);
                    response.put("message", "새 비밀번호를 입력해주세요.");
                    return ResponseEntity.badRequest().body(response);
                }

                if (!newPassword.equals(confirmPassword)) {
                    response.put("success", false);
                    response.put("message", "새 비밀번호가 일치하지 않습니다.");
                    return ResponseEntity.badRequest().body(response);
                }

                user.setPassword(passwordEncoder.encode(newPassword));
                System.out.println("🔐 비밀번호 변경 완료");
            }

            // 3️⃣ 기본 정보 업데이트
            System.out.println("\n📝 사용자 정보 업데이트 중...");
            System.out.println("  - 기존 닉네임: " + user.getNickname() + " → 새 닉네임: " + userDto.getNickname());
            System.out.println("  - 기존 이메일: " + user.getEmail() + " → 새 이메일: " + userDto.getEmail());

            user.setNickname(userDto.getNickname());
            user.setEmail(userDto.getEmail());
            user.setPhone(userDto.getPhone());
            user.setAddress(userDto.getAddress());
            user.setAddressDetail(userDto.getAddressDetail());
            user.setPostalCode(userDto.getPostalCode());

            // 4️⃣ 한 번에 모든 정보 저장
            System.out.println("\n💾 데이터베이스에 저장 중...");
            User savedUser = userRepository.save(user);

            System.out.println("✅ 모든 정보 저장 완료!");
            System.out.println("   - 저장된 닉네임: " + savedUser.getNickname());
            System.out.println("   - 저장된 이메일: " + savedUser.getEmail());
            System.out.println("   - 저장된 프로필 이미지: " + savedUser.getProfileImage());
            System.out.println("========================================\n");

            response.put("success", true);
            response.put("message", "회원정보가 수정되었습니다.");
            response.put("user", savedUser);
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            System.err.println("❌ 파일 처리 중 오류: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "프로필 이미지 업로드 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/likes")
    public ResponseEntity<Map<String, Object>> likeList(Principal principal) {
        Map<String, Object> response = new HashMap<>();

        if (principal == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        String userName = principal.getName();
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<Like> likeList = likeService.getUserLikes(user);

        response.put("success", true);
        response.put("likeList", likeList);
        response.put("user", user);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/purchases")
    public ResponseEntity<Map<String, Object>> purchaseList(Principal principal) {
        Map<String, Object> response = new HashMap<>();

        if (principal == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        String userName = principal.getName();
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<Transaction> purchases = transactionService.getPurchasesByBuyer(user);

        response.put("success", true);
        response.put("user", user);
        response.put("purchases", purchases);

        return ResponseEntity.ok(response);
    }

    /**
     * 🛒 판매 내역 보기
     */
    @GetMapping("/sales")
    public ResponseEntity<Map<String, Object>> salesList(Principal principal) {
        Map<String, Object> response = new HashMap<>();

        if (principal == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        String userName = principal.getName();
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // ✅ TransactionService를 통해 판매 내역 조회
        List<Transaction> sales = transactionService.getSalesBySeller(user);

        response.put("success", true);
        response.put("user", user);
        response.put("sales", sales);

        return ResponseEntity.ok(response);
    }

    // ✅ 사용자별 알림 내역 조회
    public List<Notification> getNotificationsByUser(User user) {
        return notificationRepository.findByReceiverOrderByCreatedDateDesc(user);
    }

    // 알림 전체 보기 페이지
    @GetMapping("/notifications")
    public ResponseEntity<Map<String, Object>> notificationsPage(Principal principal) {
        Map<String, Object> response = new HashMap<>();

        if (principal == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        User user = userService.getUserByUserName(principal.getName());
        List<Notification> notifications = notificationService.getUserNotifications(user);
        long unreadCount = notificationService.getUnreadCount(user);

        response.put("success", true);
        response.put("user", user);
        response.put("notifications", notifications);
        response.put("unreadCount", unreadCount);

        return ResponseEntity.ok(response);
    }

    // 알림 읽음 처리
    @PostMapping("/notifications/{id}/read")
    public ResponseEntity<Map<String, Object>> markNotificationAsRead(@PathVariable Long id, Principal principal) {
        Map<String, Object> response = new HashMap<>();

        if (principal == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        User user = userService.getUserByUserName(principal.getName());
        notificationService.markAsRead(id, user);

        response.put("success", true);
        response.put("message", "알림을 읽음 처리했습니다.");
        return ResponseEntity.ok(response);
    }

    // 모든 알림 읽음 처리
    @PostMapping("/notifications/read-all")
    public ResponseEntity<Map<String, Object>> markAllNotificationsAsRead(Principal principal) {
        Map<String, Object> response = new HashMap<>();

        if (principal == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        User user = userService.getUserByUserName(principal.getName());
        notificationService.markAllAsRead(user);

        response.put("success", true);
        response.put("message", "모든 알림을 읽음 처리했습니다.");
        return ResponseEntity.ok(response);
    }

    // 알림 삭제
    @PostMapping("/notifications/{id}/delete")
    public ResponseEntity<Map<String, Object>> deleteNotification(@PathVariable Long id, Principal principal) {
        Map<String, Object> response = new HashMap<>();

        if (principal == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        User user = userService.getUserByUserName(principal.getName());
        notificationService.deleteNotification(id, user);

        response.put("success", true);
        response.put("message", "알림이 삭제되었습니다.");
        return ResponseEntity.ok(response);
    }
}