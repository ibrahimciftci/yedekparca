
package com.parca.yedekparca.controller;

import com.parca.yedekparca.dto.ApiResponse;
import com.parca.yedekparca.dto.UserResponse;
import com.parca.yedekparca.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
}
