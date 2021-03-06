package com.authentication.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.util.enums.PlatformType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Authentication", description = "An authentication object that contains the user credentials.")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthRequest implements TokenRequest {

    private String email;
    private String password;
    private PlatformType platform;

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

    public PlatformType getPlatform() { return platform; }
    public void setPlatform(PlatformType platform) { this.platform = platform; }

}

