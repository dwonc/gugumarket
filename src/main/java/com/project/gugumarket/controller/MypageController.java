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

/**
 * 마이페이지 관련 기능을 처리하는 컨트롤러
 * 프로필 조회/수정, 찜 목록, 구매/판매 내역, 알림 관리 등을 담당
 */
@RequiredArgsConstructor  // final 필드에 대한 생성자 자동 생성
@Controller
public class MypageController {

    private final PasswordEncoder passwordEncoder;  // 비밀번호 암호화/검증
    private final UserRepository userRepository;  // 사용자 데이터베이스 접근
    private final MypageService mypageService;  // 마이페이지 관련 비즈니스 로직
    private final LikeService likeService;  // 찜 기능 처리
    private final TransactionService transactionService;  // 거래 내역 처리
    private final NotificationRepository notificationRepository;  // 알림 데이터베이스 접근
    private final NotificationService notificationService;  // 알림 관련 비즈니스 로직
    private final UserService userService;  // 사용자 관련 비즈니스 로직

    /**
     * 마이페이지 메인 화면 표시
     * GET /mypage
     * 사용자 정보, 찜 목록, 구매/판매 내역, 최근 알림을 한 번에 보여줌
     */
    @GetMapping("/mypage")
    public String mypage(Principal principal, Model model) {
        // 로그인하지 않은 경우 로그인 페이지로 리다이렉트
        if (principal == null) return "redirect:/login";

        // 현재 로그인한 사용자 이름 가져오기
        String userName = principal.getName();
        // 사용자 정보 조회
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 디버깅용 로그 출력
        System.out.println("=== 마이페이지 로드 ===");
        System.out.println("사용자: " + userName);
        System.out.println("프로필 이미지: " + user.getProfileImage());
        System.out.println("프로필 이미지 (기본값 포함): " + user.getProfileImageOrDefault());

        // 사용자의 찜 목록 조회
        List<Like> likes = likeService.getUserLikes(user);
        // 구매 내역 조회 (내가 구매한 상품들)
        List<Transaction> purchases = transactionService.findByBuyer(user);
        // 판매 내역 조회 (내가 판매한 상품들)
        List<Transaction> sales = transactionService.findBySeller(user);
        // 최근 알림 5개 조회
        List<Notification> recentNotifications = notificationService.getRecentNotifications(user,5);
        // 읽지 않은 알림 개수
        long unreadCount = notificationService.getUnreadCount(user);

        // 모델에 데이터 추가하여 뷰로 전달
        model.addAttribute("recentNotifications", recentNotifications);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("likes", likes);
        model.addAttribute("user", user);
        model.addAttribute("purchases", purchases);
        model.addAttribute("sales", sales);

        return "users/mypage";  // users/mypage.html 템플릿 반환
    }

    /**
     * 프로필 수정 폼 페이지 표시
     * GET /mypage/edit
     * 현재 사용자 정보를 폼에 미리 채워서 보여줌
     */
    @GetMapping("/mypage/edit")
    public String editForm(Principal principal, Model model) {
        // 로그인 확인
        if (principal == null) return "redirect:/login";

        String userName = principal.getName();
        User user = mypageService.getUserByUserName(userName);

        // User 엔티티의 정보를 UserUpdateDto로 복사
        // DTO를 사용하여 필요한 필드만 폼에 표시
        UserUpdateDto userDto = new UserUpdateDto();
        userDto.setNickname(user.getNickname());
        userDto.setEmail(user.getEmail());
        userDto.setPhone(user.getPhone());
        userDto.setPostalCode(user.getPostalCode());
        userDto.setAddress(user.getAddress());
        userDto.setAddressDetail(user.getAddressDetail());

        model.addAttribute("user", user);
        model.addAttribute("userDto", userDto);

        return "users/edit";  // users/edit.html 템플릿 반환
    }

