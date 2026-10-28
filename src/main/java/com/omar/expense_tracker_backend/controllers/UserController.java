package com.omar.expense_tracker_backend.controllers;

import com.omar.expense_tracker_backend.dtos.UserDTO;
import com.omar.expense_tracker_backend.exceptions.ResourceNotFoundException;
import com.omar.expense_tracker_backend.models.User;
import com.omar.expense_tracker_backend.responses.ApiResponse;
import com.omar.expense_tracker_backend.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> getCurrentUser(Authentication auth) {
        User user = userService.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("المستخدم غير موجود"));
        UserDTO userDto = new UserDTO(user.getId(), user.getUsername(), user.getEmail(), user.getAuthProvider());
        return ResponseEntity.ok(new ApiResponse<>("تم جلب بيانات الحساب بنجاح", userDto));
    }

    @GetMapping("/{email}")
    public ResponseEntity<ApiResponse<UserDTO>> getUser(@PathVariable String email, Authentication auth) {
        if (!auth.getName().equalsIgnoreCase(email.trim())) {
            throw new IllegalArgumentException("غير مصرح لك بالوصول إلى بيانات مستخدم آخر");
        }
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("المستخدم غير موجود"));
        UserDTO userDto = new UserDTO(user.getId(), user.getUsername(), user.getEmail(), user.getAuthProvider());
        return ResponseEntity.ok(new ApiResponse<>("تم جلب المستخدم بنجاح", userDto));
    }
}