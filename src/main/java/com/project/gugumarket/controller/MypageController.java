package com.project.gugumarket.controller;

import com.project.gugumarket.dto.UserDto;
import com.project.gugumarket.entity.Like;
import com.project.gugumarket.entity.Notification;
import com.project.gugumarket.entity.Transaction;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.NotificationRepository;
import com.project.gugumarket.repository.UserRepository;
import com.project.gugumarket.service.*;
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
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping("/mypage")
    public String mypage(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";

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
        UserDto userDto = mypageService.getUserInfo(userName);

        model.addAttribute("user", user);
        model.addAttribute("userDto", userDto);

        return "users/edit";
    }

    // ✅ 단순화된 프로필 수정 처리
    @PostMapping("/users/edit")
    public String editProfile(
            @Valid @ModelAttribute UserDto userDto,
            BindingResult bindingResult,
            @RequestParam(required = false) String currentPassword,
            @RequestParam(required = false) String newPassword,
            @RequestParam(required = false) String confirmPassword,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
            @RequestParam(value = "deleteProfileImage", required = false) String deleteProfileImage,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (principal == null) return "redirect:/login";

        String userName = principal.getName();
        User user = mypageService.getUserByUserName(userName);

        // 유효성 검증 실패 시 먼저 처리
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            return "users/edit";
        }

        try {
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
            user.setNickname(userDto.getNickname());
            user.setEmail(userDto.getEmail());
            user.setPhone(userDto.getPhone());
            user.setAddress(userDto.getAddress());
            user.setAddressDetail(userDto.getAddressDetail());
            user.setPostalCode(userDto.getPostalCode());

            // 4️⃣ 한 번에 모든 정보 저장
            userRepository.save(user);
            System.out.println("✅ 모든 정보 저장 완료");
            System.out.println("   - 프로필 이미지: " + user.getProfileImage());
            System.out.println("   - 닉네임: " + user.getNickname());
            System.out.println("   - 이메일: " + user.getEmail());

            redirectAttributes.addFlashAttribute("successMessage", "회원정보가 수정되었습니다.");
            return "redirect:/mypage";

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