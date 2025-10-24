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

/**
 * 마이페이지 관련 비즈니스 로직을 처리하는 서비스
 * 프로필 정보 조회/수정, 비밀번호 변경, 프로필 이미지 관리 등을 담당
 */
@Service
public class MypageService {

    /**
     * 파일 업로드 경로
     * application.properties에서 file.upload.path 값을 읽어옴
     * 설정이 없으면 기본값 "uploads/" 사용
     */
    @Value("${file.upload.path:uploads/}")
    private String uploadPath;

    @Autowired
    private UserRepository userRepository;  // 사용자 데이터베이스 접근

    @Autowired
    private PasswordEncoder passwordEncoder;  // 비밀번호 암호화/검증

    /**
     * 사용자 이름으로 사용자 엔티티 조회
     * @param userName 조회할 사용자 이름
     * @return User 엔티티
     * @throws IllegalArgumentException 사용자를 찾을 수 없을 때
     */
    public User getUserByUserName(String userName) {
        return userRepository.findByUserName(userName)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userName));
    }

    /**
     * 사용자 정보를 DTO로 변환하여 반환
     * 엔티티의 민감한 정보(비밀번호 등)를 제외하고 필요한 정보만 전달
     * @param userName 조회할 사용자 이름
     * @return 사용자 정보가 담긴 UserDto
     */
    public UserDto getUserInfo(String userName) {
        // 사용자 엔티티 조회
        User user = getUserByUserName(userName);

        // Entity → DTO 변환
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

    /**
     * 사용자 기본 정보 업데이트
     * 프로필 이미지와 비밀번호는 제외하고 기본 정보만 수정
     * @param userName 수정할 사용자 이름
     * @param userDto 수정할 정보가 담긴 DTO
     */
    public void updateUserInfo(String userName, UserDto userDto) {
        // 사용자 조회
        User user = getUserByUserName(userName);

        // 기본 정보만 업데이트 (프로필 이미지는 별도 메서드로 관리)
        user.setNickname(userDto.getNickname());
        user.setEmail(userDto.getEmail());
        user.setPhone(userDto.getPhone());
        user.setAddress(userDto.getAddress());
        user.setAddressDetail(userDto.getAddressDetail());
        user.setPostalCode(userDto.getPostalCode());

        // 데이터베이스에 저장
        userRepository.save(user);
        System.out.println("✅ 기본 정보 저장 완료");
    }

    /**
     * 비밀번호 변경 메서드
     * 현재 비밀번호를 확인한 후 새 비밀번호로 변경
     * @param userName 사용자 이름
     * @param currentPassword 현재 비밀번호
     * @param newPassword 새 비밀번호
     * @return true: 변경 성공, false: 현재 비밀번호 불일치
     */
    public boolean changePassword(String userName, String currentPassword, String newPassword) {
        // 사용자 조회
        User user = getUserByUserName(userName);

        // 현재 비밀번호 확인
        // 입력한 현재 비밀번호가 DB에 저장된 암호화된 비밀번호와 일치하는지 검증
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return false;  // 현재 비밀번호가 틀림
        }

        // 새 비밀번호를 암호화하여 설정
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return true;  // 변경 성공
    }

    /**
     * 프로필 이미지 업로드 처리
     * 파일을 서버에 저장하고 웹에서 접근 가능한 URL 반환
     * @param profileImage 업로드할 이미지 파일
     * @param userName 사용자 이름 (파일명 생성에 사용)
     * @return 웹에서 접근 가능한 이미지 URL (예: "/uploads/user1_1234567890.jpg")
     * @throws IOException 파일 저장 중 오류 발생 시
     */
    public String uploadProfileImage(MultipartFile profileImage, String userName) throws IOException {
        // 파일이 없거나 비어있으면 null 반환
        if (profileImage == null || profileImage.isEmpty()) {
            return null;
        }

        // 업로드 디렉토리 설정 (application.properties에서 읽어온 경로)
        String uploadDir = uploadPath;

        // 파일명 생성 (중복 방지를 위해 타임스탬프 추가)
        String originalFilename = profileImage.getOriginalFilename();
        String extension = "";  // 파일 확장자
        // 원본 파일명에서 확장자 추출 (예: .jpg, .png)
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        // 최종 파일명: "사용자명_타임스탬프.확장자" (예: user1_1234567890.jpg)
        String fileName = userName + "_" + System.currentTimeMillis() + extension;

        // 업로드 디렉토리가 없으면 생성
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            System.out.println("디렉토리 생성: " + uploadPath.toAbsolutePath());
        }

        // 파일을 디스크에 저장
        Path filePath = uploadPath.resolve(fileName);
        // 파일이 이미 존재하면 덮어쓰기
        Files.copy(profileImage.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        System.out.println("파일 저장 성공: " + filePath.toAbsolutePath());

        // 웹에서 접근 가능한 URL 반환 (정적 리소스 경로)
        return "/uploads/" + fileName;
    }

    /**
     * 프로필 이미지 삭제
     * 사용자의 프로필 이미지 URL을 null로 설정 (기본 이미지 사용)
     * 주의: 실제 파일은 삭제하지 않고 DB의 참조만 제거
     * @param userName 사용자 이름
     */
    public void deleteProfileImage(String userName) {
        User user = getUserByUserName(userName);
        user.setProfileImage(null);  // 이미지 URL 제거
        userRepository.save(user);
    }

    /**
     * 프로필 이미지 URL 업데이트
     * 이미 업로드된 이미지의 URL을 사용자 정보에 저장
     * @param userName 사용자 이름
     * @param imageUrl 저장할 이미지 URL
     */
    public void updateProfileImage(String userName, String imageUrl) {
        User user = getUserByUserName(userName);
        user.setProfileImage(imageUrl);
        userRepository.save(user);
    }

    /**
     * 전체 프로필 업데이트 (기본 정보 + 프로필 이미지)
     * 사용자 정보와 프로필 이미지를 한 번에 업데이트
     * @param userDto 수정할 사용자 기본 정보
     * @param profileImage 업로드할 프로필 이미지 (선택)
     * @throws IOException 파일 업로드 중 오류 발생 시
     */
    public void updateUserProfile(UserDto userDto, MultipartFile profileImage) throws IOException {
        // 사용자 조회
        User user = getUserByUserName(userDto.getUserName());

        // 프로필 이미지 처리 (파일이 있는 경우에만)
        if (profileImage != null && !profileImage.isEmpty()) {
            // 이미지 업로드 후 URL 받아오기
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

        // 모든 정보를 한 번에 저장
        userRepository.save(user);
    }
}