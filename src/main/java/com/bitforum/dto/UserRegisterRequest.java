package com.bitforum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegisterRequest {
    // 注册接口的入参 DTO：只接收前端允许提交的字段，并在进入业务层前做格式校验
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3,max = 30,message = "用户名长度需在3到30个字符之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6,max = 30,message = "密码长度需在6到30个字符之间")
    private String password;
}
