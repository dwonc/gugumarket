package com.project.gugumarket.config;

import com.project.gugumarket.security.JwtAuthenticationEntryPoint;
import com.project.gugumarket.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security 설정 클래스
 * JWT 기반 인증/인가 및 CORS 정책을 설정합니다.
 */
@Configuration  // Spring 설정 클래스임을 명시
@EnableWebSecurity  // Spring Security 활성화
@RequiredArgsConstructor  // final 필드에 대한 생성자 자동 생성
public class SecurityConfig {

    // JWT 인증 필터 - 요청마다 토큰을 검증
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // JWT 인증 실패 시 처리 핸들러
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    // application.properties에서 CORS 허용 출처를 읽어옴
    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    /**
     * Spring Security 필터 체인 설정
     * 인증/인가 규칙, CORS, CSRF, 세션 정책 등을 구성합니다.
     */
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS 설정 적용 (Cross-Origin Resource Sharing)
                // 프론트엔드와 백엔드가 다른 도메인에서 실행될 때 필요
                //CorsConfigurationSource=인터페이스를 구현하는 객체를 제공하는 역할
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CSRF 보호 비활성화
                // JWT를 사용하는 Stateless 환경에서는 CSRF 공격 위험이 낮음
                // JWT 방식은 세션 쿠키 대신 요청 헤더(예: Authorization: Bearer <token>)에 토큰을 담아 보내기 때문에 공격에 안전
                .csrf(csrf -> csrf.disable())

                // 세션 정책을 STATELESS로 설정
                // JWT 토큰 기반 인증을 사용하므로 서버에서 세션을 생성하지 않음
                // STATELESS=서버가 클라이언트 상태 정보 저장x,모든 요청을 처음부터 독립적으로 처리
                // 필요한 정보,인증을 요청 자체에 포함하여 보낸다는 것
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 인증 실패 시 예외 처리
                // 인증되지 않은 사용자가 보호된 리소스에 접근할 때 실행
                //사용자가 인증되지 않은 상태(Unauthenticated)에서 보호된 리소스(API 엔드포인트)에 접근하려고 할 때.
                //인증 과정 중 예외가 발생했을 때 (예: JWT가 누락되었거나 유효하지 않을 때) 호출이 된다
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )

                // HTTP 요청에 대한 인증/인가 규칙 설정
                .authorizeHttpRequests(auth -> auth
                        // 정규식을 사용하여 특정 패턴의 URL 허용
                        // 예: /api/users/123/level (사용자 레벨 조회는 인증 불필요)
                        .requestMatchers(new RegexRequestMatcher("/api/users/\\d+/level", null)).permitAll()

                        // 인증 없이 접근 가능한 경로들
                        .requestMatchers(
                                // 인증 관련 API (로그인, 토큰 재발급 등)
                                "/api/auth/**",
                                "/api/users/signup",  // 회원가입
                                "/api/users/find-username",  // 아이디 찾기
                                "/api/users/verify-email",  // 이메일 인증
                                "/api/users/reset-password",  // 비밀번호 재설정
                                "/api/users/check-username",  // 아이디 중복 확인

                                // 메인 페이지 및 상품 조회 API
                                "/api/main",  // 메인 페이지
                                "/api/products/list",  // 상품 목록 조회
                                "/api/products/*",  // 상품 상세 조회
                                "/api/products/*/comments",  // 상품 댓글 조회 (읽기만 가능)
                                "/api/categories",  // 카테고리 목록
                                "/api/districts",  // 지역 목록
                                "/api/products/map",  // 지도 기반 상품 조회
                                "/api/products/map/bounds",  // 지도 범위 내 상품 조회

                                // WebSocket 관련 경로 (실시간 채팅)
                                "/ws/**",  // WebSocket 연결
                                "/topic/**",  // 구독 경로
                                "/app/**",  // 메시지 전송 경로

                                // 정적 리소스 (이미지, CSS, JavaScript 등)
                                "/api/public/**",
                                "/uploads/**",  // 업로드된 파일
                                "/images/**",  // 이미지
                                "/css/**",  // CSS 파일
                                "/js/**"  // JavaScript 파일
                        ).permitAll()  // 위 경로들은 모두 인증 없이 접근 가능

                        // 인증이 필요한 경로들
                        .requestMatchers("/mypage/**").authenticated()  // 마이페이지
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")  // 관리자 전용
                        .requestMatchers("/api/products/map/update-coordinates").authenticated()  // 좌표 업데이트
                        .requestMatchers("/api/chat/**").authenticated()  // 채팅 기능

                        // 댓글 작성/수정/삭제는 로그인 필요 (HTTP 메서드별 설정)
                        .requestMatchers(HttpMethod.POST, "/api/products/*/comments").authenticated()  // 댓글 작성
                        .requestMatchers(HttpMethod.PUT, "/api/comments/**").authenticated()  // 댓글 수정
                        .requestMatchers(HttpMethod.DELETE, "/api/comments/**").authenticated()  // 댓글 삭제

                        // 위에서 명시하지 않은 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )
                // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
                // 모든 요청이 들어올 때 JWT 토큰을 먼저 검증하도록 설정
                //UsernamePasswordAuthenticationFilter=폼 기반 로그인 시 사용되며, 요청에서 사용자 이름과 비밀번호를 추출하여 인증을 시도하는 역할
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS 설정을 구성하는 Bean
     * 프론트엔드 애플리케이션이 백엔드 API를 호출할 수 있도록 허용
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 출처(Origin) 설정
        // 예: http://localhost:3000 (React 개발 서버)
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));

        // 허용할 HTTP 메서드 설정
        // GET, POST, PUT, DELETE, PATCH, OPTIONS 요청 허용
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // 허용할 HTTP 헤더 설정
        // 모든 헤더 허용 (Authorization, Content-Type 등)
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 인증 정보 포함 허용
        // 쿠키나 Authorization 헤더를 포함한 요청 허용
        configuration.setAllowCredentials(true);

        // Preflight 요청의 캐시 시간 설정 (1시간)
        // OPTIONS 요청의 결과를 1시간 동안 캐싱하여 성능 향상
        configuration.setMaxAge(3600L);

        // 클라이언트에 노출할 헤더 설정
        // JavaScript에서 읽을 수 있는 응답 헤더 지정
        configuration.setExposedHeaders(Arrays.asList("Authorization"));

        // 모든 경로에 대해 위 CORS 설정 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * 비밀번호 암호화를 위한 BCrypt 인코더 Bean 등록
     * 회원가입 시 비밀번호를 암호화하고, 로그인 시 비밀번호를 검증하는 데 사용
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager Bean 등록
     * 로그인 처리 시 사용자 인증을 담당
     * username과 password를 검증하여 인증 성공 여부 결정
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}