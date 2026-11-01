// 1. ملف dtos/GoogleLoginRequest.java
package com.omar.expense_tracker_backend.dtos;

public class GoogleLoginRequest {
    private String idToken;
    private String accessToken;

    public GoogleLoginRequest() {}

    public String getIdToken() { return idToken; }
    public void setIdToken(String idToken) { this.idToken = idToken; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
}