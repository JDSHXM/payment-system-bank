package com.paynetSystem.paynetSystemBank.auth_users.services;

import com.paynetSystem.paynetSystemBank.auth_users.dtos.LoginRequest;
import com.paynetSystem.paynetSystemBank.auth_users.dtos.LoginResponse;
import com.paynetSystem.paynetSystemBank.auth_users.dtos.RegistrationRequest;
import com.paynetSystem.paynetSystemBank.auth_users.dtos.ResetPasswordRequest;
import com.paynetSystem.paynetSystemBank.res.Response;

public interface AuthService {
    Response<String> register(RegistrationRequest request);

    Response<LoginResponse> login(LoginRequest loginRequest);

    Response<? > forgetPassword(String email);

    Response<? > updatePasswordViaResetCode(ResetPasswordRequest resetPasswordRequest);
}
