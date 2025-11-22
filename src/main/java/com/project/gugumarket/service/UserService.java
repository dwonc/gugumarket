package com.project.gugumarket.service;  // ✅ service 패키지

import com.project.gugumarket.DataNotFoundException;
import com.project.gugumarket.dto.UserDto;
import com.project.gugumarket.dto.FindUsernameRequest;
import com.project.gugumarket.dto.FindUsernameResponse;
import com.project.gugumarket.dto.VerifyEmailRequest;
import com.project.gugumarket.dto.VerifyEmailResponse;
import com.project.gugumarket.dto.ResetPasswordRequest;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 사용자 관련 비즈니스 로직을 처리하는 서비스 클래스
 * 회원가입, 정보 조회/수정, 비밀번호 변경 등의 기능을 담당
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {

    @Autowired
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    // 🔥 비밀번호 재설정 토큰 저장소 (실제 프로덕션에서는 Redis 사용 권장)
    private final Map<String, String> resetTokenStore = new HashMap<>();

    /**
     * 사용자 이름으로 사용자 정보 조회
     * @param userName 조회할 사용자 이름
     * @return User 엔티티
     * @throws DataNotFoundException 사용자를 찾을 수 없을 때
     */
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

    /**
     * 아이디 중복 체크 메서드
     * 회원가입 시 아이디가 이미 사용 중인지 확인
     * @param userName 확인할 사용자 이름
     * @return true: 중복됨, false: 사용 가능
     */
    public boolean isUserNameDuplicate(String userName) {
        return userRepository.existsByUserName(userName);
    }

    /**
     * 새로운 사용자 생성 (회원가입)
     * @param userDto 사용자 정보가 담긴 DTO
     * @return 저장된 User 엔티티
     * @throws IllegalArgumentException 중복된 사용자 또는 유효하지 않은 정보
     */
    @Transactional
    public User create(UserDto userDto) {
        // 1. 중복 사용자 체크
        if (userRepository.existsByUserName(userDto.getUserName())) {
            throw new IllegalArgumentException("이미 존재하는 사용자 ID입니다.");
        }

        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 2. 비밀번호 유효성 검증
        validatePassword(userDto.getPassword());

        // 비밀번호를 BCrypt로 암호화
        String encodedPassword=passwordEncoder.encode(userDto.getPassword());
        System.out.println("원본 비밀번호: "+userDto.getPassword());
        System.out.println("암호화된 비밀번호: "+encodedPassword);

        // 3. 새 사용자 객체 생성 및 설정 (Builder 패턴 사용)
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

        // 4. 데이터베이스에 저장 및 반환
        User savedUser=userRepository.save(user);
        System.out.println("DB 저장 완료 - userId: " + savedUser.getUserId());
        return savedUser;
    }

    /**
     * 비밀번호 유효성 검증 메서드
     * 비밀번호가 보안 규칙을 만족하는지 확인
     * @param password 검증할 비밀번호
     * @throws IllegalArgumentException 비밀번호가 조건을 만족하지 않을 때
     */
    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("비밀번호는 최소 8자 이상이어야 합니다.");
        }

        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*\\d.*");

        if (!hasLetter || !hasDigit) {
            throw new IllegalArgumentException("비밀번호는 영문과 숫자를 포함해야 합니다.");
        }
    }

    /**
     * 내 정보 조회 메서드
     * 현재 로그인한 사용자의 정보를 DTO로 반환
     * @param userName 조회할 사용자 이름
     * @return 사용자 정보가 담긴 UserDto
     * @throws IllegalArgumentException 사용자를 찾을 수 없을 때
     */
    public UserDto getUserInfo(String userName) {
        User user=userRepository.findByUserName(userName)
                .orElseThrow(()->new IllegalArgumentException("사용자를 찾을 수 없습니다:"+userName));

        UserDto dto=new UserDto();
        dto.setUserName(user.getUserName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        return dto;
    }

    /**
     * 내 정보 수정 메서드
     * 사용자의 기본 정보(이름, 이메일, 전화번호)를 업데이트
     * @param userName 수정할 사용자 이름
     * @param userDto 수정할 정보가 담긴 DTO
     * @throws IllegalArgumentException 사용자를 찾을 수 없을 때
     */
    public void updateUserInfo(String userName,UserDto userDto) {
        User user=userRepository.findByUserName(userName)
                .orElseThrow(()->new IllegalArgumentException("사용자를 찾을 수 없습니다:"+userName));

        user.setUserName(userDto.getUserName());
        user.setEmail(userDto.getEmail());
        user.setPhone(userDto.getPhone());

        userRepository.save(user);
    }

    /**
     * 비밀번호 변경 메서드
     * 현재 비밀번호를 확인하고 새 비밀번호로 변경
     * @param userName 사용자 이름
     * @param currentpassword 현재 비밀번호
     * @param newpassword 새 비밀번호
     * @return true: 변경 성공, false: 현재 비밀번호 불일치
     * @throws IllegalArgumentException 사용자를 찾을 수 없을 때
     */
    public boolean changePassword(String userName, String currentpassword, String newpassword) {
        User user=userRepository.findByUserName(userName)
                .orElseThrow(()-> new IllegalArgumentException("사용자를 찾을 수 없습니다:"+userName));

        // ✅ 수정: currentpassword를 검증
        if(!passwordEncoder.matches(currentpassword, user.getPassword())) {
            return false;
        }

        // 새 비밀번호 유효성 검증
        validatePassword(newpassword);

        user.setPassword(passwordEncoder.encode(newpassword));
        userRepository.save(user);
        return true;
    }

    /**
     * Principal 객체에서 현재 로그인한 사용자 정보 가져오기
     * Spring Security의 인증 정보를 활용
     * @param principal Spring Security의 Principal 객체
     * @return 현재 로그인한 User 엔티티
     * @throws IllegalArgumentException 로그인하지 않았을 때
     * @throws UsernameNotFoundException 사용자를 찾을 수 없을 때
     */
    public User getCurrentUser(Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        String username = principal.getName();

        return userRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
    }

    /**
     * 사용자 이름으로 사용자 조회
     * getUser()와 유사하지만 예외 타입이 다름
     * @param username 조회할 사용자 이름
     * @return User 엔티티
     * @throws IllegalArgumentException 사용자를 찾을 수 없을 때
     */
    public User getUserByUserName(String username) {
        return userRepository.findByUserName(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));
    }

    /**
     * 이메일로 아이디 찾기
     * @param request 이메일 정보
     * @return 아이디 정보
     */
    public FindUsernameResponse findUsername(FindUsernameRequest request) {
        log.info("🔍 아이디 찾기 - 이메일: {}", request.getEmail());

        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());

        if (userOpt.isEmpty()) {
            log.warn("⚠️ 해당 이메일로 가입된 계정이 없습니다: {}", request.getEmail());
            throw new IllegalArgumentException("해당 이메일로 가입된 계정이 없습니다.");
        }

        User user = userOpt.get();
        log.info("✅ 아이디 찾기 성공 - 아이디: {}", user.getUserName());

        return FindUsernameResponse.builder()
                .userName(user.getUserName())
                .build();
    }

    /**
     * 아이디 + 이메일 확인 및 비밀번호 재설정 토큰 발급
     * @param request 아이디 + 이메일 정보
     * @return 재설정 토큰
     */
    public VerifyEmailResponse verifyEmailForPasswordReset(VerifyEmailRequest request) {
        log.info("🔐 비밀번호 재설정 이메일 인증 - 아이디: {}, 이메일: {}",
                request.getUserName(), request.getEmail());

        Optional<User> userOpt = userRepository.findByUserName(request.getUserName());

        if (userOpt.isEmpty()) {
            log.warn("⚠️ 존재하지 않는 아이디: {}", request.getUserName());
            throw new IllegalArgumentException("아이디 또는 이메일이 일치하지 않습니다.");
        }

        User user = userOpt.get();

        if (!user.getEmail().equals(request.getEmail())) {
            log.warn("⚠️ 이메일 불일치 - 입력: {}, DB: {}", request.getEmail(), user.getEmail());
            throw new IllegalArgumentException("아이디 또는 이메일이 일치하지 않습니다.");
        }

        // 🔥 비밀번호 재설정 토큰 생성 (UUID)
        String resetToken = UUID.randomUUID().toString();

        // 🔥 토큰과 사용자명 매핑 저장
        resetTokenStore.put(resetToken, user.getUserName());

        log.info("✅ 이메일 인증 성공 - 리셋 토큰 발급: {}", resetToken);

        return VerifyEmailResponse.builder()
                .resetToken(resetToken)
                .build();
    }

    /**
     * 비밀번호 재설정
     * @param request 재설정 토큰 + 새 비밀번호
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.info("🔑 비밀번호 재설정 - 토큰: {}", request.getResetToken());

        // 🔥 토큰 검증
        String userName = resetTokenStore.get(request.getResetToken());

        if (userName == null) {
            log.warn("⚠️ 유효하지 않거나 만료된 토큰: {}", request.getResetToken());
            throw new IllegalArgumentException("유효하지 않거나 만료된 토큰입니다.");
        }

        // 사용자 조회
        Optional<User> userOpt = userRepository.findByUserName(userName);

        if (userOpt.isEmpty()) {
            log.error("❌ 사용자를 찾을 수 없음: {}", userName);
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        User user = userOpt.get();

        // 🔥 비밀번호 유효성 검증
        validatePassword(request.getNewPassword());

        // 🔥 비밀번호 암호화 후 저장
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        user.setPassword(encodedPassword);
        userRepository.save(user);

        // 🔥 사용된 토큰 삭제
        resetTokenStore.remove(request.getResetToken());

        log.info("✅ 비밀번호 재설정 완료 - 사용자: {}", userName);
    }

    /**
     * ✅ 소셜 로그인 사용자 필수정보 입력 (주소 + 비밀번호)
     * @param userName 사용자 이름
     * @param address 주소
     * @param addressDetail 상세 주소
     * @param postalCode 우편번호
     * @param newPassword 새 비밀번호 (선택)
     */
    @Transactional
    public User completeProfile(String userName, String address, String addressDetail,
                                String postalCode, String newPassword) {
        log.info("📝 필수정보 입력 - 사용자: {}", userName);

        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userName));

        // 1. 주소 정보 업데이트 (필수)
        if (address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException("주소는 필수 항목입니다.");
        }
        if (postalCode == null || postalCode.trim().isEmpty()) {
            throw new IllegalArgumentException("우편번호는 필수 항목입니다.");
        }

        user.setAddress(address);
        user.setAddressDetail(addressDetail != null ? addressDetail : "");
        user.setPostalCode(postalCode);

        log.info("✅ 주소 정보 업데이트 완료");

        // 2. 비밀번호 설정 (선택)
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            // 비밀번호 유효성 검증
            validatePassword(newPassword);

            // 비밀번호 암호화 후 저장
            user.setPassword(passwordEncoder.encode(newPassword));

            log.info("✅ 비밀번호 설정 완료");
        }

        // 3. 저장
        User savedUser = userRepository.save(user);

        log.info("✅ 필수정보 입력 완료 - 사용자: {}", userName);

        return savedUser;
    }
}  // ✅ 마지막 중괄호 추가!