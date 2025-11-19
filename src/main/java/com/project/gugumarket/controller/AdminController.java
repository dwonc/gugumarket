package com.project.gugumarket.controller;

import com.project.gugumarket.dto.*;
import com.project.gugumarket.entity.Product;
import com.project.gugumarket.entity.QnaPost;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 관리자 기능을 처리하는 REST API 컨트롤러
 * - 회원 관리 (조회, 상태 변경, 삭제)
 * - 상품 관리 (조회, 검색, 삭제)
 * - Q&A 관리 (조회, 답변 등록)
 * - 통계 데이터 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // ADMIN 역할을 가진 사용자만 접근 가능
public class AdminController {

    private final AdminService adminService;

    /**
     * 관리자 대시보드 통계 조회
     *
     * @return 통계 데이터 (총 회원 수, 상품 수, 미답변 Q&A 수)
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ResponseDto<AdminDashboardDto>> getDashboard() {
        try {
            long totalUsers = adminService.getTotalUsersCount();
            long totalProducts = adminService.getTotalProductsCount();
            long unansweredQna = adminService.getUnansweredQnaCount();

            AdminDashboardDto dashboard = AdminDashboardDto.builder()
                    .totalUsers(totalUsers)
                    .totalProducts(totalProducts)
                    .unansweredQna(unansweredQna)
                    .build();

            return ResponseEntity.ok(ResponseDto.success("통계 조회 성공", dashboard));
        } catch (Exception e) {
            log.error("대시보드 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDto.fail("통계 조회에 실패했습니다."));
        }
    }

    /**
     * 회원 목록 조회
     *
     * @param search 검색 키워드 (선택)
     * @return 회원 목록
     */
    @GetMapping("/users")
    public ResponseEntity<ResponseDto<List<UserListDto>>> getUsers(
            @RequestParam(required = false) String search) {
        try {
            List<User> users = search != null && !search.trim().isEmpty()
                    ? adminService.searchUsers(search)
                    : adminService.getAllUsers();

            // ✅ Entity → DTO 변환
            List<UserListDto> userDtos = users.stream()
                    .map(UserListDto::fromEntity)
                    .toList();

            return ResponseEntity.ok(ResponseDto.success("회원 목록 조회 성공", userDtos));
        } catch (Exception e) {
            log.error("회원 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDto.fail("회원 목록 조회에 실패했습니다."));
        }
    }

    /**
     * 회원 상세 조회
     *
     * @param userId 조회할 회원 ID
     * @return 회원 상세 정보 (기본 정보, 등록 상품, 작성 Q&A)
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<ResponseDto<UserDetailDto>> getUserDetail(@PathVariable Long userId) {
        try {
            User user = adminService.getUserById(userId);
            List<Product> products = adminService.getProductsByUser(userId);
            List<QnaPost> qnaPosts = adminService.getQnaPostsByUser(userId);

            UserDetailDto userDetail = UserDetailDto.fromEntity(user, products, qnaPosts);

            return ResponseEntity.ok(ResponseDto.success("회원 상세 조회 성공", userDetail));
        } catch (Exception e) {
            log.error("회원 상세 조회 실패: userId={}", userId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseDto.fail("회원 정보를 찾을 수 없습니다."));
        }
    }

    /**
     * 회원 상태 토글 (활성/정지)
     *
     * @param userId 상태를 변경할 회원 ID
     * @return 변경된 상태
     */
    @PatchMapping("/users/{userId}/toggle-status")
    public ResponseEntity<ResponseDto<Map<String, Object>>> toggleUserStatus(@PathVariable Long userId) {
        try {
            boolean newStatus = adminService.toggleUserStatus(userId);
            String statusMessage = newStatus ? "활성화" : "정지";

            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("isActive", newStatus);
            result.put("status", statusMessage);

            return ResponseEntity.ok(ResponseDto.success("회원이 " + statusMessage + " 되었습니다.", result));
        } catch (Exception e) {
            log.error("회원 상태 변경 실패: userId={}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDto.fail("회원 상태 변경에 실패했습니다."));
        }
    }

    /**
     * 회원 삭제
     *
     * @param userId 삭제할 회원 ID
     * @return 삭제 결과
     */
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ResponseDto<Void>> deleteUser(@PathVariable Long userId) {
        try {
            adminService.deleteUser(userId);
            return ResponseEntity.ok(ResponseDto.success("회원이 삭제되었습니다."));
        } catch (Exception e) {
            log.error("회원 삭제 실패: userId={}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDto.fail("회원 삭제에 실패했습니다."));
        }
    }

    /**
     * 상품 목록 조회
     *
     * @param search 검색 키워드 (선택)
     * @param isDeleted 삭제 상태 필터 (선택)
     * @return 상품 목록
     */
    @GetMapping("/products")
    public ResponseEntity<ResponseDto<List<ProductSimpleDto>>> getProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isDeleted) {
        try {
            List<Product> products;

            if (search != null && !search.trim().isEmpty()) {
                products = adminService.searchProducts(search);
            } else if (isDeleted != null) {
                products = adminService.getProductsByDeletedStatus(isDeleted);
            } else {
                products = adminService.getAllProducts();
            }

            // ✅ Entity → DTO 변환
            List<ProductSimpleDto> productDtos = products.stream()
                    .map(ProductSimpleDto::fromEntity)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ResponseDto.success("상품 목록 조회 성공", productDtos));
        } catch (Exception e) {
            log.error("상품 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDto.fail("상품 목록 조회에 실패했습니다."));
        }
    }

    /**
     * 상품 삭제
     *
     * @param productId 삭제할 상품 ID
     * @return 삭제 결과
     */
    @DeleteMapping("/products/{productId}")
    public ResponseEntity<ResponseDto<Void>> deleteProduct(@PathVariable Long productId) {
        try {
            adminService.deleteProduct(productId);
            return ResponseEntity.ok(ResponseDto.success("상품이 삭제되었습니다."));
        } catch (Exception e) {
            log.error("상품 삭제 실패: productId={}", productId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDto.fail("상품 삭제에 실패했습니다."));
        }
    }

    /**
     * Q&A 목록 조회 (미답변 우선 정렬)
     *
     * @return Q&A 목록
     */
    @GetMapping("/qna")
    public ResponseEntity<ResponseDto<List<QnaSimpleDto>>> getQnaPosts() {
        try {
            List<QnaPost> qnaPosts = adminService.getAllQnaPostsSortedByAnswered();

            // ✅ Entity → DTO 변환
            List<QnaSimpleDto> qnaDtos = qnaPosts.stream()
                    .map(QnaSimpleDto::fromEntity)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ResponseDto.success("Q&A 목록 조회 성공", qnaDtos));
        } catch (Exception e) {
            log.error("Q&A 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDto.fail("Q&A 목록 조회에 실패했습니다."));
        }
    }

    /**
     * Q&A 답변 등록
     *
     * @param qnaId Q&A ID
     * @param answerRequest 답변 내용
     * @return 답변 등록 결과
     */
    @PostMapping("/qna/{qnaId}/answer")
    public ResponseEntity<ResponseDto<Void>> answerQna(
            @PathVariable Long qnaId,
            @Valid @RequestBody QnaAnswerRequestDto answerRequest) {
        try {
            // ✅ AdminService에서 SecurityContextHolder로 admin 정보를 가져오므로
            // Controller에서는 qnaId와 content만 전달하면 됨!
            adminService.answerQna(qnaId, answerRequest.getContent().trim());
            return ResponseEntity.ok(ResponseDto.success("답변이 등록되었습니다."));
        } catch (IllegalArgumentException e) {
            log.error("Q&A 답변 등록 실패 - 존재하지 않는 Q&A: qnaId={}", qnaId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseDto.fail(e.getMessage()));
        } catch (IllegalStateException e) {
            log.error("Q&A 답변 등록 실패 - {}: qnaId={}", e.getMessage(), qnaId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseDto.fail(e.getMessage()));
        } catch (Exception e) {
            log.error("Q&A 답변 등록 실패: qnaId={}", qnaId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDto.fail("답변 등록에 실패했습니다."));
        }
    }
}