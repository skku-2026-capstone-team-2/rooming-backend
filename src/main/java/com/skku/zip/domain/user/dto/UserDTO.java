package com.skku.zip.domain.user.dto;

public class UserDTO {

    public String name;
    public String email;
    public String picture;
    public String provider;
    public String providerId;

    public UserDTO(String name, String email, String picture, String provider, String providerId) {
        this.name = name;
        this.email = email;
        this.picture = picture;
        this.provider = provider;
        this.providerId = providerId;
    }
}
