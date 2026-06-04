package com.sjh.mapleguildtd.domain.auth.controller;

import com.sjh.mapleguildtd.infrastructure.dto.ApiResponse;
import com.sjh.mapleguildtd.infrastructure.security.config.AdminProperties;
import com.sjh.mapleguildtd.infrastructure.security.jwt.JwtUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AdminProperties adminProperties;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        if (!adminProperties.getUsername().equals(request.getUsername()) ||
                !adminProperties.getPassword().equals(request.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("아이디 또는 비밀번호가 올바르지 않습니다."));
        }

        String token = jwtUtil.generateToken(request.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(new LoginResponse(token)));
    }

    @Getter
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Getter
    @RequiredArgsConstructor
    public static class LoginResponse {
        private final String token;
    }
}