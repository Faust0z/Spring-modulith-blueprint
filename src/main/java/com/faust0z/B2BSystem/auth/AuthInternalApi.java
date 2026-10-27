package com.faust0z.B2BSystem.auth;

import com.faust0z.B2BSystem.auth.AuthInternalApi;
import com.faust0z.B2BSystem.auth.UserAuthResponse;
import com.faust0z.B2BSystem.auth.UserLoginRequest;
public interface AuthInternalApi {
    UserAuthResponse login(UserLoginRequest loginRequest);
}
