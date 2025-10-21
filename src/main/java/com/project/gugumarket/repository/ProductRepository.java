package com.project.gugumarket.repository;

import com.project.gugumarket.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> { }
