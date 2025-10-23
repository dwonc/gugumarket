package com.project.gugumarket.repository;

import com.project.gugumarket.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

<<<<<<< HEAD
=======
import org.springframework.stereotype.Repository;

import java.util.List;
>>>>>>> 99e0d3e7d634953e5cc34f25606565e61d769023
import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {

    // userName으로 중복 체크
    boolean existsByUserName(String userName);

    // email로 중복 체크
    boolean existsByEmail(String email);

    // 로그인용 (나중에 필요)
    Optional<User> findByUserName(String userName);

<<<<<<< HEAD
=======
    /**
     * 전체 회원 조회 (최신순)
     */
    List<User> findAllByOrderByCreatedDateDesc();

    /**
     * 회원 검색 (아이디, 닉네임, 이메일)
     */
    List<User> findByUserNameContainingOrNicknameContainingOrEmailContaining(
            String userName, String nickname, String email);
    // ❌ findByUsername() 는 삭제!
    // email로 조회
>>>>>>> 99e0d3e7d634953e5cc34f25606565e61d769023
    Optional<User> findByEmail(String email);
}
