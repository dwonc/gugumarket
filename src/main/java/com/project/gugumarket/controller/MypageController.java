package com.project.gugumarket.controller;

import com.project.gugumarket.dto.UserDto;
<<<<<<< HEAD
import com.project.gugumarket.entity.Like;
=======
import com.project.gugumarket.dto.UserUpdateDto;
import com.project.gugumarket.entity.Like;
import com.project.gugumarket.entity.Notification;
>>>>>>> 28cebc40083f14c3d32f93518519a56ce9ec8b8a
import com.project.gugumarket.entity.Transaction;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.NotificationRepository;
import com.project.gugumarket.repository.UserRepository;
<<<<<<< HEAD
import com.project.gugumarket.service.LikeService;
import com.project.gugumarket.service.TransactionService;
import com.project.gugumarket.service.UserService;
import com.project.gugumarket.service.MypageService;
=======
import com.project.gugumarket.service.*;
>>>>>>> 28cebc40083f14c3d32f93518519a56ce9ec8b8a
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.io.IOException;
import java.security.Principal;
import java.util.List;

@RequiredArgsConstructor
@Controller
public class MypageController {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final MypageService mypageService;
    private final LikeService likeService;
    private final TransactionService transactionService;
<<<<<<< HEAD

=======
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final UserService userService;
>>>>>>> 28cebc40083f14c3d32f93518519a56ce9ec8b8a

    @GetMapping("/mypage")
    public String mypage(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";

        String userName = principal.getName();
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        // 프로필 이미지 로그 출력
        System.out.println("=== 마이페이지 로드 ===");
        System.out.println("사용자: " + userName);
        System.out.println("프로필 이미지: " + user.getProfileImage());
        System.out.println("프로필 이미지 (기본값 포함): " + user.getProfileImageOrDefault());

        List<Like> likes = likeService.getUserLikes(user);

        model.addAttribute("likes", likes);
        model.addAttribute("user", user);
        return "users/mypage";


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
        List<Notification> recentNotifications = notificationService.getRecentNotifications(user,5);
        long unreadCount = notificationService.getUnreadCount(user);

        model.addAttribute("recentNotifications", recentNotifications);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("likes", likes);
        model.addAttribute("user", user);
        model.addAttribute("purchases", purchases);
        model.addAttribute("sales", sales);
        return "users/mypage";
    }

    @GetMapping("/mypage/edit")
    public String editForm(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";

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

        model.addAttribute("user", user);
        model.addAttribute("userDto", userDto);

        return "users/edit";
    }

