package com.project.gugumarket.controller;

import com.project.gugumarket.dto.ResponseDto;
import com.project.gugumarket.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
@Slf4j
public class AdminController {
    private final AdminService adminService;

    /**
     * 상품 강제 삭제 API
     * @param id 삭제할 상품 ID
     * @return 삭제 결과
     */
    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<ResponseDto<Void>> forceDelete(@PathVariable("id") Long id) {
        try {
            log.info("상품 강제 삭제 요청 - ID: {}", id);

            // 서비스 호출하여 삭제 수행
            adminService.forceDeleteProduct(id);

            // 성공 응답
            return ResponseEntity.ok(
                    ResponseDto.success("상품이 성공적으로 삭제되었습니다.")
            );

        } catch (IllegalArgumentException e) {
            log.warn("상품 삭제 실패 - {}", e.getMessage());
            // 상품을 찾을 수 없는 경우
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ResponseDto.fail(e.getMessage()));

        } catch (Exception e) {
            log.error("상품 삭제 중 오류 발생", e);
            // 기타 예외 처리
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDto.fail("상품 삭제 중 오류가 발생했습니다."));
        }
    }
}
