
package com.parca.yedekparca.controller;

import com.parca.yedekparca.dto.ApiResponse;
import com.parca.yedekparca.dto.UserResponse;
import com.parca.yedekparca.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.parca.yedekparca.dto.UserUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Giriş yapan kullanıcının profil bilgilerini getirir.
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(
            Authentication authentication) {

        String email = authentication.getName();

        UserResponse user = userService.getProfile(email);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Kullanıcı profili başarıyla getirildi.",
                        user
                )
        );
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UserUpdateRequest request) {

        String email = authentication.getName();

        UserResponse updatedUser =
                userService.updateProfile(email, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(
                        "Kullanıcı profili başarıyla güncellendi.",
                        updatedUser
                ));
    }

}
