package com.project.gugumarket.service;

import com.project.gugumarket.DataNotFoundException;
import com.project.gugumarket.dto.UserDto;
import com.project.gugumarket.entity.PasswordResetToken;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.PasswordResetTokenRepository;
import com.project.gugumarket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;

    public User getUser(String userName) {
        Optional<User> siteUser = this.userRepository.findByUserName(userName);

        if(siteUser.isPresent()) {
            User user = siteUser.get();
            return user;
        }
        else {
            throw new DataNotFoundException("siteuser not found");
        }
    }

    // 아이디 중복 체크
    public boolean isUserNameDuplicate(String userName) {
        return userRepository.existsByUserName(userName);
    }

    @Transactional
    public User create(UserDto userDto) {
        // 1. 중복 사용자 체크
        if (userRepository.existsByUserName(userDto.getUserName())) {
            throw new IllegalArgumentException("이미 존재하는 사용자 ID입니다.");
        }

        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 2. 비밀번호 검증 (선택사항)
        validatePassword(userDto.getPassword());

        String encodedPassword=passwordEncoder.encode(userDto.getPassword());
        System.out.println("원본 비밀번호: "+userDto.getPassword());
        System.out.println("암호화된 비밀번호: "+encodedPassword);

        // 3. 새 사용자 객체 생성 및 설정
        User user = User.builder()
                .userName(userDto.getUserName())
                .password(encodedPassword)
                .email(userDto.getEmail())
                .nickname(userDto.getNickname())
                .phone(userDto.getPhone())
                .address(userDto.getAddress())
                .addressDetail(userDto.getAddressDetail())
                .postalCode(userDto.getPostalCode())
                .createdDate(LocalDateTime.now())
                .isActive(true)
                .role("USER")
                .build();

        // 4. 저장 및 반환
        User savedUser=userRepository.save(user);
        System.out.println("DB 저장 완료 - userId: " + savedUser.getUserId());
        return savedUser;
    }

    // 비밀번호 유효성 검증 (선택사항)
    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("비밀번호는 최소 8자 이상이어야 합니다.");
        }

        // 추가 검증 로직 (영문, 숫자, 특수문자 포함 등)
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*\\d.*");

        if (!hasLetter || !hasDigit) {
            throw new IllegalArgumentException("비밀번호는 영문과 숫자를 포함해야 합니다.");
        }
    }

    //내 정보 조회
    public UserDto getUserInfo(String userName) {
        User user=userRepository.findByUserName(userName)
                .orElseThrow(()->new IllegalArgumentException("사용자를 찾을 수 없습니다:"+userName));
        UserDto dto=new UserDto();
        dto.setUserName(user.getUserName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        return dto;
    }

    //내 정보 수정
    public void updateUserInfo(String userName,UserDto userDto) {
        User user=userRepository.findByUserName(userName)
                .orElseThrow(()->new IllegalArgumentException("사용자를 찾을 수 없습니다:"+userName));
        user.setUserName(userDto.getUserName());
        user.setEmail(userDto.getEmail());
        user.setPhone(userDto.getPhone());
        userRepository.save(user);
    }

    //비밀번호 변경
    public boolean changePassword(String userName, String currentpassword,String newpassword) {
        User user=userRepository.findByUserName(userName)
                .orElseThrow(()-> new IllegalArgumentException("사용자를 찾을 수 없습니다:"+userName));
        if(!passwordEncoder.matches(newpassword,user.getPassword())) {
            return false;
        }
        user.setPassword(passwordEncoder.encode(newpassword));
        userRepository.save(user);
        return true;
    }

    // 🔥 Principal에서 현재 사용자 가져오기
    public User getCurrentUser(Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        String username = principal.getName();
        return userRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
    }

    public User getUserByUserName(String username) {
        return userRepository.findByUserName(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));
    }

    /**
     * 이메일로 아이디 찾기
     */
    public String findUserNameByEmail(String email, String nickname) {
        User user = userRepository.findByEmailAndNickname(email, nickname)
                .orElseThrow(() -> new IllegalArgumentException("입력하신 정보와 일치하는 회원을 찾을 수 없습니다."));

        // 아이디 마스킹 처리 (예: user1234 -> user****)
        return maskUserName(user.getUserName());
    }

    /**
     * 전화번호로 아이디 찾기
     */
    public String findUserNameByPhone(String phone, String nickname) {
        User user = userRepository.findByPhoneAndNickname(phone, nickname)
                .orElseThrow(() -> new IllegalArgumentException("입력하신 정보와 일치하는 회원을 찾을 수 없습니다."));

        return maskUserName(user.getUserName());
    }

    /**
     * 가입일 조회
     */
    public LocalDateTime getJoinDate(String email, String nickname) {
        User user = userRepository.findByEmailAndNickname(email, nickname)
                .orElse(null);

        return user != null ? user.getCreatedDate() : null;
    }

    /**
     * 아이디 마스킹 처리
     */
    private String maskUserName(String userName) {
        if (userName.length() <= 4) {
            return userName.charAt(0) + "***";
        }

        int visibleChars = userName.length() / 3;
        String visible = userName.substring(0, visibleChars);
        return visible + "****";
    }

    /**
     * 비밀번호 재설정 토큰 생성 및 이메일 발송
     */
    @Transactional
    public void requestPasswordReset(String userName, String email) {
        System.out.println("========================================");
        System.out.println("=== requestPasswordReset 시작 ===");
        System.out.println("userName: " + userName);
        System.out.println("email: " + email);
        System.out.println("========================================");

        try {
            // 사용자 확인
            User user = userRepository.findByUserNameAndEmail(userName, email)
                    .orElseThrow(() -> new IllegalArgumentException("입력하신 정보와 일치하는 회원을 찾을 수 없습니다."));

            System.out.println("✅ 사용자 찾기 성공: " + user.getUserName());

            // 기존 토큰 무효화
            tokenRepository.findByUserAndUsedFalseAndExpiryDateAfter(user, LocalDateTime.now())
                    .ifPresent(token -> {
                        token.setUsed(true);
                        tokenRepository.save(token);
                        System.out.println("기존 토큰 무효화 완료");
                    });

            // 새 토큰 생성
            String token = generateToken();
            LocalDateTime expiryDate = LocalDateTime.now().plusHours(24);

            System.out.println("✅ 토큰 생성 완료: " + token);

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(token)
                    .user(user)
                    .expiryDate(expiryDate)
                    .createdDate(LocalDateTime.now())
                    .used(false)
                    .build();

            tokenRepository.save(resetToken);
            System.out.println("✅ 토큰 DB 저장 완료");

            // 이메일 발송
            String resetLink = "http://localhost:8080/users/reset-password?token=" + token;
            System.out.println("이메일 발송 시도 중...");
            System.out.println("수신자: " + email);
            System.out.println("링크: " + resetLink);

            emailService.sendPasswordResetEmail(email, resetLink);

            System.out.println("✅✅✅ 이메일 발송 성공! ✅✅✅");
            System.out.println("========================================");

        } catch (Exception e) {
            System.out.println("❌❌❌ 오류 발생! ❌❌❌");
            System.out.println("오류 메시지: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * 토큰 검증 및 비밀번호 재설정
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        // 토큰 조회
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));

        // 토큰 검증
        if (resetToken.isUsed()) {
            throw new IllegalArgumentException("이미 사용된 토큰입니다.");
        }

        if (resetToken.isExpired()) {
            throw new IllegalArgumentException("만료된 토큰입니다.");
        }

        // 비밀번호 변경
        User user = resetToken.getUser();
        validatePassword(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // 토큰 사용 처리
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }

    /**
     * 토큰 생성 (UUID 사용)
     */
    private String generateToken() {
        return java.util.UUID.randomUUID().toString();
    }

    /**
     * 토큰 유효성 검증
     */
    public boolean isTokenValid(String token) {
        return tokenRepository.findByToken(token)
                .map(t -> !t.isUsed() && !t.isExpired())
                .orElse(false);
    }
}