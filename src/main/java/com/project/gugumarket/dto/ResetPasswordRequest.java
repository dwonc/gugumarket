package com.project.gugumarket.dto;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String resetToken;
    private String newPassword;
}