package com.example.ELearningBE.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterPendingResponse {

    private String email;
    private String message;
    private long expiresInMinutes;
}