    /**
     * 프로필 수정 처리 메서드
     * POST /users/edit
     * 기본 정보, 프로필 이미지, 비밀번호 변경을 한 번에 처리
     */
    @PostMapping("/users/edit")
    public String editProfile(
            @Valid @ModelAttribute UserUpdateDto userDto,  // 수정할 사용자 정보 (유효성 검증)
            BindingResult bindingResult,  // 유효성 검증 결과
            @RequestParam(required = false) String currentPassword,  // 현재 비밀번호 (선택)
            @RequestParam(required = false) String newPassword,  // 새 비밀번호 (선택)
            @RequestParam(required = false) String confirmPassword,  // 비밀번호 확인 (선택)
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,  // 프로필 이미지 파일
            @RequestParam(value = "deleteProfileImage", required = false) String deleteProfileImage,  // 이미지 삭제 플래그
            Principal principal,  // 현재 로그인 사용자
            Model model,
            RedirectAttributes redirectAttributes) {  // 리다이렉트 시 메시지 전달

        // 디버깅 로그 시작
        System.out.println("\n========================================");
        System.out.println("🚀 프로필 수정 요청 시작!");
        System.out.println("========================================");

        // 로그인 확인
        if (principal == null) {
            System.out.println("❌ Principal이 null입니다!");
            return "redirect:/login";
        }

        String userName = principal.getName();
        System.out.println("✅ 로그인 사용자: " + userName);

        // 사용자 정보 조회
        User user = mypageService.getUserByUserName(userName);
        System.out.println("✅ 사용자 정보 조회 완료: " + user.getNickname());

        // 받은 데이터 로그 출력
        System.out.println("\n📥 받은 데이터:");
        System.out.println("  - 닉네임: " + userDto.getNickname());
        System.out.println("  - 이메일: " + userDto.getEmail());
        System.out.println("  - 전화번호: " + userDto.getPhone());
        System.out.println("  - 주소: " + userDto.getAddress());
        System.out.println("  - 상세주소: " + userDto.getAddressDetail());
        System.out.println("  - 우편번호: " + userDto.getPostalCode());

        System.out.println("\n🔍 유효성 검사:");
        System.out.println("  - 에러 있음: " + bindingResult.hasErrors());

        // 유효성 검증 실패 시 에러와 함께 폼으로 돌아감
        if (bindingResult.hasErrors()) {
            System.out.println("❌ 유효성 검사 실패!");
            // 모든 에러 메시지 출력
            bindingResult.getAllErrors().forEach(error -> {
                System.out.println("    * " + error.getDefaultMessage());
            });
            model.addAttribute("user", user);
            model.addAttribute("userDto", userDto);
            return "users/edit";
        }

        System.out.println("✅ 유효성 검사 통과!");

        try {
            System.out.println("\n🔄 데이터 업데이트 시작...");

            // 1️⃣ 프로필 이미지 처리
            if ("true".equals(deleteProfileImage)) {
                // 프로필 이미지 삭제 요청
                System.out.println("🗑️ 프로필 이미지 삭제");
                user.setProfileImage(null);
            } else if (profileImage != null && !profileImage.isEmpty()) {
                // 새 프로필 이미지 업로드
                System.out.println("📤 프로필 이미지 업로드 시작");

                // 파일 크기 체크 (최대 5MB)
                if (profileImage.getSize() > 5 * 1024 * 1024) {
                    model.addAttribute("error", "파일 크기는 5MB 이하여야 합니다.");
                    model.addAttribute("user", user);
                    model.addAttribute("userDto", userDto);
                    return "users/edit";
                }

                // 파일 형식 체크 (이미지만 허용)
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

                // MypageService를 통해 파일 업로드 처리 (서버 저장 또는 클라우드 업로드)
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
                // 현재 비밀번호가 입력되지 않은 경우
                if (currentPassword == null || currentPassword.isEmpty()) {
                    model.addAttribute("error", "현재 비밀번호를 입력해주세요.");
                    model.addAttribute("user", user);
                    model.addAttribute("userDto", userDto);
                    return "users/edit";
                }

                // 현재 비밀번호가 일치하지 않는 경우
                if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                    model.addAttribute("error", "현재 비밀번호가 일치하지 않습니다.");
                    model.addAttribute("user", user);
                    model.addAttribute("userDto", userDto);
                    return "users/edit";
                }

                // 새 비밀번호가 입력되지 않은 경우
                if (newPassword == null || newPassword.isEmpty()) {
                    model.addAttribute("error", "새 비밀번호를 입력해주세요.");
                    model.addAttribute("user", user);
                    model.addAttribute("userDto", userDto);
                    return "users/edit";
                }

                // 새 비밀번호와 확인 비밀번호가 일치하지 않는 경우
                if (!newPassword.equals(confirmPassword)) {
                    model.addAttribute("error", "새 비밀번호가 일치하지 않습니다.");
                    model.addAttribute("user", user);
                    model.addAttribute("userDto", userDto);
                    return "users/edit";
                }

                // 새 비밀번호를 암호화하여 설정
                user.setPassword(passwordEncoder.encode(newPassword));
                System.out.println("🔐 비밀번호 변경 완료");
            }

            // 3️⃣ 기본 정보 업데이트
            System.out.println("\n📝 사용자 정보 업데이트 중...");
            System.out.println("  - 기존 닉네임: " + user.getNickname() + " → 새 닉네임: " + userDto.getNickname());
            System.out.println("  - 기존 이메일: " + user.getEmail() + " → 새 이메일: " + userDto.getEmail());

            // DTO의 값을 엔티티에 설정
            user.setNickname(userDto.getNickname());
            user.setEmail(userDto.getEmail());
            user.setPhone(userDto.getPhone());
            user.setAddress(userDto.getAddress());
            user.setAddressDetail(userDto.getAddressDetail());
            user.setPostalCode(userDto.getPostalCode());

            // 4️⃣ 데이터베이스에 저장
            // 프로필 이미지, 비밀번호, 기본 정보를 한 번에 저장
            System.out.println("\n💾 데이터베이스에 저장 중...");
            User savedUser = userRepository.save(user);

            // 저장 완료 로그
            System.out.println("✅ 모든 정보 저장 완료!");
            System.out.println("   - 저장된 닉네임: " + savedUser.getNickname());
            System.out.println("   - 저장된 이메일: " + savedUser.getEmail());
            System.out.println("   - 저장된 프로필 이미지: " + savedUser.getProfileImage());
            System.out.println("========================================\n");

            // 성공 메시지와 함께 마이페이지로 리다이렉트
            redirectAttributes.addFlashAttribute("successMessage", "회원정보가 수정되었습니다.");
            return "redirect:/mypage";

        } catch (IOException e) {
            // 파일 업로드 중 오류 처리
            System.err.println("❌ 파일 처리 중 오류: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "프로필 이미지 업로드 중 오류가 발생했습니다.");
            model.addAttribute("user", user);
            model.addAttribute("userDto", userDto);
            return "users/edit";
        }
    }

