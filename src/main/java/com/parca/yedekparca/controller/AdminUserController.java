package com.parca.yedekparca.controller;

import com.parca.yedekparca.dto.AdminUserPageResponse;
import com.parca.yedekparca.dto.AdminUserResponse;
import com.parca.yedekparca.dto.ApiResponse;
import com.parca.yedekparca.dto.UserStatusRequest;
import com.parca.yedekparca.entity.UserRole;
import com.parca.yedekparca.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<AdminUserPageResponse>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserRole role,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        return ResponseEntity.ok(ApiResponse.success(
                "Kullanıcılar getirildi.",
                userService.getAdminUsers(search, role, page, size)));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusRequest request) {

        return ResponseEntity.ok(ApiResponse.success(
                "Kullanıcı durumu güncellendi.",
                userService.updateUserStatus(id, request.getActive())));
    }
}
