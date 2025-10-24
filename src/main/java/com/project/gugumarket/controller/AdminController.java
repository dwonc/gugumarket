package com.project.gugumarket.controller;

import com.project.gugumarket.entity.Product;
import com.project.gugumarket.entity.QnaPost;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * 관리자 기능을 처리하는 컨트롤러
 * - 회원 관리 (조회, 상태 변경, 삭제)
 * - 상품 관리 (조회, 검색, 삭제)
 * - Q&A 관리 (조회, 답변 등록)
 * - 통계 데이터 제공
 */
@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // ADMIN 역할을 가진 사용자만 접근 가능
public class AdminController {

    private final AdminService adminService;

    /**
     * 관리자 메인 페이지
     * - 통계 데이터 표시 (총 회원 수, 상품 수, 미답변 Q&A 수)
     * - 회원 목록 조회 및 검색
     * - 상품 목록 조회, 검색, 삭제 상태 필터링
     * - Q&A 목록 조회 (미답변 우선 정렬)
     *
     * @param userSearch 회원 검색 키워드
     * @param productSearch 상품 검색 키워드
     * @param isDeleted 상품 삭제 상태 필터 (true: 삭제된 상품, false: 활성 상품)
     * @param tab 활성화할 탭 (users/products/qna)
     * @param model 뷰에 전달할 데이터
     * @return 관리자 메인 페이지 뷰
     */
    @GetMapping
    public String adminPage(
            @RequestParam(required = false) String userSearch,
            @RequestParam(required = false) String productSearch,
            @RequestParam(required = false) Boolean isDeleted,
            @RequestParam(required = false) String tab,
            Model model
    ) {
        // 통계 데이터 조회
        long totalUsers = adminService.getTotalUsersCount();
        long totalProducts = adminService.getTotalProductsCount();
        long unansweredQna = adminService.getUnansweredQnaCount();

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("unansweredQna", unansweredQna);

        // 회원 목록 조회 (검색어가 있으면 검색, 없으면 전체 조회)
        List<User> users = userSearch != null && !userSearch.trim().isEmpty()
                ? adminService.searchUsers(userSearch)
                : adminService.getAllUsers();
        model.addAttribute("users", users);

        // 상품 목록 조회 (검색어 또는 삭제 상태 필터 적용)
        List<Product> products;
        if (productSearch != null && !productSearch.trim().isEmpty()) {
            products = adminService.searchProducts(productSearch);
        } else if (isDeleted != null) {
            products = adminService.getProductsByDeletedStatus(isDeleted);
        } else {
            products = adminService.getAllProducts();
        }
        model.addAttribute("products", products);

        // Q&A 목록 조회 (미답변 게시글이 먼저 표시되도록 정렬)
        List<QnaPost> qnaPosts = adminService.getAllQnaPostsSortedByAnswered();
        model.addAttribute("qnaPosts", qnaPosts);

        // 현재 활성화된 탭 정보 유지 (페이지 새로고침 시에도 같은 탭 유지)
        if (tab != null) {
            model.addAttribute("activeTab", tab);
        }

        return "admin/admin";
    }

    /**
     * 회원 상세 페이지
     * - 회원 기본 정보
     * - 회원이 등록한 상품 목록
     * - 회원이 작성한 Q&A 게시글 목록
     * - 상품 및 Q&A 작성 수
     *
     * @param userId 조회할 회원 ID
     * @param model 뷰에 전달할 데이터
     * @param redirectAttributes 리다이렉트 시 전달할 메시지
     * @return 회원 상세 페이지 뷰 또는 관리자 메인으로 리다이렉트
     */
    @GetMapping("/users/{userId}")
    public String userDetailPage(@PathVariable Long userId, Model model, RedirectAttributes redirectAttributes) {
        try {
            // 회원 정보 및 관련 데이터 조회
            User user = adminService.getUserById(userId);
            List<Product> products = adminService.getProductsByUser(userId);
            List<QnaPost> qnaPosts = adminService.getQnaPostsByUser(userId);

            long productCount = products.size();
            long qnaCount = qnaPosts.size();

            model.addAttribute("user", user);
            model.addAttribute("products", products);
            model.addAttribute("qnaPosts", qnaPosts);
            model.addAttribute("productCount", productCount);
            model.addAttribute("qnaCount", qnaCount);

            return "admin/user-detail";
        } catch (Exception e) {
            log.error("회원 상세 조회 실패: userId={}", userId, e);
            redirectAttributes.addFlashAttribute("error", "회원 정보를 불러올 수 없습니다.");
            return "redirect:/admin";
        }
    }

