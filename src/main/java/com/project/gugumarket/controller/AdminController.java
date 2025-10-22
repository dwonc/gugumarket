package com.project.gugumarket.controller;

import com.project.gugumarket.dto.ResponseDto;
import com.project.gugumarket.entity.Product;
import com.project.gugumarket.entity.QnaPost;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관리자 컨트롤러
 *
 * 특징:
 * - GET /admin → admin/admin.html (하나의 페이지)
 * - JavaScript 탭 전환으로 회원/상품/Q&A 관리
 * - 모든 데이터를 한 번에 Model에 담아서 전달
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    /**
     * 관리자 메인 페이지 (통합)
     * GET /admin
     *
     * 반환: admin/admin.html
     *
     * Model 데이터:
     * - totalUsers: 총 회원 수
     * - totalProducts: 총 상품 수
     * - unansweredQna: 미답변 Q&A 수
     * - users: 회원 목록
     * - products: 상품 목록
     * - qnaPosts: Q&A 목록
     */
    @GetMapping
    public String dashboard(Model model) {
        log.info("========== 관리자 페이지 접속 ==========");

        // ===== 1. 통계 데이터 =====
        Long totalUsers = adminService.getTotalUsers();
        Long totalProducts = adminService.getTotalProducts();
        Long unansweredQna = adminService.getPendingQnaCount();  // ⚠️ HTML에서 unansweredQna 사용

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("unansweredQna", unansweredQna);  // ⚠️ 이름 주의!

        log.info("📊 통계 - 회원: {}명, 상품: {}개, 미답변 Q&A: {}개",
                totalUsers, totalProducts, unansweredQna);

        // ===== 2. 회원 목록 (최대 100명) =====
        List<User> users = adminService.getUserListForAdmin();
        model.addAttribute("users", users);
        log.info("👥 회원 {}명 조회", users.size());

        // ===== 3. 상품 목록 (최대 100개) =====
        List<Product> products = adminService.getProductListForAdmin();
        model.addAttribute("products", products);
        log.info("📦 상품 {}개 조회", products.size());

        // ===== 4. Q&A 목록 (최대 50개) =====
        List<QnaPost> qnaPosts = adminService.getQnaListForAdmin();
        model.addAttribute("qnaPosts", qnaPosts);
        log.info("💬 Q&A {}개 조회", qnaPosts.size());

        log.info("========================================");

        return "admin/admin";  // admin/admin.html 반환
    }

    // ==================== API 엔드포인트 (JSON 응답) ==================== //

    /**
     * 회원 상태 변경 API
     * PUT /admin/users/{id}/status
     */
    @PutMapping("/users/{id}/status")
    @ResponseBody
    public ResponseEntity<ResponseDto<Void>> changeUserStatus(
            @PathVariable("id") Long id,
            @RequestBody UserStatusRequest request
    ) {
        try {
            log.info("회원 상태 변경 요청 - ID: {}, 상태: {}", id, request.getStatus());

            adminService.changeUserStatus(id, request.getStatus());

            return ResponseEntity.ok(
                    ResponseDto.success("회원 상태가 변경되었습니다.")
            );

        } catch (IllegalArgumentException e) {
            log.warn("회원 상태 변경 실패 - {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ResponseDto.fail(e.getMessage()));

        } catch (Exception e) {
            log.error("회원 상태 변경 중 오류 발생", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDto.fail("회원 상태 변경 중 오류가 발생했습니다."));
        }
    }

    /**
     * 상품 강제 삭제 API
     * DELETE /admin/products/{id}
     */
    @DeleteMapping("/products/{id}")
    @ResponseBody
    public ResponseEntity<ResponseDto<Void>> forceDelete(@PathVariable("id") Long id) {
        try {
            log.info("상품 강제 삭제 요청 - ID: {}", id);

            adminService.forceDeleteProduct(id);

            return ResponseEntity.ok(
                    ResponseDto.success("상품이 성공적으로 삭제되었습니다.")
            );

        } catch (IllegalArgumentException e) {
            log.warn("상품 삭제 실패 - {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ResponseDto.fail(e.getMessage()));

        } catch (Exception e) {
            log.error("상품 삭제 중 오류 발생", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDto.fail("상품 삭제 중 오류가 발생했습니다."));
        }
    }

    /**
     * Q&A 답변 작성 API
     * POST /admin/qna/answer
     *
     * HTML form action: /admin/qna/answer
     */
    @PostMapping("/qna/answer")
    public String answerQna(
            @RequestParam("qnaId") Long qnaId,
            @RequestParam("content") String content
    ) {
        try {
            log.info("Q&A 답변 작성 요청 - QNA ID: {}", qnaId);

            adminService.createQnaAnswer(qnaId, content);

            log.info("✅ Q&A 답변 작성 완료");

        } catch (Exception e) {
            log.error("Q&A 답변 작성 중 오류 발생", e);
        }

        // 답변 작성 후 관리자 페이지로 리다이렉트
        return "redirect:/admin";
    }

    // ==================== 내부 클래스 (Request DTO) ==================== //

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UserStatusRequest {
        private String status;
    }
}