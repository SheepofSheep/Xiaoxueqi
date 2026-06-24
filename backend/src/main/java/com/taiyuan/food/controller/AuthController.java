package com.taiyuan.food.controller;

import com.taiyuan.food.common.ApiResponse;
import com.taiyuan.food.config.JwtUtil;
import com.taiyuan.food.dto.LoginRequestDTO;
import com.taiyuan.food.vo.LoginResultVO;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final JwtUtil jwtUtil;
    private static final Map<String, String[]> USERS = Map.of(
        "admin", new String[]{"admin123", "ADMIN", "管理员"},
        "guest", new String[]{"guest123", "USER", "游客"}
    );

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/auth/login")
    public ApiResponse<LoginResultVO> login(@RequestBody LoginRequestDTO request) {
        String[] user = USERS.get(request.username());
        if (user == null || !user[0].equals(request.password())) {
            return ApiResponse.fail(401, "用户名或密码错误");
        }
        String token = jwtUtil.generateToken(request.username(), user[1]);
        return ApiResponse.success(new LoginResultVO(token, user[1], user[2]));
    }

    @PostMapping("/auth/guest")
    public ApiResponse<LoginResultVO> guestLogin() {
        String token = jwtUtil.generateToken("guest", "USER");
        return ApiResponse.success(new LoginResultVO(token, "USER", "游客"));
    }
}
