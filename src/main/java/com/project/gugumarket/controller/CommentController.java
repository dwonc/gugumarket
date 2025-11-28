// src/main/java/com/project/gugumarket/controller/CommentController.java
package com.project.gugumarket.controller;

import com.project.gugumarket.dto.CommentDto;
import com.project.gugumarket.entity.Product;
import com.project.gugumarket.entity.User;
import com.project.gugumarket.service.CommentService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 댓글 기능을 처리하는 REST API 컨트롤러
 * - 댓글 목록 조회
 * - 댓글 작성 (일반 댓글 및 대댓글)
 * - 댓글 수정
 * - 댓글 삭제
 * 모든 응답은 JSON 형식으로 반환
 */

@RestController //  REST API 컨트롤러 선언  (모든 메서드가 JSON 반환)
@RequiredArgsConstructor    //  final 필드에 대한 생성자를 자동으로 만들어줌
@RequestMapping("/api")     //  모든 메서드의 기본 URL 경로 ( /api 로 시작)
public class CommentController {

    private final CommentService commentService;
    //  댓글 관련 비즈니스 로직을 처리하는 서비스
    private final EntityManager em; 
    //  JPA 엔티티를 관리하는 매니저 ( 데이터베이스 작업용 )

    /**
     * 현재 로그인한 사용자 정보 조회
     *
     * @param auth Spring Security 인증 정보
     * @return 현재 로그인한 User 엔티티, 로그인하지 않았으면 null
     */
    private User currentUser(Authentication auth) {
        if (auth == null) return null;
        //  auth 가 null 이면 로그인 안 한 상태 -> null 반환

        String username = auth.getName();
        //  로그인한 사용자의 username 가져옴
        //  Spring Security가 로그인 시 저장한 정보
        try {
            return em.createQuery("select u from User u where u.userName = :name", User.class)
            //  JPQL 쿼리로 데이터베이스에서 사용자 찾기
            //  "select u from User u where u.userName = :name"
            //  SQL: SELECT * FROM user WHERE user_name = ?

                    .setParameter("name", username)//  :name 파라미테어 username 값 바인딩
                    .getSingleResult(); //  결과 1개 반환 ( 없으면 예외 발생 )
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * 특정 상품의 댓글 목록 조회
     * - 해당 상품에 작성된 모든 댓글 반환
     * - 현재 사용자가 작성한 댓글인지 여부 포함
     * - 댓글 총 개수 포함
     *
     * @param productId 댓글을 조회할 상품 ID
     * @param auth 현재 로그인한 사용자 정보
     * @return JSON 응답 { success: true, comments: [...], count: n }
     */
    @GetMapping("/products/{id}/comments") //  GET 요청 매핑
    public ResponseEntity<?> list(@PathVariable("id") 
                    Long productId, //   URL 경로의 {id}를 productId 변수로
                    Authentication auth // 로그인 정보 없으면 NULL
        ) {
        Product product = em.getReference(Product.class, productId); //  상품 참조 가져오기    
        //  getReference : 실제 DB 조회 없이 프록시 객체만 가져옴 ( 성능 최적화 )
    
        User me = currentUser(auth);    //  현재 로그인한 사용자 정보 가져오기
        Long currentUserId = (me == null) ? null : me.getUserId();  
        //  현재 사용자 ID 추출 ( 로그인 안했으면 NULL )

        List<CommentDto> list = commentService.list(product, currentUserId);    //  댓글 목록 조회
        //  commentService가 비즈니스 로직 처리
        //  currentUserId를 전달해서 각 댓글의 mine 필드 설정

        Map<String, Object> body = new HashMap<>(); //  응답 JSON 생성  HashMap으로 JSON 구조 만들기
        body.put("success", true);  //  성공 여부
        body.put("comments", list); //  댓글 배열
        body.put("count", list.size()); //  댓글 개수  
        return ResponseEntity.ok(body);
        //  ResponseEntity.ok() : HTTP 200 OK 상태코드와 함께 응답
        //  결과 : { "success": true, "comments": [...], "count": 3 }
    }

    /**
     * 댓글 작성
     * - 일반 댓글: parentId 없이 작성
     * - 대댓글: parentId를 포함하여 작성
     * - 로그인한 사용자만 작성 가능
     * - 빈 내용은 작성 불가
     *
     * @param productId 댓글을 작성할 상품 ID
     * @param payload 요청 본문 { content: "댓글 내용", parentId: "부모댓글ID(선택)" }
     * @param auth 현재 로그인한 사용자 정보
     * @return JSON 응답 { success: true, comment: {...}, count: n }
     *         - 로그인 안 됨: 401 상태코드, { success: false, needLogin: true }
     *         - 내용 없음: 400 상태코드, { success: false, message: "..." }
     */
    @PostMapping("/products/{id}/comments")     //  POST 요청 매핑
    public ResponseEntity<?> create(@PathVariable("id") Long productId, //  URL 경로의 상품 ID
                                    @RequestBody Map<String, String> payload,   //  요청 본문을 map 으로 받기
                                    Authentication auth) {  //  로그인 정보
        // 로그인 여부 확인
        User me = currentUser(auth);
        if (me == null) {   //  로그인 안했으면
            return ResponseEntity.status(401).body  //  401 UNAUTHORIZED 상태코드 반환
            (Map.of("success", false, "needLogin", true));
            //  Map.of(): 간단하게 Map 생성하는 메서드
        }

        // 댓글 내용 검증
        String content = payload.getOrDefault("content", "").trim();
        //  Payload에서 "content" 키의 값을 가져옴 ( 없으면 빈 문자열 )
        if (content.isEmpty()) {    //  빈 내용이면 400 BadRequest 반환
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "내용을 입력하세요."));
        }

        // 대댓글인 경우 부모 댓글 ID 파싱
        Long parentId = null;
        try {
            String pid = payload.get("parentId");
            //  Payload에서 "parentId" 값 가져오기
            if (pid != null && !pid.isBlank()) 
                //  parentId가 있고 빈 문자열이 아니면 Long으로 변환
                //  isBlank(): 공백만 있어도 true (trim() + isEmpty())
                parentId = Long.valueOf(pid);   //  문자열 -> Long 변환
        } catch (NumberFormatException ignore) {}
            //  변환 실패해도 무시 ( parentId 는 null로 유지 )
            //  ex) "abc"를 숫자로 변환하려고 하면 예외 발생

        // 댓글 저장 및 총 개수 조회
        Product product = em.getReference(Product.class, productId);    //  상품 참조 가져오기
        CommentDto saved = commentService.create(product, me, content, parentId);
        //  commentService가 실제 댓글 저장 ( 데이터베이스 INSERT )
        //  반환값 : 저장된 댓글의 DTO ( ID, 내용, 날짜 등 포함 )

        long count = commentService.countByProductId(productId);
        //  저장 후 해당 상품의 총 댓글 개수 조회

        return ResponseEntity.ok(Map.of(    //  Map.of(): 여러 키-값 쌍을 한번에 Map으로 만들기
                "success", true,    //  성공 여부
                "comment", saved,       // 저장된 댓글 정보
                "count", count          //  총 댓글 개수
        )); 
        // 결과 ex) {"success": true, "comment": {...}, "count": 4 }
    }

