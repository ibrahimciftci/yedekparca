package com.parca.yedekparca;

import com.parca.yedekparca.dto.SellerApplicationRequest;
import com.parca.yedekparca.entity.*;
import com.parca.yedekparca.repository.SellerRepository;
import com.parca.yedekparca.repository.UserRepository;
import com.parca.yedekparca.service.LocationService;
import com.parca.yedekparca.service.SellerService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SellerApplicationServiceUnitTests {
    private final SellerRepository sellers = mock(SellerRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final LocationService locations = mock(LocationService.class);
    private final SellerService service = new SellerService(sellers, users, locations);

    @Test
    void onlyActiveCustomersCanApply() {
        User seller = User.builder().id(1L).email("seller@example.test").role(UserRole.SELLER).active(true).build();
        when(users.findByEmail(seller.getEmail())).thenReturn(Optional.of(seller));
        SellerApplicationRequest request = request();

        assertThrows(IllegalArgumentException.class, () -> service.applyForSeller(seller.getEmail(), request));
        verify(sellers, never()).save(any());
    }

    @Test
    void duplicateApplicationsAreRejectedBeforeLocationResolution() {
        User customer = User.builder().id(2L).email("customer@example.test").role(UserRole.CUSTOMER).active(true).build();
        when(users.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(sellers.existsByUserId(customer.getId())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.applyForSeller(customer.getEmail(), request()));
        verify(locations, never()).resolve(any(), any(), any(), any());
        verify(sellers, never()).save(any());
    }

    @Test
    void customerApplicationPersistsCanonicalLocationIdsAndNames() {
        User customer = User.builder().id(3L).email("new-customer@example.test").role(UserRole.CUSTOMER).active(true).build();
        City city = mock(City.class);
        District district = mock(District.class);
        when(city.getId()).thenReturn(6L);
        when(city.getName()).thenReturn("Ankara");
        when(district.getId()).thenReturn(601L);
        when(district.getName()).thenReturn("Çankaya");
        when(users.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(sellers.existsByUserId(customer.getId())).thenReturn(false);
        when(locations.resolve(null, null, "Ankara", "Çankaya")).thenReturn(new LocationService.LocationSelection(city, district));
        when(sellers.save(any(Seller.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SellerApplicationRequest request = request();

        var response = service.applyForSeller(customer.getEmail(), request);

        assertEquals("Ankara", response.getCity());
        assertEquals("Çankaya", response.getDistrict());
        assertEquals(6L, response.getCityId());
        assertEquals(601L, response.getDistrictId());
        assertEquals(SellerStatus.PENDING, response.getStatus());
    }

    @Test
    void adminApprovalAssignsSellerRoleOnExistingApplication() {
        User applicant = mock(User.class);
        when(applicant.getFirstName()).thenReturn("Test");
        when(applicant.getLastName()).thenReturn("Customer");
        when(applicant.getEmail()).thenReturn("approve@example.test");
        Seller application = mock(Seller.class);
        when(application.getStatus()).thenReturn(SellerStatus.PENDING, SellerStatus.APPROVED);
        when(application.getUser()).thenReturn(applicant);
        when(sellers.findById(5L)).thenReturn(Optional.of(application));

        var response = service.approveApplication(5L);

        assertEquals(SellerStatus.APPROVED, response.getStatus());
        verify(application).setStatus(SellerStatus.APPROVED);
        verify(applicant).setRole(UserRole.SELLER);
        verify(sellers).save(application);
    }

    private static SellerApplicationRequest request() {
        SellerApplicationRequest request = new SellerApplicationRequest();
        request.setStoreName("Test Store");
        request.setPhone("5551234567");
        request.setCity("Ankara");
        request.setDistrict("Çankaya");
        return request;
    }
}
