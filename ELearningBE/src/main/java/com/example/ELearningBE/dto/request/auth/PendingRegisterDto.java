package com.example.ELearningBE.dto.request.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingRegisterDto implements Serializable {

    private String fullName;
    private String email;
    private String passwordHash;
    private String tokenId;
}
