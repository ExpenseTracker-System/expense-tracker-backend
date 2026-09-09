package com.omar.expense_tracker_backend.exceptions;

import com.omar.expense_tracker_backend.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. صائد الأخطاء الخاصة بالمدخلات الخاطئة (400 Bad Request)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        String message = ex.getMessage();
        ApiResponse<Object> response = new ApiResponse<>(message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 2. صائد الأخطاء الخاصة بعدم العثور على الموارد (404 Not Found)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        String message = ex.getMessage();
        ApiResponse<Object> response = new ApiResponse<>(message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // 3. صائد الأخطاء العشوائية وغير المتوقعة (500 Internal Server Error)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobalException(Exception ex) {
        String message = "حدث خطأ داخلي في الخادم، يرجى المحاولة لاحقاً.";
        // يمكنك طباعة رسالة الخطأ الحقيقية في الـ Console لمعرفة السبب كمهندس
        System.out.println("Critical Error: " + ex.getMessage());

        ApiResponse<Object> response = new ApiResponse<>(message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}