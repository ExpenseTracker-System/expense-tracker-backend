package com.omar.expense_tracker_backend.dtos;

public class AuthResponse {
    private String token;
    private UserDTO userDto;

    public AuthResponse() {}

    public AuthResponse(String token, UserDTO userDto) {
        this.token = token;
        this.userDto = userDto;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public UserDTO getUserDto() { return userDto; }
    public void setUserDto(UserDTO userDto) { this.userDto = userDto; }
}