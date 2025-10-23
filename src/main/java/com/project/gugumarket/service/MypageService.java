package com.project.gugumarket.service;

import com.project.gugumarket.dto.UserDto;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class MypageService {

    @Value("${file.upload.path:uploads/}")
    private String uploadPath;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 사용자 정보 조회
    public User getUserByUserName(String userName) {
        return userRepository.findByUserName(userName)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userName));
    }

    // 사용자 정보를 DTO로 변환
    public UserDto getUserInfo(String userName) {
        User user = getUserByUserName(userName);

        UserDto dto = new UserDto();
        dto.setUserName(user.getUserName());
        dto.setEmail(user.getEmail());
        dto.setNickname(user.getNickname());
        dto.setPhone(user.getPhone());
        dto.setAddress(user.getAddress());
        dto.setAddressDetail(user.getAddressDetail());
        dto.setPostalCode(user.getPostalCode());

        return dto;
    }

    // 사용자 기본 정보 업데이트
    public void updateUserInfo(String userName, UserDto userDto) {
        User user = getUserByUserName(userName);

        user.setNickname(userDto.getNickname());
        user.setEmail(userDto.getEmail());
        user.setPhone(userDto.getPhone());
        user.setAddress(userDto.getAddress());
        user.setAddressDetail(userDto.getAddressDetail());
        user.setPostalCode(userDto.getPostalCode());

        userRepository.save(user);
    }

    // 비밀번호 변경
    public boolean changePassword(String userName, String currentPassword, String newPassword) {
        User user = getUserByUserName(userName);

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return false;
        }

        // 새 비밀번호 설정
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return true;
    }

    // 프로필 이미지 업로드
    public String uploadProfileImage(MultipartFile profileImage, String userName) throws IOException {
        if (profileImage == null || profileImage.isEmpty()) {
            return null;
        }

        // 업로드 디렉토리 설정
        String uploadDir = uploadPath + "profile/";

        // 파일명 생성 (중복 방지)
        String originalFilename = profileImage.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = userName + "_" + System.currentTimeMillis() + extension;

        // 디렉토리 생성
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            System.out.println("디렉토리 생성: " + uploadPath.toAbsolutePath());
        }

        // 파일 저장
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(profileImage.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        System.out.println("파일 저장 성공: " + filePath.toAbsolutePath());

        // 웹에서 접근 가능한 URL 반환
        return "/uploads/profile/" + fileName;
    }

    // 프로필 이미지 삭제
    public void deleteProfileImage(String userName) {
        User user = getUserByUserName(userName);
        user.setProfileImage(null);
        userRepository.save(user);
    }

    // 프로필 이미지 업데이트
    public void updateProfileImage(String userName, String imageUrl) {
        User user = getUserByUserName(userName);
        user.setProfileImage(imageUrl);
        userRepository.save(user);
    }

    // 전체 프로필 업데이트 (기본 정보 + 프로필 이미지)
    public void updateUserProfile(UserDto userDto, MultipartFile profileImage) throws IOException {
        User user = getUserByUserName(userDto.getUserName());

        // 프로필 이미지 처리
        if (profileImage != null && !profileImage.isEmpty()) {
            String imageUrl = uploadProfileImage(profileImage, user.getUserName());
            user.setProfileImage(imageUrl);
        }

        // 기본 정보 업데이트
        user.setNickname(userDto.getNickname());
        user.setEmail(userDto.getEmail());
        user.setPhone(userDto.getPhone());
        user.setAddress(userDto.getAddress());
        user.setAddressDetail(userDto.getAddressDetail());
        user.setPostalCode(userDto.getPostalCode());

        userRepository.save(user);
    }
}