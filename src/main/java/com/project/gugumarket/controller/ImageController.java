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

@RestController //      REST API 컨트롤러임을 표시
@RequestMapping("/api/images")  //      기본 URL 경로 (/api/images로 시작)
@RequiredArgsConstructor        //      final 필드에 대한 생성자 자동 생성
public class ImageController {

    private final FileService fileService;

    /**
     * 단일 이미지 업로드
     */
    @PostMapping("/upload")     //      POST 요청 매핑
    public ResponseEntity<?> upload(
        //     @RequestParam: 요청 파라미터에서 값 추출
        //     required = false : 필수가 아님 ( 없어도 됨 )
        @RequestParam(value = "file", required = false) MultipartFile file,
        @RequestParam(value = "image", required = false) MultipartFile image) {
        try {
            // 🔥 file 또는 image 파라미터 둘 다 지원
            MultipartFile uploadFile = (file != null) ? file : image;

            if (uploadFile == null || uploadFile.isEmpty()) {
                //  파일이 없거나 비어있으면 에러 반환
                return ResponseEntity.badRequest()
                        //  400 BadRequest 상태 코드와 함께 에러메시지 반환
                        .body(Map.of(
                                "success", false,
                                "message", "파일이 비어있습니다."
                        ));
            }

            System.out.println("📤 파일 업로드 시작: " + uploadFile.getOriginalFilename());

            // 파일 업로드
            String savedFileName = fileService.uploadFile(uploadFile);
            //  실제 파일을 서버에 저장
            //  반환값 : 서버에 저장된 파일명 ( UUID_원본파일명.jpg 형식 )

            // 이미지 URL 생성
            String imageUrl = "/uploads/products/" + savedFileName;
            //  frontend 에서 접근할 수 있는 URL 경로 생성
            //  ex) "/uploads/products/+ 파일명.jpg"

            System.out.println("✅ 파일 업로드 성공: " + imageUrl);

            // 응답 데이터
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);      //      성공 여부
            response.put("fileName", savedFileName);    //      저장된 파일명
            response.put("imageUrl", imageUrl); //      이미지 URL
            response.put("url", imageUrl);  // 🔥 url 필드 추가 (호환성)
            response.put("message", "이미지 업로드 성공");      //      성공 메시지

            return ResponseEntity.ok(response);
            // 결과: 
            // {
            //   "success": true,
            //   "fileName": "abc123_image.jpg",
            //   "imageUrl": "/uploads/products/abc123_image.jpg",
            //   "url": "/uploads/products/abc123_image.jpg",
            //   "message": "이미지 업로드 성공"
            // }


            //  에러 처리
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
    @PostMapping("/upload-multiple")    //      POST 요청 매핑
    public ResponseEntity<?> uploadMultiple(@RequestParam("files") List<MultipartFile> files) {
                // "files" 라는 이름의 파라미터
                // 여러 파일을 리스트로 받기
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
            //  여러 파일을 한 번에 저장 -> 반환값은 저장된 파일명들의 리스트

            // 이미지 URL 리스트 생성
            List<String> imageUrls = savedFileNames.stream() // stream API를 사용해서 각 파일명을 URL로 변환
                    .map(fileName -> "/uploads/products/" + fileName)   
                    // map : 각 요소를 변환
                    // fileName -> "/uploads/products/" + fileName
                    .collect(Collectors.toList());
                    // 결과를 리스트로 수집

                // 응답 데이터
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);      // 성공여부
                response.put("fileNames", savedFileNames);  // 파일명 리스트
                response.put("imageUrls", imageUrls);       // URL 리스트
                response.put("count", savedFileNames.size());       //업로드 된 개수
                response.put("message",savedFileNames.size() + "개의 이미지 업로드 성공");

            return ResponseEntity.ok(response);
            // 결과:
            // {
            //   "success": true,
            //   "fileNames": ["abc123_1.jpg", "def456_2.jpg"],
            //   "imageUrls": ["/uploads/products/abc123_1.jpg", "/uploads/products/def456_2.jpg"],
            //   "count": 2,
            //   "message": "2개의 이미지 업로드 성공"
            // }

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
    public ResponseEntity<?> delete(@PathVariable String fileName) {    // DELETE 요청 매핑
                                // @PathVariable : URL 경로에서 값 추출
        try {
            fileService.deleteFile(fileName);   // 서버 디스크에서 파일 삭제

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