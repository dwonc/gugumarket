package com.project.gugumarket.repository;

import com.project.gugumarket.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ID로 조회
    Optional<User> findById(Long id);

    // userName으로 조회 ✅
    Optional<User> findByUserName(String userName);

    // email로 조회
    Optional<User> findByEmail(String email);

    // ❌ findByUsername() 는 삭제!
}