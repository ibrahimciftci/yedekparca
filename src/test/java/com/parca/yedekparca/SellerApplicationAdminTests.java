package com.parca.yedekparca;

import com.parca.yedekparca.dto.SellerResponse;
import com.parca.yedekparca.entity.Seller;
import com.parca.yedekparca.entity.SellerStatus;
import com.parca.yedekparca.entity.User;
import com.parca.yedekparca.entity.UserRole;
import com.parca.yedekparca.repository.SellerRepository;
import com.parca.yedekparca.repository.UserRepository;
import com.parca.yedekparca.service.SellerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
class SellerApplicationAdminTests {

    @Autowired
    private SellerService sellerService;

    @Autowired
    private SellerRepository sellerRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void pendingApplicationResponseIncludesApplicantDetailsFromUserRelation() {
        String uniqueId = UUID.randomUUID().toString().replace("-", "");
        User applicant = userRepository.save(User.builder()
                .firstName("Applicant" + uniqueId)
                .lastName("Seller")
                .email("seller-" + uniqueId + "@example.test")
                .password("test-password")
                .role(UserRole.CUSTOMER)
                .active(true)
                .build());

        Seller application = sellerRepository.save(Seller.builder()
                .user(applicant)
                .storeName("Store " + uniqueId)
                .phone("5551234567")
                .city("Istanbul")
                .district("Kadikoy")
                .status(SellerStatus.PENDING)
                .build());

        SellerResponse response = sellerService.getPendingApplications().stream()
                .filter(item -> item.getId().equals(application.getId()))
                .findFirst()
                .orElseThrow();

        assertEquals(applicant.getFirstName(), response.getApplicantFirstName());
        assertEquals(applicant.getLastName(), response.getApplicantLastName());
        assertEquals(applicant.getEmail(), response.getApplicantEmail());
        assertNotNull(response.getAppliedAt());
    }
}
