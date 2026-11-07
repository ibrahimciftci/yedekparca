package com.parca.yedekparca;

import com.parca.yedekparca.dto.SellerProfileUpdateRequest;
import com.parca.yedekparca.entity.*;
import com.parca.yedekparca.repository.*;
import com.parca.yedekparca.service.LocationService;
import com.parca.yedekparca.service.MarketplaceService;
import com.parca.yedekparca.service.ObjectStorageService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class SellerProfileLocationUnitTests {
    @Test
    void profileSavePersistsSelectedCityAndDistrictIdsForReload() {
        User user = User.builder().id(7L).email("seller@example.test").role(UserRole.SELLER).active(true).build();
        Seller seller = Seller.builder().id(9L).user(user).storeName("Old Store").phone("5551234567")
                .city("Istanbul").district("Kadikoy").status(SellerStatus.APPROVED).build();
        SellerRepository sellers = mock(SellerRepository.class);
        UserRepository users = mock(UserRepository.class);
        LocationService locations = mock(LocationService.class);
        City city = mock(City.class);
        District district = mock(District.class);
        when(city.getId()).thenReturn(34L);
        when(city.getName()).thenReturn("İstanbul");
        when(district.getId()).thenReturn(3401L);
        when(district.getName()).thenReturn("Kadıköy");
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(sellers.findByUserId(user.getId())).thenReturn(Optional.of(seller));
        when(locations.resolve(34L, 3401L, "İstanbul", "Kadıköy"))
                .thenReturn(new LocationService.LocationSelection(city, district));
        MarketplaceService service = new MarketplaceService(mock(CatalogProductRepository.class), mock(ProductOfferRepository.class),
                sellers, users, mock(OfferPhotoRepository.class), mock(ObjectStorageService.class),
                mock(UserFavoriteRepository.class), mock(ComplaintRepository.class), mock(ProductRepository.class),
                mock(VehicleModelRepository.class), locations);
        SellerProfileUpdateRequest request = new SellerProfileUpdateRequest("Updated Store", "5551234567", null, null,
                "İstanbul", "Kadıköy", null, null, null, null, null, null, null, 34L, 3401L);

        var updated = service.updateProfile(user.getEmail(), request);
        var reloaded = service.myProfile(user.getEmail());

        assertEquals(34L, updated.cityId());
        assertEquals(3401L, updated.districtId());
        assertEquals("İstanbul", reloaded.city());
        assertEquals("Kadıköy", reloaded.district());
        assertEquals(34L, reloaded.cityId());
        assertEquals(3401L, reloaded.districtId());
    }
}
