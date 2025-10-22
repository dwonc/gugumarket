package com.project.gugumarket.controller;

import com.project.gugumarket.entity.QnaPost;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.UserRepository;
import com.project.gugumarket.service.QnaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
@RequestMapping("/qna")
public class QnaController {

    private final QnaService qnaService;
    private final UserRepository userRepository;

    /**
     * QnA 목록 (페이징 + 검색)
     */
    @GetMapping("/list")
    public String list(
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword
    ) {
        System.out.println("========== QnA 목록 ==========");
        System.out.println("📄 페이지: " + page + ", 사이즈: " + size);
        System.out.println("🔍 검색어: " + keyword);

        // 현재 로그인 사용자
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        if (!"anonymousUser".equals(username)) {
            Optional<User> userOpt = userRepository.findByUserName(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                model.addAttribute("user", user);
            }
        }

        // 페이징 설정
        Pageable pageable = PageRequest.of(page, size);

        // 검색 실행
        Page<QnaPost> qnaPosts = qnaService.searchQna(keyword, pageable);

        // Model에 데이터 추가
        model.addAttribute("qnaPosts", qnaPosts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", qnaPosts.getTotalPages());
        model.addAttribute("totalElements", qnaPosts.getTotalElements());
        model.addAttribute("keyword", keyword);

        System.out.println("✅ QnA " + qnaPosts.getContent().size() + "개 조회");
        System.out.println("================================");

        return "qna/list";  // ⭐ qna_list → list 수정
    }

    // 여기 아래에 기존 메서드들 (detail, create, update, delete) 추가
}