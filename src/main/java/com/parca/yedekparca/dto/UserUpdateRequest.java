
package com.parca.yedekparca.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserUpdateRequest {

    @NotBlank(message = "Ad alanı boş bırakılamaz.")
    @Size(max = 100, message = "Ad en fazla 100 karakter olabilir.")
    private String firstName;

    @NotBlank(message = "Soyad alanı boş bırakılamaz.")
    @Size(max = 100, message = "Soyad en fazla 100 karakter olabilir.")
    private String lastName;

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
}
