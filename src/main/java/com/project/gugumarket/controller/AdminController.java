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

@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    /**
     * 관리자 메인 페이지
     */
    @GetMapping
    public String adminPage(
            @RequestParam(required = false) String userSearch,
            @RequestParam(required = false) String productSearch,
            @RequestParam(required = false) Boolean isDeleted,
            @RequestParam(required = false) String tab,
            Model model
    ) {
        // 통계 데이터
        long totalUsers = adminService.getTotalUsersCount();
        long totalProducts = adminService.getTotalProductsCount();
        long unansweredQna = adminService.getUnansweredQnaCount();

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("unansweredQna", unansweredQna);

        // 회원 목록 (검색 포함)
        List<User> users = userSearch != null && !userSearch.trim().isEmpty()
                ? adminService.searchUsers(userSearch)
                : adminService.getAllUsers();
        model.addAttribute("users", users);

        // 상품 목록 (검색 및 필터 포함)
        List<Product> products;
        if (productSearch != null && !productSearch.trim().isEmpty()) {
            products = adminService.searchProducts(productSearch);
        } else if (isDeleted != null) {
            products = adminService.getProductsByDeletedStatus(isDeleted);
        } else {
            products = adminService.getAllProducts();
        }
        model.addAttribute("products", products);

        // Q&A 목록 (미답변 우선)
        List<QnaPost> qnaPosts = adminService.getAllQnaPostsSortedByAnswered();
        model.addAttribute("qnaPosts", qnaPosts);

        // 탭 유지
        if (tab != null) {
            model.addAttribute("activeTab", tab);
        }

        return "admin/admin";
    }

    /**
     * 회원 상세 페이지
     */
    @GetMapping("/users/{userId}")
    public String userDetailPage(@PathVariable Long userId, Model model, RedirectAttributes redirectAttributes) {
        try {
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
     */
    @PostMapping("/users/{userId}/toggle-status")
    public String toggleUserStatus(@PathVariable Long userId, RedirectAttributes redirectAttributes) {
        try {
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
     */
    @PostMapping("/qna/answer")
    public String answerQna(
            @RequestParam Long qnaId,
            @RequestParam String content,
            RedirectAttributes redirectAttributes
    ) {
        try {
            if (content == null || content.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "답변 내용을 입력해주세요.");
                return "redirect:/admin?tab=qna";
            }

            adminService.answerQna(qnaId, content.trim());
            redirectAttributes.addFlashAttribute("message", "답변이 등록되었습니다.");
        } catch (Exception e) {
            log.error("Q&A 답변 등록 실패: qnaId={}", qnaId, e);
            redirectAttributes.addFlashAttribute("error", "답변 등록에 실패했습니다.");
        }
        return "redirect:/admin?tab=qna";
    }
}