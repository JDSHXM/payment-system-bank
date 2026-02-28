package com.paynetSystem.paynetSystemBank.auth_users.services.impl;

import com.paynetSystem.paynetSystemBank.account.entity.Account;
import com.paynetSystem.paynetSystemBank.account.services.AccountService;
import com.paynetSystem.paynetSystemBank.auth_users.dtos.LoginRequest;
import com.paynetSystem.paynetSystemBank.auth_users.dtos.LoginResponse;
import com.paynetSystem.paynetSystemBank.auth_users.dtos.RegistrationRequest;
import com.paynetSystem.paynetSystemBank.auth_users.dtos.ResetPasswordRequest;
import com.paynetSystem.paynetSystemBank.auth_users.entity.PasswordResetCode;
import com.paynetSystem.paynetSystemBank.auth_users.entity.User;
import com.paynetSystem.paynetSystemBank.auth_users.repo.PasswordResetCodeRepo;
import com.paynetSystem.paynetSystemBank.auth_users.repo.UserRepo;
import com.paynetSystem.paynetSystemBank.auth_users.services.AuthService;
import com.paynetSystem.paynetSystemBank.auth_users.services.CodeGenerator;
import com.paynetSystem.paynetSystemBank.enums.AccountType;
import com.paynetSystem.paynetSystemBank.enums.Currency;
import com.paynetSystem.paynetSystemBank.exceptions.BadRequestException;
import com.paynetSystem.paynetSystemBank.exceptions.NotFoundException;
import com.paynetSystem.paynetSystemBank.notification.dtos.NotificationDTO;
import com.paynetSystem.paynetSystemBank.notification.services.NotificationService;
import com.paynetSystem.paynetSystemBank.res.Response;
import com.paynetSystem.paynetSystemBank.role.entity.Role;
import com.paynetSystem.paynetSystemBank.role.repo.RoleRepo;
import com.paynetSystem.paynetSystemBank.security.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepo userRepo;
    private final RoleRepo roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final NotificationService notificationService;
    private final AccountService accountService;

    private final CodeGenerator codeGenerator;
    private final PasswordResetCodeRepo passwordResetCodeRepo;

    @Value("${password.reset.link}")
    private String resetLink;

    @Override
    public Response<String> register(RegistrationRequest request) {
        List<Role> roles;

        if (request.getRoles() == null || request.getRoles().isEmpty()){
            //DEFAULT TO CUSTOMER
            Role defaultRole = roleRepo.findByName("CUSTOMER")
                    .orElseThrow(()-> new NotFoundException("CUSTOMER ROLE NOT FOUND"));

            roles = Collections.singletonList(defaultRole);
        }else {
            roles = request.getRoles().stream()
                    .map(roleName-> roleRepo.findByName(roleName)
                            .orElseThrow(()-> new NotFoundException("ROLE NOT FOUND" + roleName)))
                    .toList();
        }

        if (userRepo.findByEmail(request.getEmail()).isPresent()){
            throw new BadRequestException("Email Already Present");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(roles)
                .active(true)
                .build();

        User savedUser = userRepo.save(user);

        // Автоматически сгенерировать номер счёта для пользователя.
        Account savedAccount  = accountService.createAccount(AccountType.SAVINGS, savedUser);

        // Отправить приветственное письмо

        Map<String, Object> vars = new HashMap<>();
        vars.put("name", savedUser.getFirstName());

        NotificationDTO notificationDTO = NotificationDTO.builder()
                .recipient((savedUser.getEmail()))
                .subject("Добро пожаловать в Paynet Systeam 🎉🎉🎉")
                .templateName("welcome")
                .templateVariables(vars)
                .build();

        notificationService.sendEmail(notificationDTO, savedUser);

        // ОТПРАВИТЬ ПИСЬМО СОЗДАНИЯ СЧЕТА / ДЕТАЛИ СЧЕТА
        Map<String, Object> accontVars = new HashMap<>();
        accontVars.put("name", savedUser.getFirstName());
        accontVars.put("accountNumber", savedAccount.getAccountNumber());
        accontVars.put("accountType", AccountType.SAVINGS.name());
        accontVars.put("currency", Currency.USD);


        NotificationDTO accountCreateEmail = NotificationDTO.builder()
                .recipient((savedUser.getEmail()))
                .subject("Ваш новый счет был создан ✔👌")
                .templateName("account-created")
                .templateVariables(accontVars)
                .build();

        notificationService.sendEmail(accountCreateEmail, savedUser);

        return Response.<String>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Ваш счет был успешно создан ✅")
                .data("Письмо с деталями вашего счета было отправлено вам. Ваш номер счета ✅" +
                        savedAccount.getAccountNumber())
                .build();
    }

    @Override
    public Response<LoginResponse> login(LoginRequest loginRequest) {
        String email = loginRequest.getEmail();
        String password =loginRequest.getPassword();

        User user = userRepo.findByEmail(email).orElseThrow(()-> new NotFoundException("Email Not Found"));

        if (!passwordEncoder.matches(password, user.getPassword())){
            throw new BadRequestException("Password doesn`t match");
        }

        String token = tokenService.generateToken(user.getEmail());

        LoginResponse loginResponse = LoginResponse.builder()
                .roles(user.getRoles().stream().map(Role::getName).toList())
                .token(token)
                .build();

        return Response.<LoginResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Login Sucessful")
                .data(loginResponse)
                .build();
    }

    @Override
    @Transactional
    public Response<?> forgetPassword(String email) {
       User user = userRepo.findByEmail(email).orElseThrow(()->new NotFoundException("User Not Found"));
       passwordResetCodeRepo.deleteByUserId(user.getId());

       String code = codeGenerator.generateUniqueCode();

        PasswordResetCode resetCode = PasswordResetCode.builder()
                .user(user)
                .code(code)
                .expiryDate(calculateExpiryDate())
                .used(false)
                .build();

        passwordResetCodeRepo.save(resetCode);

        //send email reset link out
        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("name", user.getFirstName());
        templateVariables.put("resetLink", resetLink + code);


        NotificationDTO notificationDTO = NotificationDTO.builder()
                .recipient(user.getEmail())
                .subject("Password Reset Code")
                .templateName("password-reset")
                .templateVariables(templateVariables)
                .build();

        notificationService.sendEmail(notificationDTO, user);


        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Код для сброса пароля отправлен на вашу электронную почту ✅")
                .build();
    }

    @Override
    @Transactional
    public Response<?> updatePasswordViaResetCode(ResetPasswordRequest resetPasswordRequest) {
        String code = resetPasswordRequest.getCode();
        String newPassword = resetPasswordRequest.getNewPassword();

        // Найдите и подтвердите код

        PasswordResetCode resetCode = passwordResetCodeRepo.findByCode(code)
                .orElseThrow(() -> new BadRequestException("Invalid reset code"));

        // Сначала проверьте срок действия ✅
        if (resetCode.getExpiryDate().isBefore(LocalDateTime.now())) {
            passwordResetCodeRepo.delete(resetCode); // Clean up expired code
            throw new BadRequestException("Reset code has expired");
        }


        //Обновите пароль ✅
        User user = resetCode.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        // Удалите код сразу после успешного использования
        passwordResetCodeRepo.delete(resetCode);


        // Отправьте письмо с подтверждением
        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("name", user.getFirstName());

        NotificationDTO confirmationEmail = NotificationDTO.builder()
                .recipient(user.getEmail())
                .subject("Password Updated Successfully")
                .templateName("password-update-confirmation")
                .templateVariables(templateVariables)
                .build();

        notificationService.sendEmail(confirmationEmail, user);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Password updated successfully")
                .build();
    }

    private LocalDateTime calculateExpiryDate(){
        return LocalDateTime.now().plusHours(5);
    }
}
