package com.project.gugumarket.controller;

import com.project.gugumarket.dto.UserDto;
import com.project.gugumarket.entity.Like;
import com.project.gugumarket.entity.Transaction;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.UserRepository;
<<<<<<< HEAD
import com.project.gugumarket.service.LikeService;
import com.project.gugumarket.service.TransactionService;
=======
>>>>>>> 99e0d3e7d634953e5cc34f25606565e61d769023
import com.project.gugumarket.service.UserService;
import com.project.gugumarket.service.MypageService;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;

@RequiredArgsConstructor
@Controller
public class MypageController {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final MypageService mypageService;
<<<<<<< HEAD
    private final LikeService likeService;
    private final TransactionService transactionService;
=======
>>>>>>> 99e0d3e7d634953e5cc34f25606565e61d769023

    // 마이페이지 조회
    @GetMapping("/mypage")
    public String mypage(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";
<<<<<<< HEAD

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

=======

        String userName = principal.getName();
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        model.addAttribute("user", user);
        return "users/mypage";
>>>>>>> 99e0d3e7d634953e5cc34f25606565e61d769023
    }

    // 프로필 수정 페이지
    @GetMapping("/mypage/edit")
    public String editForm(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";

        String userName = principal.getName();

        // MypageService를 통해 사용자 정보 조회
        User user = mypageService.getUserByUserName(userName);
        UserDto userDto = mypageService.getUserInfo(userName);

        model.addAttribute("user", user);
        model.addAttribute("userDto", userDto);

        // edit.html의 실제 위치에 따라 수정
        return "users/edit";
    }

    // 프로필 수정 처리
    @PostMapping("/users/edit")
    public String editProfile(
            @Valid @ModelAttribute UserDto userDto,
            BindingResult bindingResult,
            @RequestParam(required = false) String currentPassword,
            @RequestParam(required = false) String newPassword,
            @RequestParam(required = false) String confirmPassword,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
            @RequestParam(value = "deleteProfileImage", required = false) String deleteProfileImage,
<<<<<<< HEAD
            @RequestParam(value = "currentProfileImage", required = false) String currentProfileImage,
=======
>>>>>>> 99e0d3e7d634953e5cc34f25606565e61d769023
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (principal == null) return "redirect:/login";

        String userName = principal.getName();
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

<<<<<<< HEAD
=======
        // 프로필 이미지 삭제 처리
>>>>>>> 99e0d3e7d634953e5cc34f25606565e61d769023
        if ("true".equals(deleteProfileImage)) {
            System.out.println("프로필 이미지 삭제 요청");
            user.setProfileImage(null);
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

=======
        // 프로필 이미지 업로드 처리
        else if (profileImage != null && !profileImage.isEmpty()) {
            System.out.println("프로필 이미지 업로드 시작");
            System.out.println("   - 파일명: " + profileImage.getOriginalFilename());
            System.out.println("   - 파일 크기: " + profileImage.getSize() + " bytes");
            System.out.println("   - Content-Type: " + profileImage.getContentType());

            try {
                // 파일 크기 체크 (5MB)
                if (profileImage.getSize() > 5 * 1024 * 1024) {
                    System.err.println("파일 크기 초과: " + profileImage.getSize());
                    model.addAttribute("error", "파일 크기는 5MB 이하여야 합니다.");
                    model.addAttribute("user", user);
                    model.addAttribute("userDto", userDto);
                    return "edit";
                }

                // 파일 형식 체크
>>>>>>> 99e0d3e7d634953e5cc34f25606565e61d769023
                String contentType = profileImage.getContentType();
                if (contentType == null ||
                        (!contentType.equals("image/jpeg") &&
                                !contentType.equals("image/jpg") &&
                                !contentType.equals("image/png") &&
                                !contentType.equals("image/gif"))) {
                    System.err.println("지원하지 않는 파일 형식: " + contentType);
                    model.addAttribute("error", "JPG, PNG, GIF 형식의 이미지만 업로드 가능합니다.");
                    model.addAttribute("user", user);
                    model.addAttribute("userDto", userDto);
<<<<<<< HEAD
                    return "users/edit";
                }

=======
                    return "edit";
                }

                // 파일 저장
>>>>>>> 99e0d3e7d634953e5cc34f25606565e61d769023
                String fileName = saveProfileImage(profileImage, userName);
                user.setProfileImage(fileName);
                System.out.println("프로필 이미지 업로드 완료: " + fileName);

            } catch (Exception e) {
<<<<<<< HEAD
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
=======
                System.err.println("프로필 이미지 업로드 중 오류: " + e.getMessage());
                e.printStackTrace();
                model.addAttribute("error", "프로필 이미지 업로드 중 오류가 발생했습니다: " + e.getMessage());
                model.addAttribute("user", user);
                model.addAttribute("userDto", userDto);
                return "edit";
            }
        } else {
            System.out.println("프로필 이미지 변경 없음");
>>>>>>> 99e0d3e7d634953e5cc34f25606565e61d769023
        }

        // 비밀번호 변경 요청이 있는 경우
        boolean passwordChangeRequested =
                (currentPassword != null && !currentPassword.isEmpty()) ||
                        (newPassword != null && !newPassword.isEmpty()) ||
                        (confirmPassword != null && !confirmPassword.isEmpty());

        if (passwordChangeRequested) {
<<<<<<< HEAD
=======
            // 현재 비밀번호 확인
>>>>>>> 99e0d3e7d634953e5cc34f25606565e61d769023
            if (currentPassword == null || currentPassword.isEmpty()) {
                model.addAttribute("error", "현재 비밀번호를 입력해주세요.");
                model.addAttribute("user", user);
                model.addAttribute("userDto", userDto);
<<<<<<< HEAD
                return "users/edit";
=======
                return "edit";
>>>>>>> 99e0d3e7d634953e5cc34f25606565e61d769023
            }

            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                model.addAttribute("error", "현재 비밀번호가 일치하지 않습니다.");
                model.addAttribute("user", user);
                model.addAttribute("userDto", userDto);
<<<<<<< HEAD
                return "users/edit";
            }

=======
                return "edit";
            }

