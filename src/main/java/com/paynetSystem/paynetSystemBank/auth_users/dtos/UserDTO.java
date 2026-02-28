package com.paynetSystem.paynetSystemBank.auth_users.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.paynetSystem.paynetSystemBank.account.dtos.AccountDTO;
import com.paynetSystem.paynetSystemBank.role.entity.Role;
import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDTO {

    private Long id;

    private String firstName;

    private String lastname;
    private String phoneNumber;

    private String email;

    @JsonInclude
    private String password;

    private String profilePictureUrl;
    private boolean active;

    private List<Role> roles;

    @JsonManagedReference //Это помогает избежать рекурсивного цикла, игнорируя UserDTO внутри AccountDTO.
    private List<AccountDTO> accounts;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
