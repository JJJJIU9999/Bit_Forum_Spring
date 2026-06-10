package com.bitforum.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserLoginRequest {
    // 登录接口的入参 DTO：登录只要求非空，账号密码是否正确交给 UserService 判断
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