            // 새 비밀번호 확인
>>>>>>> 99e0d3e7d634953e5cc34f25606565e61d769023
            if (newPassword == null || newPassword.isEmpty()) {
                model.addAttribute("error", "새 비밀번호를 입력해주세요.");
                model.addAttribute("user", user);
                model.addAttribute("userDto", userDto);
<<<<<<< HEAD
                return "users/edit";
=======
                return "edit";
>>>>>>> 99e0d3e7d634953e5cc34f25606565e61d769023
            }

            if (!newPassword.equals(confirmPassword)) {
                model.addAttribute("error", "새 비밀번호가 일치하지 않습니다.");
                model.addAttribute("user", user);
                model.addAttribute("userDto", userDto);
<<<<<<< HEAD
                return "users/edit";
            }

            user.setPassword(passwordEncoder.encode(newPassword));
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            return "users/edit";
        }

=======
                return "edit";
            }

            // 비밀번호 변경
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        // 유효성 검증 실패 시
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            return "edit";
        }

        // 기본 정보 업데이트
>>>>>>> 99e0d3e7d634953e5cc34f25606565e61d769023
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
<<<<<<< HEAD
    private String saveProfileImage(MultipartFile profileImage, String userName) throws IOException {
        try {
            String uploadDir = "uploads/";

            String originalFilename = profileImage.getOriginalFilename();
=======
    private String saveProfileImage(MultipartFile file, String userName) throws IOException {
        try {
            // 업로드 디렉토리 설정 (프로젝트 루트 기준)
            String uploadDir = "uploads/profile/";

            // 파일명 생성 (중복 방지)
            String originalFilename = file.getOriginalFilename();
>>>>>>> 99e0d3e7d634953e5cc34f25606565e61d769023
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = userName + "_" + System.currentTimeMillis() + extension;

<<<<<<< HEAD
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
    public String purchaseList(Principal principal, Model model) {
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
=======
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

        } catch (IOException e) {
            System.err.println("파일 저장 실패: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
>>>>>>> 99e0d3e7d634953e5cc34f25606565e61d769023
}