package com.parca.yedekparca.controller;

import com.parca.yedekparca.dto.ApiResponse;
import com.parca.yedekparca.dto.SellerResponse;
import com.parca.yedekparca.dto.SellerReviewRequest;
import com.parca.yedekparca.service.SellerService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/sellers")
public class AdminSellerController {

    private final SellerService sellerService;

    public AdminSellerController(SellerService sellerService) {
        this.sellerService = sellerService;
    }

    // Bekleyen satıcı başvurularını listele.
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<SellerResponse>>> getPendingApplications() {

        List<SellerResponse> applications =
                sellerService.getPendingApplications();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Bekleyen satıcı başvuruları getirildi.",
                        applications
                )
        );
    }

    // Satıcı başvurusunu onayla.
    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<SellerResponse>> approveApplication(
            @PathVariable Long id) {

        SellerResponse response =
                sellerService.approveApplication(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Satıcı başvurusu başarıyla onaylandı.",
                        response
                )
        );
    }

    // Satıcı başvurusunu reddet.
    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<SellerResponse>> rejectApplication(
            @PathVariable Long id,
            @Valid @RequestBody SellerReviewRequest request) {

        SellerResponse response =
                sellerService.rejectApplication(
                        id,
                        request.getRejectionReason()
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Satıcı başvurusu reddedildi.",
                        response
                )
        );
    }
}