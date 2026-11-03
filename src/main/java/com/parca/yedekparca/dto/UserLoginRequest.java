
package com.parca.yedekparca.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class UserLoginRequest {

    @NotBlank(message = "E-posta alanı boş bırakılamaz.")
    @Email(message = "Geçerli bir e-posta adresi giriniz.")
    private String email;

    @NotBlank(message = "Şifre alanı boş bırakılamaz.")
    private String password;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
