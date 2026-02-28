package com.paynetSystem.paynetSystemBank.audit_dashtboard.service;

import com.paynetSystem.paynetSystemBank.account.dtos.AccountDTO;
import com.paynetSystem.paynetSystemBank.auth_users.dtos.UserDTO;
import com.paynetSystem.paynetSystemBank.transaction.dtos.TransactionDTO;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AuditorService {

    Map<String, Long> getSystemTotals();

    Optional<UserDTO> findUserByEmail(String email);

    Optional<AccountDTO> findAccountDetailsByAccountNumber(String accountNumber);

    List<TransactionDTO> findTransactionsByAccountNumber(String accountNumber);

    Optional<TransactionDTO> findTransactionById(Long transactionId);
}
