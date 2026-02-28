package com.paynetSystem.paynetSystemBank.account.dtos;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.paynetSystem.paynetSystemBank.auth_users.dtos.UserDTO;
import com.paynetSystem.paynetSystemBank.enums.AccountStatus;
import com.paynetSystem.paynetSystemBank.enums.AccountType;
import com.paynetSystem.paynetSystemBank.enums.Currency;
import com.paynetSystem.paynetSystemBank.transaction.dtos.TransactionDTO;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class AccountDTO {

    private Long id;

    private String accountNumber;

    private BigDecimal balance = BigDecimal.ZERO;

    private AccountType accountType;

    @JsonBackReference // «Это не будет добавлено в AccountDTO. Это будет проигнорировано,
                       // потому что является обратной ссылкой (back reference).
    private UserDTO user;

    private Currency currency;

    private AccountStatus status;

    @JsonBackReference //Это помогает избежать рекурсивного цикла, игнорируя UserDTO внутри AccountDTO.
    private List<TransactionDTO> transactions;

    private LocalDateTime closedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updateAt;


}