    /**
     * 찜 목록 페이지 표시
     * GET /mypage/likes
     * 사용자가 찜한 상품 목록을 보여줌
     */
    @GetMapping("/mypage/likes")
    public String likeList(Principal principal, Model model) {
        // 로그인 확인
        if (principal == null) {
            return "redirect:/login";
        }

        String userName = principal.getName();
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 사용자의 찜 목록 조회
        List<Like> likeList = likeService.getUserLikes(user);
        model.addAttribute("likeList", likeList);
        model.addAttribute("user", user);

        return "users/mypage_likes";  // 찜 목록 전용 페이지
    }

    /**
     * 구매 내역 페이지 표시
     * GET /mypage/purchases
     * 사용자가 구매한 상품 목록을 보여줌
     */
    @GetMapping("/mypage/purchases")
    public String purchaseList(Principal principal, Model model) {
        // 로그인 확인
        if (principal == null) {
            return "redirect:/login";
        }

        String userName = principal.getName();
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 구매 내역 조회 (내가 구매자인 거래들)
        List<Transaction> purchases = transactionService.getPurchasesByBuyer(user);

        model.addAttribute("user", user);
        model.addAttribute("purchases", purchases);

        return "users/mypage";  // 마이페이지의 구매내역 탭에 표시
    }

