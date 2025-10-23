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
     * 절대 경로 가져오기
     */
    private String getAbsolutePath() {
        return new File(uploadDir).getAbsolutePath();
    }

    /**
     * 업로드 디렉토리 생성
     */
    private void createUploadDirectory() {
        String absolutePath = getAbsolutePath();
        File uploadPath = new File(absolutePath);

        if (!uploadPath.exists()) {
            boolean created = uploadPath.mkdirs();
            System.out.println("📁 업로드 디렉토리 생성: " + absolutePath + " (성공: " + created + ")");
        }
    }

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

        // 🔥 업로드 디렉토리 생성 (절대 경로)
        createUploadDirectory();

        // UUID로 고유한 파일명 생성
        String savedFileName = UUID.randomUUID().toString() + extension;

        // 🔥 절대 경로로 파일 저장
        String absolutePath = getAbsolutePath();
        String filePath = absolutePath + File.separator + savedFileName;

        System.out.println("💾 파일 저장 시작: " + originalFilename);
        System.out.println("📂 저장 경로: " + filePath);

        // 파일 저장
        File dest = new File(filePath);
        try {
            file.transferTo(dest);
            System.out.println("✅ 파일 저장 성공: " + savedFileName);
        } catch (IOException e) {
            System.err.println("❌ 파일 저장 실패: " + e.getMessage());
            throw new IOException("파일 저장 중 오류가 발생했습니다.", e);
        }

        return savedFileName;
    }

    /**
     * 파일 삭제
     * @param fileName 삭제할 파일명
     */
    public void deleteFile(String fileName) throws IOException {
        if (fileName == null || fileName.isEmpty()) {
            return;
        }

        try {
            // 🔥 절대 경로 사용
            String absolutePath = getAbsolutePath();
            Path filePath = Paths.get(absolutePath, fileName);

            boolean deleted = Files.deleteIfExists(filePath);

            if (deleted) {
                System.out.println("🗑️ 파일 삭제 완료: " + fileName);
            } else {
                System.out.println("⚠️ 파일이 존재하지 않음: " + fileName);
            }
        } catch (IOException e) {
            System.err.println("❌ 파일 삭제 실패: " + e.getMessage());
            throw e;
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

        System.out.println("✅ 총 " + savedFileNames.size() + "개 파일 업로드 완료");
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

        // 🔥 절대 경로 사용
        String absolutePath = getAbsolutePath();
        File file = new File(absolutePath + File.separator + fileName);
        return file.exists();
    }
}