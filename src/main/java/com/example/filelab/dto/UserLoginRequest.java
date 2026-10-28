package com.example.filelab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

//与注册分开，更好区分
public class UserLoginRequest {


    private String username;


    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}