    /**
     * 댓글 수정
     * - 본인이 작성한 댓글만 수정 가능
     * - 로그인한 사용자만 수정 가능
     * - 빈 내용으로 수정 불가
     *
     * @param commentId 수정할 댓글 ID
     * @param payload 요청 본문 { content: "수정할 내용" }
     * @param auth 현재 로그인한 사용자 정보
     * @return JSON 응답 { success: true, comment: {...} }
     *         - 로그인 안 됨: 401 상태코드, { success: false, needLogin: true }
     *         - 내용 없음: 400 상태코드, { success: false, message: "..." }
     */
    @PutMapping("/comments/{id}")   //  PUT 요청 매핑
    public ResponseEntity<?> update(@PathVariable("id") Long commentId,
                                    @RequestBody Map<String, String> payload,
                                    Authentication auth) {  //  로그인 정보
        // 로그인 여부 확인
        User me = currentUser(auth);
        if (me == null) {   //  로그인 안했으면 401 반환
            return ResponseEntity.status(401).body(Map.of("success", false, "needLogin", true));
        }

        // 댓글 내용 검증
        String content = payload.getOrDefault("content", "").trim();
        if (content.isEmpty()) {    //  빈 내용이면 400 반환
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "내용을 입력하세요."));
        }

        // 댓글 수정 (서비스에서 권한 검증)
        CommentDto dto = commentService.update(commentId, me, content);
        return ResponseEntity.ok(Map.of("success", true, "comment", dto));
    }

    /**
     * 댓글 삭제
     * - 본인이 작성한 댓글만 삭제 가능
     * - 로그인한 사용자만 삭제 가능
     * - 삭제 후 남은 댓글 총 개수 반환
     *
     * @param commentId 삭제할 댓글 ID
     * @param auth 현재 로그인한 사용자 정보
     * @return JSON 응답 { success: true, count: n }
     *         - 로그인 안 됨: 401 상태코드, { success: false, needLogin: true }
     */
    @DeleteMapping("/comments/{id}")    //  DELETE 요청 매핑
    public ResponseEntity<?> delete(@PathVariable("id") 
                                Long commentId, //  URL 경로의 댓글 ID
                                Authentication auth) { // 로그인 정보
        // 로그인 여부 확인
        User me = currentUser(auth);    //  로그인 여부 확인
        if (me == null) {   // 로그인 안 했으면 401 반환
            return ResponseEntity.status(401).body(Map.of("success", false, "needLogin", true));
        }

        // 댓글 삭제 및 남은 개수 반환 (서비스에서 권한 검증)
        long count = commentService.delete(commentId, me);
        return ResponseEntity.ok(Map.of("success", true, "count", count));
        //  결과 ex) { "success" : true , "count" 2 }
    }
}
