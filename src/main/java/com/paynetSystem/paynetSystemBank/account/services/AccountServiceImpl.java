package com.paynetSystem.paynetSystemBank.account.services;

import com.paynetSystem.paynetSystemBank.account.dtos.AccountDTO;
import com.paynetSystem.paynetSystemBank.account.entity.Account;
import com.paynetSystem.paynetSystemBank.account.repo.AccountRepo;
import com.paynetSystem.paynetSystemBank.auth_users.entity.User;
import com.paynetSystem.paynetSystemBank.auth_users.services.UserService;
import com.paynetSystem.paynetSystemBank.enums.AccountStatus;
import com.paynetSystem.paynetSystemBank.enums.AccountType;
import com.paynetSystem.paynetSystemBank.enums.Currency;
import com.paynetSystem.paynetSystemBank.exceptions.BadRequestException;
import com.paynetSystem.paynetSystemBank.exceptions.NotFoundException;
import com.paynetSystem.paynetSystemBank.res.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final AccountRepo accountRepo;
    private final UserService userService;
    private final ModelMapper modelMapper;

    private final Random random = new Random();


    @Override
    public Account createAccount(AccountType accountType, User user) {

        String accountNumber = generateAccountNumber();

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .accountType(accountType)
                .currency(Currency.USD)
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();

        return accountRepo.save(account);
    }

    @Override
    public Response<List<AccountDTO>> getMyAccounts() {

        User user = userService.getCurrentLoggedInUser();

        List<AccountDTO> accounts = accountRepo.findByUserId(user.getId())
                .stream()
                .map(account -> modelMapper.map(account, AccountDTO.class))
                .toList();

        return Response.<List<AccountDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("User accounts fetched successfully")
                .data(accounts)
                .build();
    }


    @Override
    public Response<?> closeAccount(String accountNumber) {

        User user = userService.getCurrentLoggedInUser();
        Account account = accountRepo.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new NotFoundException("Account Not Found"));

        if (!user.getAccounts().contains(account)) {
            throw new NotFoundException("Account doesn't belong to you");
        }

        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException("Account balance must be zero before closing");
        }
        account.setStatus(AccountStatus.CLOSED);
        account.setClosedAt(LocalDateTime.now());
        accountRepo.save(account);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Account closed successfully")
                .build();

    }


    private String generateAccountNumber() {
        String accountNumber;
        do {
            //Сгенерируйте случайное 8-значное число (от 10,000,000 до 99,999,999)
            // и объедините его с префиксом "66".
            accountNumber = "66" + (random.nextInt(90000000) + 10000000);

        } while (accountRepo.findByAccountNumber(accountNumber).isPresent());

        return accountNumber;
    }

}