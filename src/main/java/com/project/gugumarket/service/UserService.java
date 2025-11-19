package com.project.gugumarket.service;

import com.project.gugumarket.DataNotFoundException;
import com.project.gugumarket.dto.UserDto;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 사용자 관련 비즈니스 로직을 처리하는 서비스 클래스
 * 회원가입, 정보 조회/수정, 비밀번호 변경 등의 기능을 담당
 */
@RequiredArgsConstructor  // final 필드에 대한 생성자 자동 생성
@Service  // Spring의 서비스 계층 컴포넌트로 등록
public class UserService {

    @Autowired
    private final UserRepository userRepository;  // 데이터베이스와 통신하는 리포지토리
    private final BCryptPasswordEncoder passwordEncoder;  // 비밀번호 암호화를 위한 인코더

    /**
     * 사용자 이름으로 사용자 정보 조회
     * @param userName 조회할 사용자 이름
     * @return User 엔티티
     * @throws DataNotFoundException 사용자를 찾을 수 없을 때
     */
    public User getUser(String userName) {
        // Optional로 감싸진 User 객체 조회
        Optional<User> siteUser = this.userRepository.findByUserName(userName);

        if(siteUser.isPresent()) {
            // 사용자가 존재하면 User 객체 반환
            User user = siteUser.get();
            return user;
        }
        else {
            // 사용자가 없으면 예외 발생
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
    @Transactional  // 트랜잭션 처리 - 오류 발생 시 롤백
    public User create(UserDto userDto) {
        // 1. 중복 사용자 체크
        // 이미 존재하는 아이디인지 확인
        if (userRepository.existsByUserName(userDto.getUserName())) {
            throw new IllegalArgumentException("이미 존재하는 사용자 ID입니다.");
        }

        // 이미 사용 중인 이메일인지 확인
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
                .userName(userDto.getUserName())  // 사용자 이름
                .password(encodedPassword)  // 암호화된 비밀번호
                .email(userDto.getEmail())  // 이메일
                .nickname(userDto.getNickname())  // 닉네임
                .phone(userDto.getPhone())  // 전화번호
                .address(userDto.getAddress())  // 주소
                .addressDetail(userDto.getAddressDetail())  // 상세 주소
                .postalCode(userDto.getPostalCode())  // 우편번호
                .createdDate(LocalDateTime.now())  // 생성 일시
                .isActive(true)  // 활성 상태
                .role("USER")  // 기본 권한: 일반 사용자
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
        // 비밀번호가 null이거나 8자 미만인지 확인
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("비밀번호는 최소 8자 이상이어야 합니다.");
        }

        // 영문 포함 여부 확인
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        // 숫자 포함 여부 확인
        boolean hasDigit = password.matches(".*\\d.*");

        // 영문과 숫자를 모두 포함하지 않으면 예외 발생
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
        // 사용자 조회, 없으면 예외 발생
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
        // 사용자 조회
        User user=userRepository.findByUserName(userName)
                .orElseThrow(()->new IllegalArgumentException("사용자를 찾을 수 없습니다:"+userName));

        // 정보 업데이트
        user.setUserName(userDto.getUserName());
        user.setEmail(userDto.getEmail());
        user.setPhone(userDto.getPhone());

        // 변경사항 저장
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
    public boolean changePassword(String userName, String currentpassword,String newpassword) {
        // 사용자 조회
        User user=userRepository.findByUserName(userName)
                .orElseThrow(()-> new IllegalArgumentException("사용자를 찾을 수 없습니다:"+userName));

        // 새 비밀번호가 현재 비밀번호와 일치하지 않는지 확인
        // ⚠️ 주의: 로직 오류 - currentpassword 대신 newpassword를 비교하고 있음
        if(!passwordEncoder.matches(newpassword,user.getPassword())) {
            return false;
        }

        // 새 비밀번호를 암호화하여 저장
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
        // Principal이 null이면 로그인하지 않은 상태
        if (principal == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        // Principal에서 사용자 이름 추출
        String username = principal.getName();

        // 사용자 이름으로 User 엔티티 조회
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
}