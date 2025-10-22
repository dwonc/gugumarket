package com.project.gugumarket.repository;

import com.project.gugumarket.dto.QnaDto;
import com.project.gugumarket.entity.QnaPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QnaRepository extends JpaRepository<QnaPost, Long> {

}
