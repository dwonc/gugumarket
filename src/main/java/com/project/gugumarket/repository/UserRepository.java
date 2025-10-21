package com.project.gugumarket.repository;

import com.project.gugumarket.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {

    // userName으로 중복 체크
    boolean existsByUserName(String userName);

    // email로 중복 체크
    boolean existsByEmail(String email);

    // 로그인용 (나중에 필요)
    Optional<User> findByUserName(String userName);

    Optional<User> findByEmail(String email);
}
