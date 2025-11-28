package com.project.gugumarket.service;

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
 * 회원가입, 정보 조회/수정, 비밀번호 변경, 계정 찾기 등의 기능을 담당
 */
@Slf4j  // 로깅 기능
@RequiredArgsConstructor  // final 필드 자동 생성자 주입
@Service  // 스프링 서비스 계층 컴포넌트
public class UserService {

    @Autowired
    private final UserRepository userRepository;  // 사용자 데이터베이스 접근
    private final BCryptPasswordEncoder passwordEncoder;  // 비밀번호 암호화

    /**
     * 비밀번호 재설정 토큰 저장소
     * 메모리 기반이므로 서버 재시작 시 토큰 소멸
     * 실제 프로덕션 환경에서는 Redis 등의 외부 저장소 사용 권장
     *
     * 사용 흐름:
     * 1. 이메일 인증 성공 시 UUID 토큰 생성 및 저장
     * 2. 비밀번호 재설정 시 토큰 검증
     * 3. 재설정 완료 후 토큰 삭제 (일회용)
     */
    private final Map<String, String> resetTokenStore = new HashMap<>();

    /**
     * 사용자 이름으로 사용자 정보 조회
     *
     * @param userName 조회할 사용자 이름
     * @return User 엔티티
     * @throws DataNotFoundException 사용자를 찾을 수 없을 때
     */
    public User getUser(String userName) { //Optional= 값이 있을수도 없을수도 있는 컨테이너 객체 null 안정성을 위해 사용
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
     * 아이디 중복 체크
     * 회원가입 시 아이디가 이미 사용 중인지 실시간으로 확인
     *
     * @param userName 확인할 사용자 이름
     * @return true: 중복됨(사용 불가), false: 사용 가능
     */
    public boolean isUserNameDuplicate(String userName) {
        // existsByUserName: JPA에서 제공하는 존재 여부 확인 메서드
        return userRepository.existsByUserName(userName);
    }

    /**
     * 새로운 사용자 생성 (회원가입)
     *
     * @param userDto 사용자 정보가 담긴 DTO
     * @return 저장된 User 엔티티
     * @throws IllegalArgumentException 중복된 사용자 또는 유효하지 않은 정보
     *
     * 처리 순서:
     * 1. 아이디/이메일 중복 체크
     * 2. 비밀번호 유효성 검증 (최소 8자, 영문+숫자)
     * 3. 비밀번호 BCrypt 암호화
     * 4. User 엔티티 생성 및 저장
     */
    @Transactional  // 트랜잭션 관리 (데이터 일관성 보장)
    public User create(UserDto userDto) { //username=id
        // 1. 중복 사용자 체크
        if (userRepository.existsByUserName(userDto.getUserName())) {
            throw new IllegalArgumentException("이미 존재하는 사용자 ID입니다.");
        }

        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 2. 비밀번호 유효성 검증
        validatePassword(userDto.getPassword());

        // 3. 비밀번호를 BCrypt로 암호화
        // BCrypt: 단방향 해시 함수 (복호화 불가능, salt 자동 추가)
        String encodedPassword=passwordEncoder.encode(userDto.getPassword());
        System.out.println("원본 비밀번호: "+userDto.getPassword());
        System.out.println("암호화된 비밀번호: "+encodedPassword);

        // 4. 새 사용자 객체 생성 (Builder 패턴 사용)
        User user = User.builder()
                .userName(userDto.getUserName())
                .password(encodedPassword)  // 암호화된 비밀번호 저장
                .email(userDto.getEmail())
                .nickname(userDto.getNickname())
                .phone(userDto.getPhone())
                .address(userDto.getAddress())
                .addressDetail(userDto.getAddressDetail())
                .postalCode(userDto.getPostalCode())
                .createdDate(LocalDateTime.now())
                .isActive(true)  // 계정 활성화 상태
                .role("USER")  // 기본 권한: USER
                .build();

        // 5. 데이터베이스에 저장
        User savedUser=userRepository.save(user);
        System.out.println("DB 저장 완료 - userId: " + savedUser.getUserId());
        return savedUser;
    }

    /**
     * 비밀번호 유효성 검증 메서드
     * 보안 규칙을 만족하는지 확인
     *
     * @param password 검증할 비밀번호
     * @throws IllegalArgumentException 비밀번호가 조건을 만족하지 않을 때
     *
     * 검증 규칙:
     * - 최소 8자 이상
     * - 영문 포함 필수
     * - 숫자 포함 필수
     */
    private void validatePassword(String password) {
        // 길이 검증
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("비밀번호는 최소 8자 이상이어야 합니다.");
        }

        // 영문 포함 여부 검증 (정규식)
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        // 숫자 포함 여부 검증 (정규식)
        boolean hasDigit = password.matches(".*\\d.*");

        if (!hasLetter || !hasDigit) {
            throw new IllegalArgumentException("비밀번호는 영문과 숫자를 포함해야 합니다.");
        }
    }

