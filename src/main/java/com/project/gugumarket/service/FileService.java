package com.project.gugumarket.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileService {

    @Value("${file.upload-dir:uploads/products}")
    private String uploadDir;

    // 허용된 이미지 확장자
    private static final List<String> ALLOWED_EXTENSIONS =
            Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp");

    // 최대 파일 크기 (10MB)
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * 파일 업로드
     * @param file 업로드할 파일
     * @return 저장된 파일명
     */
    public String uploadFile(MultipartFile file) throws IOException {
        // 파일이 비어있는지 확인
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        // 파일 크기 검증
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("파일 크기는 10MB를 초과할 수 없습니다.");
        }

        // 파일 타입 검증 (이미지만 허용)
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
        }

        // 원본 파일명과 확장자 추출
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("유효하지 않은 파일명입니다.");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();

        // 파일 확장자 검증
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다. (jpg, jpeg, png, gif, webp만 가능)");
        }

        // 업로드 디렉토리 생성
        File uploadPath = new File(uploadDir);
        if (!uploadPath.exists()) {
            uploadPath.mkdirs();
        }

        // UUID로 고유한 파일명 생성
        String savedFileName = UUID.randomUUID().toString() + extension;

        // 파일 저장 경로
        String filePath = uploadDir + File.separator + savedFileName;

        // 파일 저장
        File dest = new File(filePath);
        file.transferTo(dest);

        return savedFileName;
    }

    /**
     * 파일 삭제
     * @param fileName 삭제할 파일명
     */
    public void deleteFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return;
        }

        try {
            Path filePath = Paths.get(uploadDir, fileName);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // 로그만 남기고 계속 진행
            System.err.println("파일 삭제 실패: " + e.getMessage());
        }
    }

    /**
     * 여러 파일 업로드
     * @param files 업로드할 파일들
     * @return 저장된 파일명 리스트
     */
    public List<String> uploadFiles(List<MultipartFile> files) throws IOException {
        List<String> savedFileNames = new ArrayList<>();

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String savedFileName = uploadFile(file);
                savedFileNames.add(savedFileName);
            }
        }

        return savedFileNames;
    }

    /**
     * 파일 존재 여부 확인
     * @param fileName 확인할 파일명
     * @return 존재 여부
     */
    public boolean fileExists(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }

        File file = new File(uploadDir + File.separator + fileName);
        return file.exists();
    }
}