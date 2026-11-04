package com.parca.yedekparca.controller;

import com.parca.yedekparca.dto.ApiResponse;
import com.parca.yedekparca.dto.SellerApplicationRequest;
import com.parca.yedekparca.dto.SellerResponse;
import com.parca.yedekparca.service.SellerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sellers")
public class SellerController {

    private final SellerService sellerService;

    public SellerController(SellerService sellerService) {
        this.sellerService = sellerService;
    }

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<SellerResponse>> applyForSeller(
            Authentication authentication,
            @Valid @RequestBody SellerApplicationRequest request) {

        SellerResponse response = sellerService.applyForSeller(
                authentication.getName(), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Satıcı başvurunuz başarıyla oluşturuldu.", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<SellerResponse>> getMyApplication(
            Authentication authentication) {

        SellerResponse response = sellerService.getMyApplication(
                authentication.getName());

        return ResponseEntity.ok(ApiResponse.success(
                "Satıcı başvurunuz başarıyla getirildi.", response));
    }
}