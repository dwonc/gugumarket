//package com.project.gugumarket.controller;
//
//import com.project.gugumarket.dto.QnaDto;
//import com.project.gugumarket.entity.QnaPost;
//import com.project.gugumarket.entity.User;
//import com.project.gugumarket.repository.UserRepository;
//import com.project.gugumarket.service.QnaService;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.validation.BindingResult;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.servlet.mvc.support.RedirectAttributes;
//
//import java.security.Principal;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Optional;
//
//@Slf4j
//@Controller
//@RequiredArgsConstructor
//@RequestMapping("/qna")
//public class QnaController {
//
//    private final QnaService qnaService;
//    private final UserRepository userRepository;
//
//    /**
//     * QnA 목록 (페이징 + 검색)
//     */
//    @GetMapping("/list")
//    public String list(
//            Model model,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size,
//            @RequestParam(required = false) String keyword
//    ) {
//        System.out.println("========== QnA 목록 ==========");
//        System.out.println("📄 페이지: " + page + ", 사이즈: " + size);
//        System.out.println("🔍 검색어: " + keyword);
//
//        // 현재 로그인 사용자
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        String username = authentication.getName();
//
//        if (!"anonymousUser".equals(username)) {
//            Optional<User> userOpt = userRepository.findByUserName(username);
//            if (userOpt.isPresent()) {
//                User user = userOpt.get();
//                model.addAttribute("user", user);
//            }
//        }
//
//        // 페이징 설정
//        Pageable pageable = PageRequest.of(page, size);
//
//        // 검색 실행
//        Page<QnaPost> qnaPosts = qnaService.searchQna(keyword, pageable);
//
//        // Model에 데이터 추가
//        model.addAttribute("qnaPosts", qnaPosts);
//        model.addAttribute("currentPage", page);
//        model.addAttribute("totalPages", qnaPosts.getTotalPages());
//        model.addAttribute("totalElements", qnaPosts.getTotalElements());
//        model.addAttribute("keyword", keyword);
//
//        System.out.println("✅ QnA " + qnaPosts.getContent().size() + "개 조회");
//        System.out.println("================================");
//
//        return "qna/list";  // ⭐ qna_list → list 수정
//    }
//
//    // 여기 아래에 기존 메서드들 (detail, create, update, delete) 추가
//
//    /**
//     * Q&A 작성 처리
//     * POST /qna/write
//     */
//    @PostMapping("/write")
//    public String write(@Valid @ModelAttribute("qnaDto") QnaDto qnaDto,
//                        BindingResult br,
//                        Principal principal,
//                        RedirectAttributes ra) {
//
//        log.info("Q&A 작성 요청");
//
//        // 1. 폼 검증
//        if (br.hasErrors()) {
//            log.warn("Q&A 작성 실패 - 유효성 검사 오류");
//            return "qna/qnaForm";
//        }
//
//        // 2. 로그인 확인
//        if (principal == null) {
//            log.warn("Q&A 작성 실패 - 로그인 필요");
//            ra.addFlashAttribute("error", "로그인이 필요합니다.");
//            return "redirect:/login";
//        }
//
//        try {
//            // 3. 저장 (principal.getName() = 로그인 식별자)
//            QnaPost saved = qnaService.create(qnaDto, principal.getName());
//
//            log.info("Q&A 작성 완료 - ID: {}", saved.getQnaId());
//
//            // 4. 성공 메시지 + 목록으로
//            ra.addFlashAttribute("msg", "문의가 등록되었습니다.");
//            return "redirect:/qna/list";
//
//        } catch (Exception e) {
//            log.error("Q&A 작성 중 오류 발생", e);
//            ra.addFlashAttribute("error", "문의 등록 중 오류가 발생했습니다.");
//            return "redirect:/qna/qnaForm";
//        }
//    }
//
//    /**
//     * Q&A 작성 폼 페이지
//     * GET /qna/write
//     */
//    @GetMapping("/write")
//    public String writeForm(Model model) {
//        log.info("Q&A 작성 폼 페이지 요청");
//
//        if (!model.containsAttribute("qnaDto")) {
//            model.addAttribute("qnaDto", new QnaDto());
//        }
//
//        return "qna/qnaForm";
//    }
//
//}

