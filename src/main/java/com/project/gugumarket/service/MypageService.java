package com.project.gugumarket.service;

import com.project.gugumarket.dto.UserDto;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
public class MypageService {
    @Value("${file.upload.path}")
    private String uploadPath;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;


    public UserDto getUserInfo(String userName){
        User user  = userRepository.findByUserName(userName)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."+userName));

        UserDto dto = new UserDto();
        dto.setUserName(dto.getUserName());
        dto.setEmail(dto.getEmail());
        dto.setPhone(dto.getPhone());
        return dto;
    }

    public void updateUserInfo(String userName,UserDto userDto){
        User user=userRepository.findByUserName(userName)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."+userName));
        user.setUserName(userDto.getUserName());
        user.setEmail(userDto.getEmail());
        user.setPhone(userDto.getPhone());
        userRepository.save(user);
    }

    public boolean changePassword(String userName,String currentPassword, String newPassword){
        User user=userRepository.findByUserName(userName)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."+userName));
        if(!passwordEncoder.matches(currentPassword,user.getPassword())){
            return false;
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }
    public void updateUserProfile(UserDto userDto, MultipartFile profileImage) throws IOException {
        User user = userRepository.findByUserName(userDto.getUserName())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 파일 업로드 처리
        if (profileImage != null && !profileImage.isEmpty()) {
            // 디렉토리 없으면 생성
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();

                // 파일명 고유화
                String originalFilename = profileImage.getOriginalFilename();
                String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                String newFileName = UUID.randomUUID() + extension;

                // 저장 경로
                File saveFile = new File(uploadDir, newFileName);
                profileImage.transferTo(saveFile);

                // DB에 저장할 경로 (static 기준)
                user.setProfileImage("/uploads/" + newFileName);
            } // ✅ 새 파일이 없고 기존 이미지가 없는 경우 기본 이미지 유지
            else if (user.getProfileImage() == null || user.getProfileImage().isEmpty()) {
                user.setProfileImage("/images/default-profile.png");
            }
        }

        // 나머지 정보 업데이트
        user.setNickname(userDto.getNickname());
        user.setEmail(userDto.getEmail());
        user.setPhone(userDto.getPhone());
        user.setAddress(userDto.getAddress());
        user.setAddressDetail(userDto.getAddressDetail());
        user.setPostalCode(userDto.getPostalCode());

        userRepository.save(user);
    }
    public User getUserByUserName(String userName) {
        return userRepository.findByUserName(userName)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }
}