    /**
     * 회원 상태 토글 (활성/정지)
     * - 현재 상태를 반대로 전환
     * - 활성 회원 → 정지, 정지 회원 → 활성
     *
     * @param userId 상태를 변경할 회원 ID
     * @param redirectAttributes 리다이렉트 시 전달할 메시지
     * @return 회원 상세 페이지로 리다이렉트
     */
    @PostMapping("/users/{userId}/toggle-status")
    public String toggleUserStatus(@PathVariable Long userId, RedirectAttributes redirectAttributes) {
        try {
            // 상태 변경 후 새로운 상태값 반환 (true: 활성, false: 정지)
            boolean newStatus = adminService.toggleUserStatus(userId);
            String statusMessage = newStatus ? "활성화" : "정지";
            redirectAttributes.addFlashAttribute("message", "회원이 " + statusMessage + " 되었습니다.");
        } catch (Exception e) {
            log.error("회원 상태 변경 실패: userId={}", userId, e);
            redirectAttributes.addFlashAttribute("error", "회원 상태 변경에 실패했습니다.");
        }
        return "redirect:/admin/users/" + userId;
    }

    /**
     * 회원 삭제
     * - 회원 계정 및 관련 데이터 삭제
     * - 삭제 후 관리자 메인 페이지로 이동
     *
     * @param userId 삭제할 회원 ID
     * @param redirectAttributes 리다이렉트 시 전달할 메시지
     * @return 관리자 메인 페이지 또는 회원 상세 페이지로 리다이렉트
     */
    @PostMapping("/users/{userId}/delete")
    public String deleteUser(@PathVariable Long userId, RedirectAttributes redirectAttributes) {
        try {
            adminService.deleteUser(userId);
            redirectAttributes.addFlashAttribute("message", "회원이 삭제되었습니다.");
            return "redirect:/admin";
        } catch (Exception e) {
            log.error("회원 삭제 실패: userId={}", userId, e);
            redirectAttributes.addFlashAttribute("error", "회원 삭제에 실패했습니다.");
            return "redirect:/admin/users/" + userId;
        }
    }

    /**
     * 상품 삭제
     * - 상품을 삭제 처리
     * - 삭제 후 관리자 메인 페이지의 상품 탭으로 이동
     *
     * @param productId 삭제할 상품 ID
     * @param redirectAttributes 리다이렉트 시 전달할 메시지
     * @return 관리자 메인 페이지(상품 탭)로 리다이렉트
     */
    @PostMapping("/products/{productId}/delete")
    public String deleteProduct(@PathVariable Long productId, RedirectAttributes redirectAttributes) {
        try {
            adminService.deleteProduct(productId);
            redirectAttributes.addFlashAttribute("message", "상품이 삭제되었습니다.");
        } catch (Exception e) {
            log.error("상품 삭제 실패: productId={}", productId, e);
            redirectAttributes.addFlashAttribute("error", "상품 삭제에 실패했습니다.");
        }
        return "redirect:/admin?tab=products";
    }

    /**
     * Q&A 답변 등록
     * - 관리자가 Q&A 게시글에 답변 작성
     * - 답변 내용 유효성 검사 (빈 값 체크)
     * - 등록 후 관리자 메인 페이지의 Q&A 탭으로 이동
     *
     * @param qnaId 답변을 등록할 Q&A 게시글 ID
     * @param content 답변 내용
     * @param redirectAttributes 리다이렉트 시 전달할 메시지
     * @return 관리자 메인 페이지(Q&A 탭)로 리다이렉트
     */
    @PostMapping("/qna/answer")
    public String answerQna(
            @RequestParam Long qnaId,
            @RequestParam String content,
            RedirectAttributes redirectAttributes
    ) {
        try {
            // 답변 내용이 비어있는지 검증
            if (content == null || content.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "답변 내용을 입력해주세요.");
                return "redirect:/admin?tab=qna";
            }

            // 답변 등록 (앞뒤 공백 제거)
            adminService.answerQna(qnaId, content.trim());
            redirectAttributes.addFlashAttribute("message", "답변이 등록되었습니다.");
        } catch (Exception e) {
            log.error("Q&A 답변 등록 실패: qnaId={}", qnaId, e);
            redirectAttributes.addFlashAttribute("error", "답변 등록에 실패했습니다.");
        }
        return "redirect:/admin?tab=qna";
    }
}