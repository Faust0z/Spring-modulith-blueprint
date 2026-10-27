package com.faust0z.B2BSystem.auth;

import com.faust0z.B2BSystem.auth.ClientPortalLoginResponse;
import com.faust0z.B2BSystem.auth.PortalLoginResult;
public record PortalLoginResult(
        String token,
        ClientPortalLoginResponse response
) {
}
