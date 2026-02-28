package com.paynetSystem.paynetSystemBank.account.services;

import com.paynetSystem.paynetSystemBank.account.dtos.AccountDTO;
import com.paynetSystem.paynetSystemBank.account.entity.Account;
import com.paynetSystem.paynetSystemBank.auth_users.entity.User;
import com.paynetSystem.paynetSystemBank.enums.AccountType;
import com.paynetSystem.paynetSystemBank.res.Response;

import java.util.List;

public interface AccountService {
    Account createAccount(AccountType accountType, User user);

    Response<List<AccountDTO>> getMyAccounts();

    Response<?> closeAccount(String accountNumber);
}
