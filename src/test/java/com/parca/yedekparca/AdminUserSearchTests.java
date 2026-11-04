package com.parca.yedekparca;

import com.parca.yedekparca.dto.AdminUserPageResponse;
import com.parca.yedekparca.entity.User;
import com.parca.yedekparca.entity.UserRole;
import com.parca.yedekparca.repository.UserRepository;
import com.parca.yedekparca.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class AdminUserSearchTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    private String searchToken;
    private Long customerId;
    private Long sellerId;
    private Long adminId;

    @BeforeEach
    void setUp() {
        searchToken = "needle" + UUID.randomUUID().toString().replace("-", "");
        customerId = createUser("Mixed" + searchToken, "Customer", UserRole.CUSTOMER).getId();
        sellerId = createUser("Mixed" + searchToken, "Seller", UserRole.SELLER).getId();
        adminId = createUser("Mixed" + searchToken, "Admin", UserRole.ADMIN).getId();
    }

    @Test
    void nullEmptyAndWhitespaceSearchReturnAllRoles() {
        assertContainsAllFixtureUsers(userService.getAdminUsers(null, null, 0, 100));
        assertContainsAllFixtureUsers(userService.getAdminUsers("", null, 0, 100));
        assertContainsAllFixtureUsers(userService.getAdminUsers("   ", null, 0, 100));
    }

    @Test
    void textSearchIsCaseInsensitiveAcrossUserFields() {
        AdminUserPageResponse firstNameResult = userService.getAdminUsers(
                searchToken.toUpperCase(Locale.ROOT), null, 0, 100);
        AdminUserPageResponse emailResult = userService.getAdminUsers(
                ("account" + searchToken).toUpperCase(Locale.ROOT), null, 0, 100);
        AdminUserPageResponse lastNameResult = userService.getAdminUsers(
                "seller", null, 0, 100);

        assertEquals(3, firstNameResult.getContent().stream()
                .filter(user -> hasFixtureId(user.getId())).count());
        assertTrue(emailResult.getContent().stream().anyMatch(user -> user.getId().equals(customerId)));
        assertTrue(emailResult.getContent().stream().anyMatch(user -> user.getId().equals(sellerId)));
        assertTrue(emailResult.getContent().stream().anyMatch(user -> user.getId().equals(adminId)));
        assertEquals(1, lastNameResult.getContent().stream()
                .filter(user -> hasFixtureId(user.getId())).count());
        assertTrue(lastNameResult.getContent().stream().anyMatch(user -> user.getId().equals(sellerId)));
    }

    @Test
    void roleFilterReturnsOnlyRequestedRole() {
        AdminUserPageResponse result = userService.getAdminUsers(null, UserRole.SELLER, 0, 100);

        assertTrue(result.getContent().stream().allMatch(user -> "SELLER".equals(user.getRole())));
        assertTrue(result.getContent().stream().anyMatch(user -> user.getId().equals(sellerId)));
    }

    @Test
    void textSearchAndRoleFilterCanBeCombined() {
        AdminUserPageResponse result = userService.getAdminUsers(
                searchToken.toUpperCase(Locale.ROOT), UserRole.SELLER, 0, 100);

        assertEquals(1, result.getContent().size());
        assertEquals(sellerId, result.getContent().get(0).getId());
        assertEquals("SELLER", result.getContent().get(0).getRole());
    }

    private User createUser(String firstName, String lastName, UserRole role) {
        return userRepository.save(User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(("account" + searchToken + "." + role + "@example.test").toLowerCase())
                .password("test-password")
                .role(role)
                .active(true)
                .build());
    }

    private void assertContainsAllFixtureUsers(AdminUserPageResponse result) {
        assertTrue(result.getContent().stream().anyMatch(user -> user.getId().equals(customerId)));
        assertTrue(result.getContent().stream().anyMatch(user -> user.getId().equals(sellerId)));
        assertTrue(result.getContent().stream().anyMatch(user -> user.getId().equals(adminId)));
    }

    private boolean hasFixtureId(Long id) {
        return id.equals(customerId) || id.equals(sellerId) || id.equals(adminId);
    }
}
