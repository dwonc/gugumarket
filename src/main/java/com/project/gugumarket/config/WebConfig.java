package com.project.gugumarket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:uploads/products}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 🔥 절대 경로로 변환
        String absolutePath = new File(uploadDir).getAbsolutePath() + File.separator;

        // 🔥 디렉토리가 없으면 생성
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            System.out.println("📁 업로드 디렉토리 생성: " + absolutePath + " (성공: " + created + ")");
        }

        System.out.println("📂 정적 리소스 경로 설정: " + absolutePath);

        // 업로드된 이미지를 /uploads/products/** URL로 접근 가능하게 설정
        registry.addResourceHandler("/uploads/products/**")
                .addResourceLocations("file:" + absolutePath);
    }
}