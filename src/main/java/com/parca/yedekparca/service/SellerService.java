package com.parca.yedekparca.service;

import com.parca.yedekparca.dto.SellerApplicationRequest;
import com.parca.yedekparca.dto.SellerResponse;
import com.parca.yedekparca.entity.Seller;
import com.parca.yedekparca.entity.SellerStatus;
import com.parca.yedekparca.entity.User;
import com.parca.yedekparca.repository.SellerRepository;
import com.parca.yedekparca.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import com.parca.yedekparca.entity.UserRole;

@Service
public class SellerService {

    private final SellerRepository sellerRepository;
    private final UserRepository userRepository;
    private final LocationService locations;

    public SellerService(
            SellerRepository sellerRepository,
            UserRepository userRepository,
            LocationService locations) {
        this.sellerRepository = sellerRepository;
        this.userRepository = userRepository;
        this.locations = locations;
    }

    @Transactional
    public SellerResponse applyForSeller(
            String email,
            SellerApplicationRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("Kullanıcı bulunamadı."));

        if (user.getRole() != UserRole.CUSTOMER || !Boolean.TRUE.equals(user.getActive())) {
            throw new IllegalArgumentException("Yalnızca aktif müşteri hesapları satıcı başvurusu yapabilir.");
        }

        if (sellerRepository.existsByUserId(user.getId())) {
            throw new IllegalArgumentException(
                    "Zaten bir satıcı başvurunuz bulunmaktadır.");
        }

        LocationService.LocationSelection location = locations.resolve(
                request.getCityId(), request.getDistrictId(), request.getCity(), request.getDistrict());

        Seller seller = Seller.builder()
                .user(user)
                .storeName(request.getStoreName())
                .phone(request.getPhone())
                .city(location.city().getName())
                .district(location.district().getName())
                .cityId(location.city().getId())
                .districtId(location.district().getId())
                .description(request.getDescription())
                .status(SellerStatus.PENDING)
                .build();

        Seller savedSeller = sellerRepository.save(seller);

        return mapToSellerResponse(savedSeller);
    }

    @Transactional(readOnly = true)
    public SellerResponse getMyApplication(String email) {

        Seller seller = sellerRepository.findByUserId(
                        userRepository.findByEmail(email)
                                .orElseThrow(() ->
                                        new IllegalArgumentException(
                                                "Kullanıcı bulunamadı."))
                                .getId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Satıcı başvurunuz bulunamadı."));

        return mapToSellerResponse(seller);
    }

    @Transactional(readOnly = true)
    public List<SellerResponse> getPendingApplications() {

        return sellerRepository.findByStatus(SellerStatus.PENDING)
                .stream()
                .map(this::mapToSellerResponse)
                .toList();
    }

    @Transactional
    public SellerResponse approveApplication(Long sellerId) {

        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Satıcı başvurusu bulunamadı."));

        if (seller.getStatus() != SellerStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Bu başvuru daha önce değerlendirilmiş.");
        }

        seller.setStatus(SellerStatus.APPROVED);
        seller.setReviewedAt(LocalDateTime.now());

        User user = seller.getUser();
        user.setRole(UserRole.SELLER);

        sellerRepository.save(seller);

        return mapToSellerResponse(seller);
    }

    @Transactional
    public SellerResponse rejectApplication(
            Long sellerId,
            String rejectionReason) {

        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Satıcı başvurusu bulunamadı."));

        if (seller.getStatus() != SellerStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Bu başvuru daha önce değerlendirilmiş.");
        }

        seller.setStatus(SellerStatus.REJECTED);
        seller.setRejectionReason(rejectionReason);
        seller.setReviewedAt(LocalDateTime.now());

        sellerRepository.save(seller);

        return mapToSellerResponse(seller);
    }

    private SellerResponse mapToSellerResponse(Seller seller) {
        return SellerResponse.builder()
                .id(seller.getId())
                .applicantFirstName(seller.getUser().getFirstName())
                .applicantLastName(seller.getUser().getLastName())
                .applicantEmail(seller.getUser().getEmail())
                .storeName(seller.getStoreName())
                .phone(seller.getPhone())
                .city(seller.getCity())
                .district(seller.getDistrict())
                .cityId(seller.getCityId())
                .districtId(seller.getDistrictId())
                .description(seller.getDescription())
                .status(seller.getStatus())
                .rejectionReason(seller.getRejectionReason())
                .appliedAt(seller.getAppliedAt())
                .reviewedAt(seller.getReviewedAt())
                .build();
    }
}
