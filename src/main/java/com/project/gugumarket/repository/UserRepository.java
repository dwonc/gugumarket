package com.project.gugumarket.repository;

import com.project.gugumarket.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // userName으로 중복 체크
    boolean existsByUserName(String userName);

    // email로 중복 체크
    boolean existsByEmail(String email);

    // ID로 조회
    Optional<User> findById(Long id);

    // userName으로 조회 ✅
    Optional<User> findByUserName(String userName);

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
    Optional<User> findByEmail(String email);
}
