package com.project.gugumarket.config;

import com.project.gugumarket.security.JwtAuthenticationEntryPoint;
import com.project.gugumarket.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CSRF 비활성화 (JWT 사용하므로)
                .csrf(csrf -> csrf.disable())

                // 세션 사용 안 함 (JWT 사용)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 인증 실패 처리
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )

                .authorizeHttpRequests(auth -> auth
                        // ✅ 정규식으로 사용자 레벨 조회 허용
                        .requestMatchers(new RegexRequestMatcher("/api/users/\\d+/level", null)).permitAll()

                        // ✅ 인증 불필요 경로
                        .requestMatchers(
                                // 인증 관련
                                "/api/auth/**",
                                "/api/users/signup",
                                "/api/users/find-username",
                                "/api/users/verify-email",
                                "/api/users/reset-password",
                                "/api/users/check-username",

                                // 메인 & 상품
                                "/api/main",
                                "/api/products/list",
                                "/api/products/*",
                                "/api/products/*/comments",      // ✅ 추가: 댓글 조회
                                "/api/categories",
                                "/api/districts",
                                "/api/products/map",
                                "/api/products/map/bounds",

                                // WebSocket
                                "/ws/**",
                                "/topic/**",
                                "/app/**",

                                // 정적 리소스
                                "/api/public/**",
                                "/uploads/**",
                                "/images/**",
                                "/css/**",
                                "/js/**"
                        ).permitAll()

                        // ✅ 인증 필요 경로
                        .requestMatchers("/mypage/**").authenticated()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/products/map/update-coordinates").authenticated()
                        .requestMatchers("/api/chat/**").authenticated()

                        // 나머지는 인증 필요
                        .anyRequest().authenticated()
                )
                // JWT 필터 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // CORS 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // React 앱의 주소 허용
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));

        // 모든 HTTP 메서드 허용
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // 모든 헤더 허용
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 인증 정보 포함 허용
        configuration.setAllowCredentials(true);

        // preflight 요청 캐시 시간
        configuration.setMaxAge(3600L);

        // 노출할 헤더 설정
        configuration.setExposedHeaders(Arrays.asList("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // AuthenticationManager Bean 등록 (로그인 시 필요)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}