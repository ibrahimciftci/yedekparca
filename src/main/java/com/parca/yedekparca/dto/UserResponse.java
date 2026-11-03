
package com.parca.yedekparca.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {

    private Long id;

    private String firstName;

    private String lastName;

    private String email;

    private String role;

    private LocalDateTime createdAt;
}
