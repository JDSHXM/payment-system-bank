package com.paynetSystem.paynetSystemBank.transaction.services;

import com.paynetSystem.paynetSystemBank.res.Response;
import com.paynetSystem.paynetSystemBank.transaction.dtos.TransactionDTO;
import com.paynetSystem.paynetSystemBank.transaction.dtos.TransactionRequest;

import java.util.List;

public interface TransactionService {
    Response<?>createTransaction(TransactionRequest transactionRequest);
    Response<List<TransactionDTO>> getTransactionsForMyAccount(
            String accountNumber, int page, int size);
}