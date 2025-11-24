package com.project.gugumarket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.gugumarket.dto.KakaoTokenResponse;
import com.project.gugumarket.dto.KakaoUserInfo;
import com.project.gugumarket.dto.LoginResponse;
import com.project.gugumarket.dto.UserResponseDto;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.UserRepository;
import com.project.gugumarket.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoAuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder passwordEncoder;
    private final CustomUserDetailService customUserDetailService;

    @Value("${kakao.client-id}")
    private String kakaoClientId;

    @Value("${kakao.redirect-uri}")
    private String kakaoRedirectUri;

    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    /**
     * 카카오 Authorization Code로 Access Token 받기
     */
    public KakaoTokenResponse getKakaoAccessToken(String code) {
        log.info("🔑 카카오 토큰 요청 시작 - code: {}", code);

        RestTemplate restTemplate = new RestTemplate();

        // 요청 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 요청 바디 설정
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoClientId);
        params.add("redirect_uri", kakaoRedirectUri);
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<KakaoTokenResponse> response = restTemplate.postForEntity(
                    KAKAO_TOKEN_URL,
                    request,
                    KakaoTokenResponse.class
            );

            log.info("✅ 카카오 토큰 받기 성공");
            return response.getBody();

        } catch (Exception e) {
            log.error("❌ 카카오 토큰 받기 실패: {}", e.getMessage());
            throw new RuntimeException("카카오 토큰 받기 실패", e);
        }
    }

    /**
     * 카카오 Access Token으로 사용자 정보 가져오기
     */
    public KakaoUserInfo getKakaoUserInfo(String accessToken) {
        log.info("👤 카카오 사용자 정보 요청 시작");

        RestTemplate restTemplate = new RestTemplate();

        // 요청 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    KAKAO_USER_INFO_URL,
                    HttpMethod.GET,
                    request,
                    String.class
            );

            // JSON 파싱
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            Long id = jsonNode.get("id").asLong();
            JsonNode kakaoAccount = jsonNode.get("kakao_account");
            JsonNode profile = kakaoAccount.get("profile");

            String email = kakaoAccount.has("email") ? kakaoAccount.get("email").asText() : null;
            String nickname = profile.get("nickname").asText();
            String profileImage = profile.has("profile_image_url")
                    ? profile.get("profile_image_url").asText()
                    : null;

            log.info("✅ 카카오 사용자 정보 가져오기 성공 - email: {}", email);

            return KakaoUserInfo.builder()
                    .id(id)
                    .email(email)
                    .nickname(nickname)
                    .profileImage(profileImage)
                    .build();

        } catch (Exception e) {
            log.error("❌ 카카오 사용자 정보 가져오기 실패: {}", e.getMessage());
            throw new RuntimeException("카카오 사용자 정보 가져오기 실패", e);
        }
    }

    /**
     * ✅ 주소 정보가 필요한지 체크하는 메서드
     */
    private boolean requiresAddressUpdate(User user) {
        // 주소가 "미입력" 또는 비어있거나 null인 경우
        return user.getAddress() == null ||
                user.getAddress().isEmpty() ||
                user.getAddress().equals("미입력") ||
                user.getPostalCode() == null ||
                user.getPostalCode().equals("00000");
    }

    /**
     * 카카오 로그인 처리 (회원가입 or 로그인)
     */
    @Transactional
    public LoginResponse kakaoLogin(String code) {
        log.info("🚀 카카오 로그인 처리 시작");

        // 1. 카카오 토큰 받기
        KakaoTokenResponse tokenResponse = getKakaoAccessToken(code);

        // 2. 카카오 사용자 정보 가져오기
        KakaoUserInfo kakaoUserInfo = getKakaoUserInfo(tokenResponse.getAccessToken());

        // 3. 이메일이 없으면 에러
        if (kakaoUserInfo.getEmail() == null) {
            throw new RuntimeException("카카오 계정에 이메일이 없습니다. 카카오 계정 설정을 확인해주세요.");
        }

        // 4. 기존 회원 확인 or 신규 회원 가입
        User user = userRepository.findByEmail(kakaoUserInfo.getEmail())
                .orElseGet(() -> createKakaoUser(kakaoUserInfo));

        // ✅ 5. 주소 입력 필요 여부 체크
        boolean needsAddress = requiresAddressUpdate(user);

        log.info("🏠 주소 입력 필요 여부: {}", needsAddress);

        // 6. JWT 토큰 생성
        UserDetails userDetails = customUserDetailService.loadUserByUsername(user.getUserName());
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserName());

        log.info("✅ 카카오 로그인 성공 - username: {}", user.getUserName());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .username(user.getUserName())
                .email(user.getEmail())
                .role(user.getRole())
                .requiresAddressUpdate(needsAddress)  // ✅ 플래그 추가
                .user(UserResponseDto.fromEntity(user))  // ✅ 사용자 정보 추가
                .build();
    }

    /**
     * 카카오 신규 회원 생성
     */
    private User createKakaoUser(KakaoUserInfo kakaoUserInfo) {
        log.info("🆕 카카오 신규 회원 생성 - email: {}", kakaoUserInfo.getEmail());

        // 카카오 ID로 유니크한 username 생성
        String username = "kakao_" + kakaoUserInfo.getId();

        // 이미 같은 username이 있는지 확인 (거의 없겠지만)
        int count = 1;
        String finalUsername = username;
        while (userRepository.findByUserName(finalUsername).isPresent()) {
            finalUsername = username + "_" + count++;
        }

        // 랜덤 비밀번호 생성 (카카오 로그인이므로 사용 안 함)
        String randomPassword = UUID.randomUUID().toString();

        User newUser = User.builder()
                .userName(finalUsername)
                .email(kakaoUserInfo.getEmail())
                .nickname(kakaoUserInfo.getNickname())
                .password(passwordEncoder.encode(randomPassword))
                .profileImage(kakaoUserInfo.getProfileImage())
                .phone("") // 카카오에서 제공하지 않음
                .address("미입력")  // ✅ 주소 누락 표시
                .addressDetail("미입력")
                .postalCode("00000")
                .role("USER")
                .isActive(true)
                .build();

        User savedUser = userRepository.save(newUser);
        log.info("✅ 카카오 신규 회원 생성 완료 - username: {}", savedUser.getUserName());

        return savedUser;
    }
}