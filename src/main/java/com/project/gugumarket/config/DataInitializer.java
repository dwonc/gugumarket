package com.project.gugumarket.config;

import com.project.gugumarket.entity.Category;
import com.project.gugumarket.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    @Bean
    public CommandLineRunner initCategories(CategoryRepository categoryRepository) {
        return args -> {
            if (categoryRepository.count() == 0) {
                categoryRepository.save(Category.builder().name("전자기기").build());
                categoryRepository.save(Category.builder().name("의류").build());
                categoryRepository.save(Category.builder().name("가구/인테리어").build());
                categoryRepository.save(Category.builder().name("도서").build());
                categoryRepository.save(Category.builder().name("스포츠/레저").build());
                categoryRepository.save(Category.builder().name("뷰티/미용").build());
                categoryRepository.save(Category.builder().name("가전제품").build());
                categoryRepository.save(Category.builder().name("취미/게임").build());
                categoryRepository.save(Category.builder().name("생활/주방").build());
                categoryRepository.save(Category.builder().name("기타").build());

                System.out.println("✅ 카테고리 초기 데이터 생성 완료");
            }
        };
    }
}