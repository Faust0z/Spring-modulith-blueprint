package com.faust0z.B2BSystem.auth;

import com.faust0z.B2BSystem.user.UserAuthInternalResponse;
import com.faust0z.B2BSystem.user.UserInternalApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.faust0z.B2BSystem.auth.CustomUserDetails;
import com.faust0z.B2BSystem.auth.CustomUserDetailsService;
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserInternalApi userInternalApi;

    // "username" = user email.
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalizedIdentifier = username.toLowerCase();
        try {
            UserAuthInternalResponse userAuthResponse = userInternalApi.getUserAuthInternalByIdentifier(normalizedIdentifier);
            return new CustomUserDetails(userAuthResponse);
        } catch (Exception e) {
            throw new UsernameNotFoundException("User not found with identifier: " + username);
        }
    }
}
