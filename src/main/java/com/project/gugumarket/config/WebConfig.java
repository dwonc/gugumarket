package com.project.gugumarket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Spring MVC 웹 설정 클래스
 * CORS 정책과 정적 리소스(이미지 파일) 경로를 설정합니다.
 */
@Configuration  // Spring 설정 클래스임을 명시
public class WebConfig implements WebMvcConfigurer {

    // application.properties에서 제품 이미지 업로드 디렉토리 경로를 읽어옴
    // 기본값: uploads/products
    @Value("${file.upload-dir:uploads/products}")
    private String uploadDir;

    // application.properties에서 일반 파일 업로드 경로를 읽어옴
    // 프로필 이미지 등 제품 외 파일들이 저장되는 경로
    // 기본값: uploads/
    @Value("${file.upload.path:uploads/}")
    private String uploadPath;

    /**
     * CORS(Cross-Origin Resource Sharing) 설정
     * 프론트엔드 애플리케이션이 백엔드 API에 접근할 수 있도록 허용
     *
     * 주의: SecurityConfig에서도 CORS를 설정하고 있으므로,
     * 두 설정이 함께 적용됩니다. SecurityConfig의 설정이 우선순위가 높습니다.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) { //CORS 설정=프론트,백엔드가 다른 출처 일때 교차 출처 요청을 허용하는것
        registry.addMapping("/**")  // 모든 경로에 대해 CORS 설정 적용
                .allowedOrigins("http://localhost:5173")  // Vite 개발 서버 주소 허용
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")  // 허용할 HTTP 메서드
                .allowedHeaders("*")  // 모든 헤더 허용
                .allowCredentials(true)  // 인증 정보(쿠키, Authorization 헤더 등) 포함 허용
                .maxAge(3600);  // Preflight 요청 결과를 1시간 동안 캐싱

        System.out.println("✅ CORS 설정 완료: http://localhost:5173");
    }

    /**
     * 정적 리소스 핸들러 설정
     * 업로드된 이미지 파일들을 URL로 접근할 수 있도록 경로를 매핑합니다.
     *
     * 예시:
     * - /uploads/products/image.jpg -> 실제 파일 시스템의 uploads/products/image.jpg
     * - /uploads/profile.jpg -> 실제 파일 시스템의 uploads/profile.jpg
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) { //ResourceHandlerRegistry=웹 내 정적 리소스에 대한 접근 경로와 해당 리소스가 실제로 저장된 위치를 매핑 하는데 사용 되는 설정
        // ========== 제품 이미지 경로 설정 ==========

        // 업로드 디렉토리의 절대 경로 생성
        String absolutePath = new File(uploadDir).getAbsolutePath() + File.separator;

        // 디렉토리가 존재하지 않으면 생성
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();  // 필요한 모든 상위 디렉토리도 함께 생성
            System.out.println("📁 업로드 디렉토리 생성: " + absolutePath + " (성공: " + created + ")");
        }
        System.out.println("📂 제품 리소스 경로: " + absolutePath);

        // URL 패턴 /uploads/products/**를 실제 파일 시스템 경로로 매핑
        // 예: GET /uploads/products/phone.jpg -> 실제 파일: C:/project/uploads/products/phone.jpg
        registry.addResourceHandler("/uploads/products/**")
                .addResourceLocations("file:" + absolutePath);

        // ========== 프로필 이미지 및 기타 파일 경로 설정 ==========

        // 업로드 경로 끝에 슬래시가 없으면 추가
        String profilePath = uploadPath.endsWith("/") ? uploadPath : uploadPath + "/";

        // 프로필 이미지 디렉토리 생성 (없는 경우)
        File profileDir = new File(profilePath);
        if (!profileDir.exists()) {
            profileDir.mkdirs();
            System.out.println("📁 프로필 디렉토리 생성: " + profilePath);
        }

        // 프로필 디렉토리의 절대 경로 생성
        //File.separator는 현재 운영 체제에서 사용하는 경로 구분자(Directory Separator) 문자열
        String profileAbsolutePath = profileDir.getAbsolutePath() + File.separator;

        // URL 패턴 /uploads/**를 실제 파일 시스템 경로로 매핑
        // /uploads/products/** 보다 더 넓은 범위이므로,
        // 제품 이미지를 제외한 모든 업로드 파일(프로필 이미지 등)을 처리
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + profileAbsolutePath);

        // 디버깅을 위한 로그 출력
        System.out.println("✅ 프로필 리소스 경로: /uploads/** -> " + profileAbsolutePath);

        // 특정 파일 존재 여부 확인 (디버깅용)
        // 실제 운영 환경에서는 제거하는 것이 좋음
        System.out.println("✅ 파일 확인: " + new File(profileAbsolutePath + "jlan1234_1761185348206.jpg").exists());
    }
}