    // ✅ 단순화된 프로필 수정 처리
    @PostMapping("/users/edit")
    public String editProfile(
            @Valid @ModelAttribute UserUpdateDto userDto,  // ← 여기만 변경!
            BindingResult bindingResult,
            @RequestParam(required = false) String currentPassword,
            @RequestParam(required = false) String newPassword,
            @RequestParam(required = false) String confirmPassword,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
            @RequestParam(value = "deleteProfileImage", required = false) String deleteProfileImage,
            @RequestParam(value = "currentProfileImage", required = false) String currentProfileImage,

            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        // 🔥 디버깅 로그 추가
        System.out.println("\n========================================");
        System.out.println("🚀 프로필 수정 요청 시작!");
        System.out.println("========================================");

        if (principal == null) {
            System.out.println("❌ Principal이 null입니다!");
            return "redirect:/login";
        }

        String userName = principal.getName();
        System.out.println("✅ 로그인 사용자: " + userName);

        User user = mypageService.getUserByUserName(userName);
        System.out.println("✅ 사용자 정보 조회 완료: " + user.getNickname());

        // 🔥 받은 데이터 확인
        System.out.println("\n📥 받은 데이터:");
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
            bindingResult.getAllErrors().forEach(error -> {
                System.out.println("    * " + error.getDefaultMessage());
            });
            model.addAttribute("user", user);
            model.addAttribute("userDto", userDto);
            return "users/edit";
        }
<<<<<<< HEAD
        else if (profileImage != null && !profileImage.isEmpty()) {
            System.out.println("프로필 이미지 업로드 시작");

            try {
                if (profileImage.getSize() > 5 * 1024 * 1024) {
                    model.addAttribute("error", "파일 크기는 5MB 이하여야 합니다.");
                    model.addAttribute("user", user);
                    model.addAttribute("userDto", userDto);
                    return "users/edit";
                }
        // 프로필 이미지 업로드 처리
        else if (profileImage != null && !profileImage.isEmpty()) {
            System.out.println("프로필 이미지 업로드 시작");
            System.out.println("   - 파일명: " + profileImage.getOriginalFilename());
            System.out.println("   - 파일 크기: " + profileImage.getSize() + " bytes");
            System.out.println("   - Content-Type: " + profileImage.getContentType());
=======
>>>>>>> 28cebc40083f14c3d32f93518519a56ce9ec8b8a

        System.out.println("✅ 유효성 검사 통과!");

        try {
            System.out.println("\n🔄 데이터 업데이트 시작...");

            // 1️⃣ 프로필 이미지 처리
            if ("true".equals(deleteProfileImage)) {
                System.out.println("🗑️ 프로필 이미지 삭제");
                user.setProfileImage(null);
            } else if (profileImage != null && !profileImage.isEmpty()) {
                System.out.println("📤 프로필 이미지 업로드 시작");

                // 파일 크기 체크 (5MB)
                if (profileImage.getSize() > 5 * 1024 * 1024) {
                    model.addAttribute("error", "파일 크기는 5MB 이하여야 합니다.");
                    model.addAttribute("user", user);
                    model.addAttribute("userDto", userDto);
                    return "users/edit";
                }

                // 파일 형식 체크
                String contentType = profileImage.getContentType();
                if (contentType == null ||
                        (!contentType.equals("image/jpeg") &&
                                !contentType.equals("image/jpg") &&
                                !contentType.equals("image/png") &&
                                !contentType.equals("image/gif"))) {
                    model.addAttribute("error", "JPG, PNG, GIF 형식의 이미지만 업로드 가능합니다.");
                    model.addAttribute("user", user);
                    model.addAttribute("userDto", userDto);
                    return "users/edit";
                }

<<<<<<< HEAD
                // 파일 저장
                String fileName = saveProfileImage(profileImage, userName);
                user.setProfileImage(fileName);
                System.out.println("프로필 이미지 업로드 완료: " + fileName);

            } catch (Exception e) {
                model.addAttribute("error", "프로필 이미지 업로드 중 오류가 발생했습니다.");
                model.addAttribute("user", user);
                model.addAttribute("userDto", userDto);
                return "users/edit";
            }
        } else {
            if (currentProfileImage != null && !currentProfileImage.isEmpty()) {
                user.setProfileImage(currentProfileImage);
                System.out.println("기존 프로필 유지: " + currentProfileImage);
            }
                System.err.println("프로필 이미지 업로드 중 오류: " + e.getMessage());
                e.printStackTrace();
                model.addAttribute("error", "프로필 이미지 업로드 중 오류가 발생했습니다: " + e.getMessage());
                model.addAttribute("user", user);
                model.addAttribute("userDto", userDto);
                return "users/edit";
            }
        } else {
            System.out.println("프로필 이미지 변경 없음");
        }

        // 비밀번호 변경 요청이 있는 경우
        boolean passwordChangeRequested =
                (currentPassword != null && !currentPassword.isEmpty()) ||
                        (newPassword != null && !newPassword.isEmpty()) ||
                        (confirmPassword != null && !confirmPassword.isEmpty());

        if (passwordChangeRequested) {
            // 현재 비밀번호 확인
            if (currentPassword == null || currentPassword.isEmpty()) {
                model.addAttribute("error", "현재 비밀번호를 입력해주세요.");
                model.addAttribute("user", user);
                model.addAttribute("userDto", userDto);
                return "users/edit";
            }

            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                model.addAttribute("error", "현재 비밀번호가 일치하지 않습니다.");
                model.addAttribute("user", user);
                model.addAttribute("userDto", userDto);
                return "users/edit";
            }

            // 새 비밀번호 확인
            if (newPassword == null || newPassword.isEmpty()) {
                model.addAttribute("error", "새 비밀번호를 입력해주세요.");
                model.addAttribute("user", user);
                model.addAttribute("userDto", userDto);
                return "users/edit";
                return "edit";
            }

            if (!newPassword.equals(confirmPassword)) {
                model.addAttribute("error", "새 비밀번호가 일치하지 않습니다.");
                model.addAttribute("user", user);
                model.addAttribute("userDto", userDto);
                return "users/edit";
            }

            user.setPassword(passwordEncoder.encode(newPassword));
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            return "users/edit";
        }
            // 비밀번호 변경
            user.setPassword(passwordEncoder.encode(newPassword));
        }
=======
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
                    model.addAttribute("error", "현재 비밀번호를 입력해주세요.");
                    model.addAttribute("user", user);
                    model.addAttribute("userDto", userDto);
                    return "users/edit";
                }

                if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                    model.addAttribute("error", "현재 비밀번호가 일치하지 않습니다.");
                    model.addAttribute("user", user);
                    model.addAttribute("userDto", userDto);
                    return "users/edit";
                }

                if (newPassword == null || newPassword.isEmpty()) {
                    model.addAttribute("error", "새 비밀번호를 입력해주세요.");
                    model.addAttribute("user", user);
                    model.addAttribute("userDto", userDto);
                    return "users/edit";
                }

                if (!newPassword.equals(confirmPassword)) {
                    model.addAttribute("error", "새 비밀번호가 일치하지 않습니다.");
                    model.addAttribute("user", user);
                    model.addAttribute("userDto", userDto);
                    return "users/edit";
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
>>>>>>> 28cebc40083f14c3d32f93518519a56ce9ec8b8a

            System.out.println("✅ 모든 정보 저장 완료!");
            System.out.println("   - 저장된 닉네임: " + savedUser.getNickname());
            System.out.println("   - 저장된 이메일: " + savedUser.getEmail());
            System.out.println("   - 저장된 프로필 이미지: " + savedUser.getProfileImage());
            System.out.println("========================================\n");

<<<<<<< HEAD
        // 기본 정보 업데이트
        user.setNickname(userDto.getNickname());
        user.setEmail(userDto.getEmail());
        user.setPhone(userDto.getPhone());
        user.setAddress(userDto.getAddress());
        user.setAddressDetail(userDto.getAddressDetail());
        user.setPostalCode(userDto.getPostalCode());

        userRepository.save(user);

        redirectAttributes.addFlashAttribute("successMessage", "회원정보가 수정되었습니다.");
        return "redirect:/mypage";
    }

    // 프로필 이미지 저장 메서드
    private String saveProfileImage(MultipartFile profileImage, String userName) throws IOException {
        try {
            String uploadDir = "uploads/";
            String originalFilename = profileImage.getOriginalFilename();

    private String saveProfileImage(MultipartFile file, String userName) throws IOException {
        try {
            // 업로드 디렉토리 설정 (프로젝트 루트 기준)
            String uploadDir = "uploads/profile/";

            // 파일명 생성 (중복 방지)
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = userName + "_" + System.currentTimeMillis() + extension;
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                System.out.println("✅ 디렉토리 생성: " + uploadPath.toAbsolutePath());
            }

            Path filePath = uploadPath.resolve(fileName);
            Files.copy(profileImage.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("✅ 파일 저장 성공: " + filePath.toAbsolutePath());
            System.out.println("✅ 반환 URL: /uploads/" + fileName);

            return "/uploads/" + fileName;

        } catch (IOException e) {
            System.err.println("❌ 파일 저장 실패: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("파일 저장 중 오류 발생: " + e.getMessage(), e);
        }
    }
    @GetMapping("/mypage/likes")
    public String listList(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        String userName = principal.getName();
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // LikeService를 통해 사용자의 찜 목록 조회
        List<Like> likeList = likeService.getUserLikes(user);
        model.addAttribute("likeList", likeList);
        model.addAttribute("user", user);
        // users/mypage_likes.html로 이동
        return "users/mypage_likes";
    }
    /**
     * 🛒 구매 내역 보기
     */
    @GetMapping("/mypage/purchases")
    public String purchaseList(Principal principal,Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        String userName = principal.getName();
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // ✅ TransactionService를 통해 구매 내역 가져오기
        List<Transaction> purchases = transactionService.getPurchasesByBuyer(user);

        model.addAttribute("user", user);
        model.addAttribute("purchases", purchases);

        return "users/mypage";
    }
        // 디렉토리 생성
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                System.out.println("디렉토리 생성: " + uploadPath.toAbsolutePath());
            }

            // 파일 저장
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("파일 저장 성공: " + filePath.toAbsolutePath());
            System.out.println("반환 URL: /uploads/profile/" + fileName);

            // 웹에서 접근 가능한 URL 반환
            return "/uploads/profile/" + fileName;
=======
            redirectAttributes.addFlashAttribute("successMessage", "회원정보가 수정되었습니다.");
            return "redirect:/mypage";
>>>>>>> 28cebc40083f14c3d32f93518519a56ce9ec8b8a

        } catch (IOException e) {
            System.err.println("❌ 파일 처리 중 오류: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "프로필 이미지 업로드 중 오류가 발생했습니다.");
            model.addAttribute("user", user);
            model.addAttribute("userDto", userDto);
            return "users/edit";
        }
    }

    @GetMapping("/mypage/likes")
    public String likeList(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        String userName = principal.getName();
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<Like> likeList = likeService.getUserLikes(user);
        model.addAttribute("likeList", likeList);
        model.addAttribute("user", user);

        return "users/mypage_likes";
    }

    @GetMapping("/mypage/purchases")
    public String purchaseList(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        String userName = principal.getName();
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<Transaction> purchases = transactionService.getPurchasesByBuyer(user);

        model.addAttribute("user", user);
        model.addAttribute("purchases", purchases);

        return "users/mypage";
    }
    /**
     * 🛍 판매 내역 보기
     */
    @GetMapping("/mypage/sales")
    public String salesList (Principal principal, Model model){
        if (principal == null) {
            return "redirect:/login";
        }

        String userName = principal.getName();
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // ✅ TransactionService를 통해 판매 내역 조회
        List<Transaction> sales = transactionService.getSalesBySeller(user);

        model.addAttribute("user", user);
        model.addAttribute("sales", sales);

        return "mypage"; // mypage.html 내 판매내역 탭에 표시됨
    }
    // ✅ 사용자별 알림 내역 조회
    public List<Notification> getNotificationsByUser(User user) {
        return notificationRepository.findByReceiverOrderByCreatedDateDesc(user);
    }
    // 알림 전체 보기 페이지
    @GetMapping("/mypage/notifications")
    public String notificationsPage(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";

        User user = userService.getUserByUserName(principal.getName());
        List<Notification> notifications = notificationService.getUserNotifications(user);
        long unreadCount = notificationService.getUnreadCount(user);

        model.addAttribute("user", user);
        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);

        return "notifications/notifications";
    }

    // 알림 읽음 처리
    @PostMapping("/mypage/notifications/{id}/read")
    @ResponseBody
    public String markNotificationAsRead(@PathVariable Long id, Principal principal) {
        if (principal == null) return "error";

        User user = userService.getUserByUserName(principal.getName());
        notificationService.markAsRead(id, user);

        return "success";
    }

    // 모든 알림 읽음 처리
    @PostMapping("/mypage/notifications/read-all")
    @ResponseBody
    public String markAllNotificationsAsRead(Principal principal) {
        if (principal == null) return "error";

        User user = userService.getUserByUserName(principal.getName());
        notificationService.markAllAsRead(user);

        return "success";
    }

    // 알림 삭제
    @PostMapping("/mypage/notifications/{id}/delete")
    @ResponseBody
    public String deleteNotification(@PathVariable Long id, Principal principal) {
        if (principal == null) return "error";

        User user = userService.getUserByUserName(principal.getName());
        notificationService.deleteNotification(id, user);

        return "success";
    }
}