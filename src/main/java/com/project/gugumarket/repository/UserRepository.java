package com.project.gugumarket.repository;

import com.project.gugumarket.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 팀의 User 엔티티가 username/email을 가진 경우 대비
    Optional<User> findByUserName(String userName);
    Optional<User> findByEmail(String email);
}