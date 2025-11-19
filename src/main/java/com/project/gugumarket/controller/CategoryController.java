package com.project.gugumarket.controller;

import com.project.gugumarket.dto.CategoryDto;
import com.project.gugumarket.dto.ResponseDto;
import com.project.gugumarket.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {
    private final CategoryService categoryService;

    /**
     * 카테고리 목록 조회
     * GET /api/categories
     *
     * 쿼리 파라미터:
     * - includeCount: true/false (상품 개수 포함 여부, 기본값: false)
     */
    @GetMapping
    @ResponseBody
    public ResponseEntity<ResponseDto<List<CategoryDto>>> list(
            @RequestParam(required = false, defaultValue = "false") Boolean includeCount) {

        try {
            log.info("카테고리 목록 조회 요청 - includeCount: {}", includeCount);

            List<CategoryDto> categories;

            if (includeCount) {
                // 상품 개수 포함
                categories = categoryService.getCategoriesWithProductCount();
            } else {
                // 기본 조회
                categories = categoryService.getAllCategories();
            }

            log.info("카테고리 목록 조회 성공 - {}개", categories.size());

            return ResponseEntity.ok(
                    ResponseDto.success("카테고리 목록 조회 성공", categories)
            );

        } catch (Exception e) {
            log.error("카테고리 목록 조회 중 오류 발생", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDto.fail("카테고리 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 카테고리 상세 조회
     * GET /api/categories/{id}
     */
    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<ResponseDto<CategoryDto>> detail(@PathVariable("id") Long id) {
        try {
            log.info("카테고리 상세 조회 요청 - ID: {}", id);

            CategoryDto categoryDto = categoryService.getCategoryDetail(id);

            return ResponseEntity.ok(
                    ResponseDto.success("카테고리 조회 성공", categoryDto)
            );

        } catch (IllegalArgumentException e) {
            log.warn("카테고리 조회 실패 - {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ResponseDto.fail(e.getMessage()));

        } catch (Exception e) {
            log.error("카테고리 조회 중 오류 발생", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDto.fail("카테고리 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 카테고리명으로 조회
     * GET /api/categories/name/{name}
     */
    @GetMapping("/name/{name}")
    @ResponseBody
    public ResponseEntity<ResponseDto<CategoryDto>> getByName(@PathVariable("name") String name) {
        try {
            log.info("카테고리 이름 조회 요청 - 이름: {}", name);

            CategoryDto categoryDto = categoryService.getCategoryByName(name);

            return ResponseEntity.ok(
                    ResponseDto.success("카테고리 조회 성공", categoryDto)
            );

        } catch (IllegalArgumentException e) {
            log.warn("카테고리 조회 실패 - {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ResponseDto.fail(e.getMessage()));

        } catch (Exception e) {
            log.error("카테고리 조회 중 오류 발생", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDto.fail("카테고리 조회 중 오류가 발생했습니다."));
        }
    }
}