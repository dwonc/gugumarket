package com.project.gugumarket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .requestMatchers(
                                new AntPathRequestMatcher("/test/**"),
                                new AntPathRequestMatcher("/users/**"),
                                new AntPathRequestMatcher("/users/signup"),
                                new AntPathRequestMatcher("/users/login"),
                                new AntPathRequestMatcher("/users/check-username"),
                                new AntPathRequestMatcher("/"),
                                new AntPathRequestMatcher("/main"),
                                new AntPathRequestMatcher("/h2-console/**"),
                                new AntPathRequestMatcher("/js/**"),
                                new AntPathRequestMatcher("/images/**"),
                                new AntPathRequestMatcher("/css/**"),
                                new AntPathRequestMatcher("/uploads/**")  // 🔥 업로드된 이미지 접근 허용
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                //로그인 설정
                .formLogin(form -> form
                        .loginPage("/users/login")
                        .loginProcessingUrl("/users/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/main", true)
                        .failureUrl("/users/login?error=true")
                        .permitAll()
                )
                //세션 유지
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // 세션 유지
                )
                //로그아웃 설정
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/users/logout", "POST"))
                        .logoutSuccessUrl("/?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .clearAuthentication(true)
                        .permitAll()
                )
                // 🔥 CSRF 설정 수정 - API 엔드포인트 추가
                .csrf((csrf) -> csrf
                        .ignoringRequestMatchers(
                                new AntPathRequestMatcher("/h2-console/**"),
                                new AntPathRequestMatcher("/api/**"),  // 🔥 이 줄 추가!
                                new AntPathRequestMatcher("/users/signup"),      // ✅ 추가
                                new AntPathRequestMatcher("/users/check-username"), // ✅ 추가
                                new AntPathRequestMatcher("/users/logout"),      // ✅ 추가
                                new AntPathRequestMatcher("/product/*/status"),
                                new AntPathRequestMatcher("/product/*/like"),
                                new AntPathRequestMatcher("/product/*"),
                                new AntPathRequestMatcher("/transaction/*/complete")
                        ))
                .headers((headers)->headers.addHeaderWriter(
                        new XFrameOptionsHeaderWriter(
                                XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN))
                );
        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}