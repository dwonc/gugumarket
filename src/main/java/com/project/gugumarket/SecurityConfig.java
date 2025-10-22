package com.project.gugumarket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/users/signup", "/users/login", "/users/check-username").permitAll()
                        .requestMatchers("/uploads/**").permitAll()  // ← 이 줄 추가
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                        .anyRequest().authenticated()
                )
                //로그인 설정
                .formLogin(form -> form
                        .loginPage("/users/login")           // 커스텀 로그인 페이지
                        .loginProcessingUrl("/users/login") // 로그인 처리 URL
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/main", true)        // 로그인 성공 시 이동
                        .failureUrl("/users/login?error=true")    // 로그인 실패 시
                        .permitAll()
                )
                //로그아웃 설정
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/users/logout", "POST"))  // GET → POST로 변경 (보안)
                        .logoutSuccessUrl("/?logout=true")  // 로그아웃 성공 메시지 표시용
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .clearAuthentication(true)  // 인증 정보 명확히 제거
                        .permitAll()  // 로그아웃은 누구나 가능
                )
                .csrf((csrf) -> csrf.ignoringRequestMatchers(new AntPathRequestMatcher("/h2-console/**")))
                .headers((headers)->headers.addHeaderWriter(new XFrameOptionsHeaderWriter(XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN)));
        return http.build();
    }
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
