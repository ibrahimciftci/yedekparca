
package com.parca.yedekparca.service;

import com.parca.yedekparca.dto.UserLoginRequest;
import com.parca.yedekparca.dto.UserLoginResponse;
import com.parca.yedekparca.dto.UserRegisterRequest;
import com.parca.yedekparca.dto.UserResponse;
import com.parca.yedekparca.entity.User;
import com.parca.yedekparca.entity.UserRole;
import com.parca.yedekparca.repository.UserRepository;
import com.parca.yedekparca.dto.UserUpdateRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UserResponse register(UserRegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                    "Bu e-posta adresi zaten kullanılıyor."
            );
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.CUSTOMER)
                .active(true)
                .build();

        User savedUser = userRepository.save(user);

        return mapToUserResponse(savedUser);
    }

    public UserLoginResponse login(UserLoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException(
                        "E-posta veya şifre hatalı."
                ));

        if (!user.getActive()) {
            throw new IllegalArgumentException(
                    "Hesabınız pasif durumdadır."
            );
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {
            throw new IllegalArgumentException(
                    "E-posta veya şifre hatalı."
            );
        }

        String token = jwtService.generateToken(user.getEmail());

        return UserLoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpiration())
                .user(mapToUserResponse(user))
                .build();
    }

    private UserResponse mapToUserResponse(User user) {

        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .build();
    }


    public UserResponse getProfile(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Kullanıcı bulunamadı."
                ));

        return mapToUserResponse(user);
    }


    public UserResponse updateProfile(
            String email,
            UserUpdateRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Kullanıcı bulunamadı."
                ));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        User updatedUser = userRepository.save(user);

        return mapToUserResponse(updatedUser);
    }

}