    /**
     * 내 정보 조회
     * 현재 로그인한 사용자의 정보를 DTO로 반환
     *
     * @param userName 조회할 사용자 이름
     * @return 사용자 정보가 담긴 UserDto
     * @throws IllegalArgumentException 사용자를 찾을 수 없을 때
     */
    public UserDto getUserInfo(String userName) {
        // Optional을 사용한 안전한 조회
        User user=userRepository.findByUserName(userName)
                .orElseThrow(()->new IllegalArgumentException("사용자를 찾을 수 없습니다:"+userName));

        // Entity를 DTO로 변환 (필요한 정보만 노출)
        UserDto dto=new UserDto();
        dto.setUserName(user.getUserName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        return dto;
    }

    /**
     * 내 정보 수정
     * 사용자의 기본 정보(이름, 이메일, 전화번호)를 업데이트
     *
     * @param userName 수정할 사용자 이름
     * @param userDto 수정할 정보가 담긴 DTO
     * @throws IllegalArgumentException 사용자를 찾을 수 없을 때
     */
    public void updateUserInfo(String userName,UserDto userDto) {
        User user=userRepository.findByUserName(userName)
                .orElseThrow(()->new IllegalArgumentException("사용자를 찾을 수 없습니다:"+userName));

        // 변경할 정보 설정
        user.setUserName(userDto.getUserName());
        user.setEmail(userDto.getEmail());
        user.setPhone(userDto.getPhone());

        // 저장 (JPA의 더티 체킹으로 자동 UPDATE 쿼리 실행)
        userRepository.save(user);
    }

    /**
     * 비밀번호 변경
     * 현재 비밀번호를 확인하고 새 비밀번호로 변경
     *
     * @param userName 사용자 이름
     * @param currentpassword 현재 비밀번호 (평문)
     * @param newpassword 새 비밀번호 (평문)
     * @return true: 변경 성공, false: 현재 비밀번호 불일치
     * @throws IllegalArgumentException 사용자를 찾을 수 없을 때
     *
     * 보안 처리:
     * 1. 현재 비밀번호 검증 (BCrypt matches 사용)
     * 2. 새 비밀번호 유효성 검증
     * 3. 새 비밀번호 암호화 후 저장
     */
    public boolean changePassword(String userName, String currentpassword, String newpassword) {
        User user=userRepository.findByUserName(userName)
                .orElseThrow(()-> new IllegalArgumentException("사용자를 찾을 수 없습니다:"+userName));

        // 현재 비밀번호 검증
        // matches: 평문 비밀번호와 암호화된 비밀번호를 비교
        if(!passwordEncoder.matches(currentpassword, user.getPassword())) {
            return false;  // 현재 비밀번호가 틀림
        }

        // 새 비밀번호 유효성 검증
        validatePassword(newpassword);

        // 새 비밀번호 암호화 후 저장
        user.setPassword(passwordEncoder.encode(newpassword));
        userRepository.save(user);
        return true;
    }

    /**
     * Principal 객체에서 현재 로그인한 사용자 정보 가져오기
     * Spring Security의 인증 정보를 활용
     *
     * @param principal Spring Security의 Principal 객체
     * @return 현재 로그인한 User 엔티티
     * @throws IllegalArgumentException 로그인하지 않았을 때
     * @throws UsernameNotFoundException 사용자를 찾을 수 없을 때
     *
     * Principal: Spring Security가 인증된 사용자 정보를 담는 객체 누가 사용하고 있는지를 나타내준다
     * principal.getName()으로 사용자 아이디(username)를 가져올 수 있음
     */
    public User getCurrentUser(Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        // Principal에서 사용자 아이디 추출
        String username = principal.getName();

        return userRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
    }

    /**
     * 사용자 이름으로 사용자 조회
     * getUser()와 유사하지만 예외 타입이 다름
     *
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
     * 사용자가 아이디를 잊어버렸을 때 이메일로 조회
     *
     * @param request 이메일 정보
     * @return 아이디 정보
     * @throws IllegalArgumentException 이메일로 가입된 계정이 없을 때
     */
    public FindUsernameResponse findUsername(FindUsernameRequest request) {
        log.info("🔍 아이디 찾기 - 이메일: {}", request.getEmail());

        // 이메일로 사용자 조회
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
     * 비밀번호 재설정을 위한 이메일 인증
     * 아이디와 이메일이 일치하는지 확인하고 재설정 토큰 발급
     *
     * @param request 아이디 + 이메일 정보
     * @return 재설정 토큰 (UUID)
     * @throws IllegalArgumentException 아이디/이메일 불일치
     *
     * 처리 흐름:
     * 1. 아이디로 사용자 조회
     * 2. 이메일 일치 여부 확인
     * 3. UUID 토큰 생성 및 저장
     * 4. 토큰 반환 (클라이언트가 다음 단계에서 사용)
     */
    public VerifyEmailResponse verifyEmailForPasswordReset(VerifyEmailRequest request) {
        log.info("🔐 비밀번호 재설정 이메일 인증 - 아이디: {}, 이메일: {}",
                request.getUserName(), request.getEmail());

        // 아이디로 사용자 조회
        Optional<User> userOpt = userRepository.findByUserName(request.getUserName());

        if (userOpt.isEmpty()) {
            log.warn("⚠️ 존재하지 않는 아이디: {}", request.getUserName());
            throw new IllegalArgumentException("아이디 또는 이메일이 일치하지 않습니다.");
        }

        User user = userOpt.get();

        // 이메일 일치 여부 확인
        if (!user.getEmail().equals(request.getEmail())) {
            log.warn("⚠️ 이메일 불일치 - 입력: {}, DB: {}", request.getEmail(), user.getEmail());
            throw new IllegalArgumentException("아이디 또는 이메일이 일치하지 않습니다.");
        }

        // UUID를 사용한 재설정 토큰 생성
        // UUID: 중복되지 않는 고유 식별자
        String resetToken = UUID.randomUUID().toString();

        // 토큰과 사용자명을 메모리에 저장
        // Key: 토큰, Value: 사용자명
        resetTokenStore.put(resetToken, user.getUserName());

        log.info("✅ 이메일 인증 성공 - 리셋 토큰 발급: {}", resetToken);

        return VerifyEmailResponse.builder()
                .resetToken(resetToken)
                .build();
    }

    /**
     * 비밀번호 재설정
     * 토큰을 검증하고 새 비밀번호로 변경
     *
     * @param request 재설정 토큰 + 새 비밀번호
     * @throws IllegalArgumentException 토큰이 유효하지 않거나 만료됨
     *
     * 처리 순서:
     * 1. 토큰 검증 (resetTokenStore에서 조회)
     * 2. 사용자 조회
     * 3. 새 비밀번호 유효성 검증
     * 4. 비밀번호 암호화 후 저장
     * 5. 사용된 토큰 삭제 (일회용)
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.info("🔑 비밀번호 재설정 - 토큰: {}", request.getResetToken());

        // 토큰으로 사용자명 조회
        String userName = resetTokenStore.get(request.getResetToken());

        // 토큰이 없거나 이미 사용됨
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

        // 새 비밀번호 유효성 검증
        validatePassword(request.getNewPassword());

        // 비밀번호 암호화 후 저장
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        user.setPassword(encodedPassword);
        userRepository.save(user);

        // 사용된 토큰 삭제 (재사용 방지)
        resetTokenStore.remove(request.getResetToken());

        log.info("✅ 비밀번호 재설정 완료 - 사용자: {}", userName);
    }

    /**
     * 소셜 로그인 사용자 필수정보 입력
     * OAuth로 가입한 사용자가 추가 정보(주소, 비밀번호)를 입력할 때 사용
     *
     * @param userName 사용자 이름
     * @param address 주소 (필수)
     * @param addressDetail 상세 주소
     * @param postalCode 우편번호 (필수)
     * @param newPassword 새 비밀번호 (선택)
     * @return 업데이트된 User 엔티티
     *
     * 소셜 로그인 사용자는 초기에 주소 정보가 없으므로
     * 상품을 판매하려면 반드시 주소를 입력해야 함
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
        // 소셜 로그인 사용자는 비밀번호가 없을 수 있음
        // 나중에 일반 로그인도 사용하려면 비밀번호 설정 필요
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

    /**
     * userId로 사용자 조회
     * 다른 사용자의 정보를 조회할 때 사용 (예: 판매자 정보 표시)
     *
     * @param userId 조회할 사용자 ID
     * @return User 엔티티
     * @throws IllegalArgumentException 사용자를 찾을 수 없을 때
     */
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }
}