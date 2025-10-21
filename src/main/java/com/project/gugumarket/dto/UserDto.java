package com.project.gugumarket.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDto {
    @NotEmpty(message="아이디는 필수항목입니다.")
    @Size(min=5, max=20)
    private String userName;

    @NotEmpty(message = "이메일은 필수항목입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotEmpty(message="비밀번호는 필수항목입니다.")
    private String password;

    @NotEmpty(message="비밀번호 확인은 필수 항목입니다.")
    private String passwordConfirm;

    @NotEmpty(message = "닉네임은 필수 항목입니다.")
    private String nickname;

    private String phone;

    @NotEmpty(message = "우편번호는 필수 항목입니다.")
    private String postalCode;

    @NotEmpty(message = "주소는 필수 항목입니다.")
    private String address;

    @NotEmpty(message = "상세주소는 필수 항목입니다.")
    private String addressDetail;
}
