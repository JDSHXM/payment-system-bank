package com.paynetSystem.paynetSystemBank.transaction.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.paynetSystem.paynetSystemBank.enums.TransactionType;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionRequest {
    private TransactionType transactionType;
    private BigDecimal amount;
    private String accountNumber;
    private String description;

    private String destinationAccountNumber; // Номер счёта получателя, если это перевод

}
