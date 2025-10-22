package com.project.gugumarket.controller;

import com.project.gugumarket.dto.QnaDto;
import com.project.gugumarket.entity.QnaPost;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.repository.UserRepository;
import com.project.gugumarket.service.QnaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Optional;

@Slf4j
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

    /**
     * Q&A 작성 처리
     * POST /qna/write
     */
    @PostMapping("/write")
    public String write(@Valid @ModelAttribute("qnaDto") QnaDto qnaDto,
                        BindingResult br,
                        Principal principal,
                        RedirectAttributes ra) {

        log.info("Q&A 작성 요청");

        // 1. 폼 검증
        if (br.hasErrors()) {
            log.warn("Q&A 작성 실패 - 유효성 검사 오류");
            return "qna/qnaForm";
        }

        // 2. 로그인 확인
        if (principal == null) {
            log.warn("Q&A 작성 실패 - 로그인 필요");
            ra.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/login";
        }

        try {
            // 3. 저장 (principal.getName() = 로그인 식별자)
            QnaPost saved = qnaService.create(qnaDto, principal.getName());

            log.info("Q&A 작성 완료 - ID: {}", saved.getQnaId());

            // 4. 성공 메시지 + 목록으로
            ra.addFlashAttribute("msg", "문의가 등록되었습니다.");
            return "redirect:/qna/list";

        } catch (Exception e) {
            log.error("Q&A 작성 중 오류 발생", e);
            ra.addFlashAttribute("error", "문의 등록 중 오류가 발생했습니다.");
            return "redirect:/qna/qnaForm";
        }
    }

    /**
     * Q&A 작성 폼 페이지
     * GET /qna/write
     */
    @GetMapping("/write")
    public String writeForm(Model model) {
        log.info("Q&A 작성 폼 페이지 요청");

        if (!model.containsAttribute("qnaDto")) {
            model.addAttribute("qnaDto", new QnaDto());
        }

        return "qna/qnaForm";
    }

}
