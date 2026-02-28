package com.paynetSystem.paynetSystemBank.transaction.services;

import com.paynetSystem.paynetSystemBank.account.entity.Account;
import com.paynetSystem.paynetSystemBank.account.repo.AccountRepo;
import com.paynetSystem.paynetSystemBank.auth_users.entity.User;
import com.paynetSystem.paynetSystemBank.auth_users.services.UserService;
import com.paynetSystem.paynetSystemBank.enums.TransactionStatus;
import com.paynetSystem.paynetSystemBank.enums.TransactionType;
import com.paynetSystem.paynetSystemBank.exceptions.BadRequestException;
import com.paynetSystem.paynetSystemBank.exceptions.InsufficientBalanceException;
import com.paynetSystem.paynetSystemBank.exceptions.InvalidTransactionException;
import com.paynetSystem.paynetSystemBank.exceptions.NotFoundException;
import com.paynetSystem.paynetSystemBank.notification.dtos.NotificationDTO;
import com.paynetSystem.paynetSystemBank.notification.services.NotificationService;
import com.paynetSystem.paynetSystemBank.res.Response;
import com.paynetSystem.paynetSystemBank.transaction.dtos.TransactionDTO;
import com.paynetSystem.paynetSystemBank.transaction.dtos.TransactionRequest;
import com.paynetSystem.paynetSystemBank.transaction.entity.Transaction;
import com.paynetSystem.paynetSystemBank.transaction.repo.TransactionRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepo transactionRepo;
    private final AccountRepo accountRepo;
    private final NotificationService notificationService;
    private final UserService userService;
    private final ModelMapper modelMapper;


    @Override
    @Transactional
    public Response<?> createTransaction(TransactionRequest transactionRequest) {

        Transaction transaction = new Transaction();

        transaction.setTransactionType(transactionRequest.getTransactionType());
        transaction.setAmount(transactionRequest.getAmount());
        transaction.setDescription(transactionRequest.getDescription());

        switch (transactionRequest.getTransactionType()) {
            case DEPOSIT -> handleDeposit(transactionRequest, transaction);
            case WITHDRAWAL -> handleWithdrawal(transactionRequest, transaction);
            case TRANSFER -> handleTransfer(transactionRequest, transaction);
            default -> throw new InvalidTransactionException("Invalid transaction type");
        }

        transaction.setStatus(TransactionStatus.SUCCESS);
        Transaction savedTxn = transactionRepo.save(transaction);

        //отправить уведомление
        sendTransactionNotifications(savedTxn);


        return Response.builder()
                .statusCode(200)
                .message("Transaction successful")
                .build();
    }

    @Override
    @Transactional
    public Response<List<TransactionDTO>> getTransactionsForMyAccount(String accountNumber, int page, int size) {

        // Получить текущего вошедшего пользователя
        User user = userService.getCurrentLoggedInUser();

        // Найти счет по его номеру
        Account account = accountRepo.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new NotFoundException("Account not found"));

        // Убедитесь, что счет принадлежит пользователю. Дополнительная проверка безопасности
        if (!account.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Account does not belong to the authenticated user");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        Page<Transaction> txns = transactionRepo.findByAccount_AccountNumber(accountNumber, pageable);

        List<TransactionDTO> transactionDTOS = txns.getContent().stream()
                .map(transaction -> modelMapper.map(transaction, TransactionDTO.class))
                .toList();

        return Response.<List<TransactionDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Transactions retrieved")
                .data(transactionDTOS)
                .meta(Map.of(
                        "currentPage", txns.getNumber(),
                        "totalItems", txns.getTotalElements(),
                        "totalPages", txns.getTotalPages(),
                        "pageSize", txns.getSize()
                ))
                .build();
    }

    private void handleDeposit(TransactionRequest request, Transaction transaction) {

        Account account = accountRepo.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new NotFoundException("Account not found"));

        account.setBalance(account.getBalance().add(request.getAmount()));
        transaction.setAccount(account);
        accountRepo.save(account);
    }

    private void handleWithdrawal(TransactionRequest request, Transaction transaction) {

        Account account = accountRepo.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new NotFoundException("Account not found"));

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        transaction.setAccount(account);
        accountRepo.save(account);
    }

    private void handleTransfer(TransactionRequest request, Transaction transaction) {

        Account sourceAccount = accountRepo.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new NotFoundException("Account not found"));

        Account destination = accountRepo.findByAccountNumber(request.getDestinationAccountNumber())
                .orElseThrow(() -> new NotFoundException("Destination Account not found"));

        if (sourceAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance in source account");

        }

        // Снять с источника
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(request.getAmount()));
        accountRepo.save(sourceAccount);


        // Добавить на счет получателя
        destination.setBalance(destination.getBalance().add(request.getAmount()));
        accountRepo.save(destination);

        transaction.setAccount(sourceAccount);
        transaction.setSourceAccount(sourceAccount.getAccountNumber());
        transaction.setDestinationAccount(destination.getAccountNumber());
    }


    private void sendTransactionNotifications(Transaction tnx) {

        User user = tnx.getAccount().getUser();
        String subject;
        String template;

        Map<String, Object> templateVariables = new HashMap<>();

        templateVariables.put("name", user.getFirstName());
        templateVariables.put("amount", tnx.getAmount());
        templateVariables.put("accountNumber", tnx.getAccount().getAccountNumber());
        templateVariables.put("date", tnx.getTransactionDate());
        templateVariables.put("balance", tnx.getAccount().getBalance());

        if (tnx.getTransactionType() == TransactionType.DEPOSIT) {
            subject = "Credit Alert";
            template = "credit-alert";

            NotificationDTO notificationEmailToSendOut = NotificationDTO.builder()
                    .recipient(user.getEmail())
                    .subject(subject)
                    .templateName(template)
                    .templateVariables(templateVariables)
                    .build();
            notificationService.sendEmail(notificationEmailToSendOut, user);

        } else if (tnx.getTransactionType() == TransactionType.WITHDRAWAL) {

            subject = "Debit Alert";
            template = "debit-alert";

            NotificationDTO notificationEmailToSendOut = NotificationDTO.builder()
                    .recipient(user.getEmail())
                    .subject(subject)
                    .templateName(template)
                    .templateVariables(templateVariables)
                    .build();

            notificationService.sendEmail(notificationEmailToSendOut, user);
        } else if (tnx.getTransactionType() == TransactionType.TRANSFER) {

            subject = "Debit Alert";
            template = "debit-alert";


            NotificationDTO notificationEmailToSendOut = NotificationDTO.builder()
                    .recipient(user.getEmail())
                    .subject(subject)
                    .templateName(template)
                    .templateVariables(templateVariables)
                    .build();

            notificationService.sendEmail(notificationEmailToSendOut, user);


            // Получателю УВЕДОМЛЕНИЕ О КРЕДИТЕ
            Account destination = accountRepo.findByAccountNumber(tnx.getDestinationAccount())
                    .orElseThrow(() -> new NotFoundException("Destination account not found"));


            User receiver = destination.getUser();


            Map<String, Object> recvVars = new HashMap<>();
            recvVars.put("name", receiver.getFirstName());
            recvVars.put("amount", tnx.getAmount());
            recvVars.put("accountNumber", destination.getAccountNumber());
            recvVars.put("date", tnx.getTransactionDate());
            recvVars.put("balance", destination.getBalance());


            NotificationDTO notificationEmailToSendOutToReveiver = NotificationDTO.builder()
                    .recipient(receiver.getEmail())
                    .subject("Credit Alert")
                    .templateName("credit-alert")
                    .templateVariables(recvVars)
                    .build();

            notificationService.sendEmail(notificationEmailToSendOutToReveiver, user);
        }
    }
}