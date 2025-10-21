package com.project.gugumarket.controller;

import com.project.gugumarket.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final FileService fileService;

    /**
     * 단일 이미지 업로드
     */
    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam(value = "file", required = false) MultipartFile file,
                                    @RequestParam(value = "image", required = false) MultipartFile image) {
        try {
            // 🔥 file 또는 image 파라미터 둘 다 지원
            MultipartFile uploadFile = (file != null) ? file : image;

            if (uploadFile == null || uploadFile.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message", "파일이 비어있습니다."
                        ));
            }

            System.out.println("📤 파일 업로드 시작: " + uploadFile.getOriginalFilename());

            // 파일 업로드
            String savedFileName = fileService.uploadFile(uploadFile);

            // 이미지 URL 생성
            String imageUrl = "/uploads/products/" + savedFileName;

            System.out.println("✅ 파일 업로드 성공: " + imageUrl);

            // 응답 데이터
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("fileName", savedFileName);
            response.put("imageUrl", imageUrl);
            response.put("url", imageUrl);  // 🔥 url 필드 추가 (호환성)
            response.put("message", "이미지 업로드 성공");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            System.err.println("❌ 잘못된 요청: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        } catch (IOException e) {
            System.err.println("❌ 업로드 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "이미지 업로드 중 오류가 발생했습니다: " + e.getMessage()
                    ));
        }
    }

    /**
     * 여러 이미지 업로드
     */
    @PostMapping("/upload-multiple")
    public ResponseEntity<?> uploadMultiple(@RequestParam("files") List<MultipartFile> files) {
        try {
            // 파일 개수 제한 (예: 최대 5개)
            if (files.size() > 5) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message", "최대 5개의 이미지만 업로드 가능합니다."
                        ));
            }

            // 파일들 업로드
            List<String> savedFileNames = fileService.uploadFiles(files);

            // 이미지 URL 리스트 생성
            List<String> imageUrls = savedFileNames.stream()
                    .map(fileName -> "/uploads/products/" + fileName)
                    .collect(Collectors.toList());

            // 응답 데이터
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("fileNames", savedFileNames);
            response.put("imageUrls", imageUrls);
            response.put("count", savedFileNames.size());
            response.put("message", savedFileNames.size() + "개의 이미지 업로드 성공");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "이미지 업로드 중 오류가 발생했습니다: " + e.getMessage()
                    ));
        }
    }

    /**
     * 이미지 삭제
     */
    @DeleteMapping("/{fileName}")
    public ResponseEntity<?> delete(@PathVariable String fileName) {
        try {
            fileService.deleteFile(fileName);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "이미지 삭제 성공"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "이미지 삭제 중 오류가 발생했습니다: " + e.getMessage()
                    ));
        }
    }
}