
package com.parca.yedekparca.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserRegisterRequest {

    @NotBlank(message = "Ad alanı boş bırakılamaz.")
    @Size(max = 100, message = "Ad en fazla 100 karakter olabilir.")
    private String firstName;

    @NotBlank(message = "Soyad alanı boş bırakılamaz.")
    @Size(max = 100, message = "Soyad en fazla 100 karakter olabilir.")
    private String lastName;

    @NotBlank(message = "E-posta alanı boş bırakılamaz.")
    @Email(message = "Geçerli bir e-posta adresi giriniz.")
    @Size(max = 150)
    private String email;

    @NotBlank(message = "Şifre alanı boş bırakılamaz.")
    @Size(min = 8, max = 100, message = "Şifre 8-100 karakter arasında olmalıdır.")
    private String password;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

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
