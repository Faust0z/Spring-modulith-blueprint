package com.faust0z.B2BSystem.auth;

import com.faust0z.B2BSystem.auth.UserAuthResponse;
import com.faust0z.B2BSystem.auth.UserLoginInternalResponse;
public record UserAuthResponse(
        String token,
        UserLoginInternalResponse response
) {
}