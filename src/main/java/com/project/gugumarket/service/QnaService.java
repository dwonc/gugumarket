package com.project.gugumarket.service;


import com.project.gugumarket.dto.CategoryDto;
import com.project.gugumarket.dto.QnaDto;
import com.project.gugumarket.entity.QnaPost;
import com.project.gugumarket.repository.QnaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class QnaService {

    @Autowired
    private QnaRepository  qnaRepository;

    public List<QnaDto> getAllqnalist() {

        List<QnaPost> qnaList = qnaRepository.findAll();
        return qnaList.stream()
                .map(QnaDto::fromEntity)
                .collect(Collectors.toList());
    }
}
