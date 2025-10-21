package com.project.gugumarket.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductStatusRequest {
    private String status; // "SALE", "RESERVED", "SOLD_OUT"
}
