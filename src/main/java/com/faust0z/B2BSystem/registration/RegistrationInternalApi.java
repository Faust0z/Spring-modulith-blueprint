package com.faust0z.B2BSystem.registration;

import com.faust0z.B2BSystem.auth.UserAuthResponse;

import java.util.UUID;

import com.faust0z.B2BSystem.registration.RegistrationInternalApi;
import com.faust0z.B2BSystem.registration.TenantRegisterRequest;
public interface RegistrationInternalApi {
    UserAuthResponse register(TenantRegisterRequest request);

    void validateEmail(String token);

    void resendVerificationEmail(UUID tenantId);
}
