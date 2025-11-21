package com.project.gugumarket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:uploads/products}")
    private String uploadDir;

    @Value("${file.upload.path:uploads/}")
    private String uploadPath;
    // 🔥 CORS 설정 추가
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        System.out.println("✅ CORS 설정 완료: http://localhost:5173");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 제품 이미지 경로
        String absolutePath = new File(uploadDir).getAbsolutePath() + File.separator;
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            System.out.println("📁 업로드 디렉토리 생성: " + absolutePath + " (성공: " + created + ")");
        }
        System.out.println("📂 제품 리소스 경로: " + absolutePath);

        registry.addResourceHandler("/uploads/products/**")
                .addResourceLocations("file:" + absolutePath);

        // 프로필 이미지 경로 (실제 파일이 저장된 위치)
        String profilePath = uploadPath.endsWith("/") ? uploadPath : uploadPath + "/";
        File profileDir = new File(profilePath);
        if (!profileDir.exists()) {
            profileDir.mkdirs();
            System.out.println("📁 프로필 디렉토리 생성: " + profilePath);
        }

        String profileAbsolutePath = profileDir.getAbsolutePath() + File.separator;

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + profileAbsolutePath);

        System.out.println("✅ 프로필 리소스 경로: /uploads/** -> " + profileAbsolutePath);
        System.out.println("✅ 파일 확인: " + new File(profileAbsolutePath + "jlan1234_1761185348206.jpg").exists());
    }
}