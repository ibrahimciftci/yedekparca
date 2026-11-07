package com.parca.yedekparca;

import com.parca.yedekparca.entity.*;
import com.parca.yedekparca.repository.*;
import com.parca.yedekparca.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;

class SellerLogoServiceTest {
    private UserRepository users; private SellerRepository sellers; private ObjectStorageService storage; private MarketplaceService service; private Seller seller;
    @BeforeEach void setup() {
        users=mock(UserRepository.class); sellers=mock(SellerRepository.class); storage=mock(ObjectStorageService.class);
        service=new MarketplaceService(mock(CatalogProductRepository.class),mock(ProductOfferRepository.class),sellers,users,mock(OfferPhotoRepository.class),storage,mock(UserFavoriteRepository.class),mock(ComplaintRepository.class),mock(ProductRepository.class),mock(VehicleModelRepository.class),mock(LocationService.class));
        User user=User.builder().id(3L).email("seller@example.test").role(UserRole.SELLER).active(true).build();
        seller=Seller.builder().id(7L).user(user).storeName("Test").phone("123456789").city("Istanbul").district("Kadikoy").status(SellerStatus.APPROVED).logoUrl("https://img.test/seller-logos/old.jpg").build();
        when(users.findByEmail("seller@example.test")).thenReturn(Optional.of(user)); when(sellers.findByUserId(3L)).thenReturn(Optional.of(seller));
    }
    @Test void rejectsUnauthorisedRoleAndInvalidFilesBeforeStorage() {
        User customer=User.builder().id(3L).email("seller@example.test").role(UserRole.CUSTOMER).active(true).build();
        when(users.findByEmail("seller@example.test")).thenReturn(Optional.of(customer));
        assertThrows(IllegalArgumentException.class,()->service.uploadSellerLogo("seller@example.test",new MockMultipartFile("file","x.jpg","image/jpeg",new byte[]{(byte)255,(byte)216,(byte)255})));
        verify(storage,never()).put(anyString(),anyString(),any());
    }
    @Test void rejectsOversizeAndMismatchedContent() {
        assertThrows(IllegalArgumentException.class,()->service.uploadSellerLogo("seller@example.test",new MockMultipartFile("file","x.jpg","image/jpeg",new byte[2*1024*1024+1])));
        assertThrows(IllegalArgumentException.class,()->service.uploadSellerLogo("seller@example.test",new MockMultipartFile("file","x.png","image/png",new byte[]{(byte)255,(byte)216,(byte)255,0})));
        verify(storage,never()).put(anyString(),anyString(),any());
    }
    @Test void replacesAndRemovesOnlySellerLogoObjects() {
        byte[] jpeg={(byte)255,(byte)216,(byte)255,0}; when(storage.newSellerLogoKey("jpg")).thenReturn("seller-logos/new.jpg"); when(storage.put("seller-logos/new.jpg","image/jpeg",jpeg)).thenReturn("https://img.test/seller-logos/new.jpg");
        service.uploadSellerLogo("seller@example.test",new MockMultipartFile("file","new.jpg","image/jpeg",jpeg));
        assertEquals("https://img.test/seller-logos/new.jpg",seller.getLogoUrl()); verify(storage).delete("seller-logos/old.jpg");
        service.deleteSellerLogo("seller@example.test"); assertNull(seller.getLogoUrl()); verify(storage).delete("seller-logos/new.jpg");
    }
    @Test void failedUploadKeepsExistingLogo() {
        byte[] jpeg={(byte)255,(byte)216,(byte)255,0}; when(storage.newSellerLogoKey("jpg")).thenReturn("seller-logos/new.jpg");
        when(storage.put(anyString(),anyString(),any())).thenThrow(new IllegalStateException("storage unavailable"));
        assertThrows(IllegalStateException.class,()->service.uploadSellerLogo("seller@example.test",new MockMultipartFile("file","new.jpg","image/jpeg",jpeg)));
        assertEquals("https://img.test/seller-logos/old.jpg",seller.getLogoUrl()); verify(storage,never()).delete(anyString());
    }
}