package com.project.gugumarket.controller;

import com.project.gugumarket.dto.QnaDto;
import com.project.gugumarket.dto.UserResponseDto;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import com.project.gugumarket.dto.QnaResponseDto;  // <-- qnawrite 무한루프 다시 생겨서 추가함

        import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController  // 👈 변경: @Controller → @RestController
@RequiredArgsConstructor
@RequestMapping("/qna")  // 👈 주소 그대로 유지
public class QnaController {

    private final QnaService qnaService;
    private final UserRepository userRepository;

    @GetMapping("/list")
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword
    ) {
        System.out.println("========== QnA 목록 ==========");
        System.out.println("📄 페이지: " + page + ", 사이즈: " + size);
        System.out.println("🔍 검색어: " + keyword);

        try {
            // 현재 로그인 사용자
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            UserResponseDto userDto = null;  // 👈 변경: User → UserResponseDto // User 대신 DTO
            if (!"anonymousUser".equals(username)) {
                Optional<User> userOpt = userRepository.findByUserName(username);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    userDto = UserResponseDto.fromEntity(user);  // 👈 추가: DTO로 변환 // 필요한 정보만 복사
                }
            }

            // 페이징 설정
            Pageable pageable = PageRequest.of(page, size);

            // 검색 실행
            Page<QnaPost> qnaPosts = qnaService.searchQna(keyword, pageable);

            System.out.println("✅ QnA " + qnaPosts.getContent().size() + "개 조회");
            System.out.println("================================");


            // DTO로 변환 (무한루프해결용)
            List<QnaResponseDto> qnaPostDtos = qnaPosts.getContent().stream()
                    .map(QnaResponseDto::fromEntity)
                    .toList();

            // JSON 응답 데이터 구성
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("qnaPosts", qnaPostDtos);
            response.put("currentPage", page);
            response.put("totalPages", qnaPosts.getTotalPages());
            response.put("totalElements", qnaPosts.getTotalElements());
            response.put("keyword", keyword);
            response.put("user", userDto);  // 👈 변경: user → userDto // products 없는 깨끗한 데이터 (무한루프해결용)

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("QnA 목록 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "목록 조회 중 오류가 발생했습니다: " + e.getMessage()
                    ));
        }
    }

    /**
     * Q&A 작성 처리
     * POST /qna/write
     */
    @PostMapping("/write")
    public ResponseEntity<?> write(  // 👈 변경: String → ResponseEntity<?>
                                     @Valid @RequestBody QnaDto qnaDto,  // 👈 변경: @ModelAttribute → @RequestBody
                                     BindingResult br,
                                     Principal principal
    ) {
        log.info("Q&A 작성 요청");

        // 1. 폼 검증
        if (br.hasErrors()) {
            log.warn("Q&A 작성 실패 - 유효성 검사 오류");
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", "입력값이 올바르지 않습니다.",
                            "errors", br.getAllErrors()
                    ));
        }

        // 2. 로그인 확인
        if (principal == null) {
            log.warn("Q&A 작성 실패 - 로그인 필요");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "message", "로그인이 필요합니다."
                    ));
        }

        try {
            // 3. 저장
            QnaPost saved = qnaService.create(qnaDto, principal.getName());

            // 👇 DTO로 변환 (이 줄 추가!)
            QnaResponseDto responseDto = QnaResponseDto.fromEntity(saved);

            log.info("Q&A 작성 완료 - ID: {}", saved.getQnaId());

            // 4. 성공 응답
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "문의가 등록되었습니다.",
                    "qnaId", saved.getQnaId(),
                    "qnaPost", responseDto  //  saved → responseDto 로 변경
            ));

        } catch (Exception e) {
            log.error("Q&A 작성 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "문의 등록 중 오류가 발생했습니다: " + e.getMessage()
                    ));
        }
    }

    /**
     * Q&A 작성 폼 정보 (선택적)
     * GET /qna/write
     */
    @GetMapping("/write")
    public ResponseEntity<?> writeForm() {
        log.info("Q&A 작성 폼 정보 요청");

        // REST API에서는 폼 정보가 필요 없지만, 기존 구조 유지를 위해 간단한 응답
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Q&A 작성 준비 완료"
        ));
    }
}