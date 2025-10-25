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

    // 이메일과 닉네임으로 조회 (아이디 찾기용)
    Optional<User> findByEmailAndNickname(String email, String nickname);

    // 전화번호와 닉네임으로 조회 (아이디 찾기용)
    Optional<User> findByPhoneAndNickname(String phone, String nickname);

    // 아이디와 이메일로 조회 (비밀번호 찾기용)
    Optional<User> findByUserNameAndEmail(String userName, String email);

}