    /**
     * 판매 내역 페이지 표시
     * GET /mypage/sales
     * 사용자가 판매한 상품 목록을 보여줌
     */
    @GetMapping("/mypage/sales")
    public String salesList (Principal principal, Model model){
        // 로그인 확인
        if (principal == null) {
            return "redirect:/login";
        }

        String userName = principal.getName();
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 판매 내역 조회 (내가 판매자인 거래들)
        List<Transaction> sales = transactionService.getSalesBySeller(user);

        model.addAttribute("user", user);
        model.addAttribute("sales", sales);

        return "mypage";  // mypage.html 내 판매내역 탭에 표시
    }

    /**
     * 사용자별 알림 내역 조회 메서드
     * 특정 사용자가 받은 모든 알림을 최신순으로 반환
     * @param user 조회할 사용자
     * @return 알림 목록 (최신순 정렬)
     */
    public List<Notification> getNotificationsByUser(User user) {
        return notificationRepository.findByReceiverOrderByCreatedDateDesc(user);
    }

    /**
     * 알림 전체 보기 페이지
     * GET /mypage/notifications
     * 사용자의 모든 알림을 보여줌
     */
    @GetMapping("/mypage/notifications")
    public String notificationsPage(Principal principal, Model model) {
        // 로그인 확인
        if (principal == null) return "redirect:/login";

        // 사용자 정보 조회
        User user = userService.getUserByUserName(principal.getName());
        // 모든 알림 조회
        List<Notification> notifications = notificationService.getUserNotifications(user);
        // 읽지 않은 알림 개수
        long unreadCount = notificationService.getUnreadCount(user);

        model.addAttribute("user", user);
        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);

        return "notifications/notifications";  // 알림 전용 페이지
    }

    /**
     * 개별 알림 읽음 처리
     * POST /mypage/notifications/{id}/read
     * AJAX 요청으로 특정 알림을 읽음 상태로 변경
     */
    @PostMapping("/mypage/notifications/{id}/read")
    @ResponseBody  // JSON 응답
    public String markNotificationAsRead(@PathVariable Long id, Principal principal) {
        // 로그인 확인
        if (principal == null) return "error";

        User user = userService.getUserByUserName(principal.getName());
        // 알림을 읽음 상태로 변경
        notificationService.markAsRead(id, user);

        return "success";
    }

    /**
     * 모든 알림 읽음 처리
     * POST /mypage/notifications/read-all
     * AJAX 요청으로 모든 알림을 읽음 상태로 변경
     */
    @PostMapping("/mypage/notifications/read-all")
    @ResponseBody  // JSON 응답
    public String markAllNotificationsAsRead(Principal principal) {
        // 로그인 확인
        if (principal == null) return "error";

        User user = userService.getUserByUserName(principal.getName());
        // 모든 알림을 읽음 상태로 변경
        notificationService.markAllAsRead(user);

        return "success";
    }

    /**
     * 알림 삭제
     * POST /mypage/notifications/{id}/delete
     * AJAX 요청으로 특정 알림을 삭제
     */
    @PostMapping("/mypage/notifications/{id}/delete")
    @ResponseBody  // JSON 응답
    public String deleteNotification(@PathVariable Long id, Principal principal) {
        // 로그인 확인
        if (principal == null) return "error";

        User user = userService.getUserByUserName(principal.getName());
        // 알림 삭제
        notificationService.deleteNotification(id, user);

        return "success";
    }
}