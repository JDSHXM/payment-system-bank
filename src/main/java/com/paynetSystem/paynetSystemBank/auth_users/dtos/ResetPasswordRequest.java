package com.paynetSystem.paynetSystemBank.auth_users.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResetPasswordRequest {

    private String email;

    private String code;
    private String newPassword;

}
