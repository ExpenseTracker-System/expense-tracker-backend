package com.omar.expense_tracker_backend.controllers;

import com.omar.expense_tracker_backend.dtos.UserDTO;
import com.omar.expense_tracker_backend.exceptions.ResourceNotFoundException;
import com.omar.expense_tracker_backend.models.User;
import com.omar.expense_tracker_backend.responses.ApiResponse;
import com.omar.expense_tracker_backend.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    public UserController(UserService userService) {
        this.userService = userService;
    }
    @GetMapping("/{email}")
    public ResponseEntity<ApiResponse<UserDTO>> getUser(@PathVariable String email) {
        Optional<User> userOptional = userService.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            UserDTO userDto = new UserDTO(user.getId(), user.getUsername(),user.getEmail(),user.getAuthProvider());
            ApiResponse<UserDTO> response = new ApiResponse<>("تم جلب المستخدم بنجاح", userDto);
            return ResponseEntity.ok().body(response);
        }
        throw new ResourceNotFoundException("المستخدم غير موجود");
    }
}
