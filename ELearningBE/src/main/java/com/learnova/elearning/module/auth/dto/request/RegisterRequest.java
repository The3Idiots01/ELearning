package com.learnova.elearning.module.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "FIELD_REQUIRED")
    @Size(max = 150)
    private String fullName;

    @NotBlank(message = "FIELD_REQUIRED")
    @Email(message = "INVALID_EMAIL_FORMAT")
    private String email;

    @NotBlank(message = "FIELD_REQUIRED")
    @Size(min = 6, message = "PASSWORD_TOO_WEAK")
    private String password;
}
