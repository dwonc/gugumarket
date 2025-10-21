package com.project.gugumarket.controller;

import com.project.gugumarket.dto.QnaDto;
import com.project.gugumarket.dto.ResponseDto;
import com.project.gugumarket.entity.QnaPost;
import com.project.gugumarket.service.CategoryService;
import com.project.gugumarket.service.QnaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
@Slf4j
@RestController
@RequestMapping("/qna")
@RequiredArgsConstructor
public class QnaController {

    private final QnaService qnaService;

    @GetMapping
    public ResponseEntity<ResponseDto<List<QnaDto>>> qnaList() {
        try {
            List<QnaDto> qnaList = qnaService.getAllqnalist();
            System.out.println("테스트"+qnaList);
            return ResponseEntity.ok(
                    ResponseDto.success("qna 목록 조회 성공",qnaList ));
        } catch (Exception e) {
            log.error("qna 조회 중 오류 발생", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDto.fail("qna 조회 중 오류가 발생했습니다."));
        }
    }
}
