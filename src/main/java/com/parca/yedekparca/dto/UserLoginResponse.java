
package com.parca.yedekparca.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserLoginResponse {

    private String token;
    private String tokenType;
    private long expiresIn;
    private UserResponse user;
}
