package com.example.filelab.dto;

//不直接传用户user，因为其中又password在里面
public class CurrentUserResponse {

    private Long id;
    private String username;
    private String role;

    public CurrentUserResponse(Long id, String username, String role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }
}