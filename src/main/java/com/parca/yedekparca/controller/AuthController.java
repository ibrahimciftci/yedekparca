
package com.parca.yedekparca.controller;

import com.parca.yedekparca.dto.ApiResponse;
import com.parca.yedekparca.dto.UserLoginRequest;
import com.parca.yedekparca.dto.UserLoginResponse;
import com.parca.yedekparca.dto.UserRegisterRequest;
import com.parca.yedekparca.dto.UserResponse;
import com.parca.yedekparca.service.UserService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // Kullanıcı kayıt işlemi
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody UserRegisterRequest request) {

        UserResponse user = userService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Kullanıcı başarıyla kaydedildi.",
                        user
                ));
    }

    // Kullanıcı giriş işlemi
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserLoginResponse>> login(
            @Valid @RequestBody UserLoginRequest request) {

        UserLoginResponse response = userService.login(request);

        return ResponseEntity
                .ok(ApiResponse.success(
                        "Giriş başarılı.",
                        response
                ));
    }
}
