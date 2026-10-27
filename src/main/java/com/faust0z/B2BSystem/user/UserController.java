package com.faust0z.B2BSystem.user;

import com.faust0z.B2BSystem.core.RateLimiterService;
import com.faust0z.B2BSystem.core.RequireAdmin;
import com.faust0z.B2BSystem.core.RequirePinAuthorization;
import com.faust0z.B2BSystem.core.TenantContext;
import com.faust0z.B2BSystem.user.Role;
import com.faust0z.B2BSystem.user.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import com.faust0z.B2BSystem.user.Role;
import com.faust0z.B2BSystem.user.UserResponse;
import com.faust0z.B2BSystem.user.UserService;
import com.faust0z.B2BSystem.user.UserChangeRoleRequest;
import com.faust0z.B2BSystem.user.UserController;
import com.faust0z.B2BSystem.user.UserCreateRequest;
import com.faust0z.B2BSystem.user.UserUpdatePasswordRequest;
import com.faust0z.B2BSystem.user.UserUpdatePinRequest;
import com.faust0z.B2BSystem.user.UserUpdateRequest;
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/users")
@Tag(name = "Users", description = "Tenant's users management")
public class UserController {

    private final UserService userService;
    private final RateLimiterService rateLimiterService;

    @Operation(summary = "Get all users. Requires ADMIN or TENANT_ADMIN role")
    @ApiResponse(responseCode = "200", description = "Usuarios obtenidos correctamente")
    @RequireAdmin
    @GetMapping
    List<UserResponse> getAllUsers() {
        return userService.getAllUsers(TenantContext.getTenantId());
    }

    @Operation(summary = "Get a single user by ID. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @RequireAdmin
    @GetMapping("/{userId}")
    UserResponse getUserById(@PathVariable UUID userId) {
        UUID contextTenantId = TenantContext.getTenantId();
        return userService.getUserById(userId, contextTenantId);
    }

    @Operation(summary = "Get the current logged user's data")
    @ApiResponse(responseCode = "200", description = "Usuario encontrado")
    @RequirePinAuthorization
    @GetMapping("/me")
    UserResponse getMyDetails(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        UUID contextTenantId = TenantContext.getTenantId();
        return userService.getUserById(userId, contextTenantId);
    }

    @Operation(summary = "Create a new user. Requires ADMIN or TENANT_ADMIN role")
    @ApiResponse(responseCode = "201", description = "Usuario creado correctamente")
    @ApiResponse(responseCode = "400", description = "Entrada inválida o correo ya existente")
    @RequireAdmin
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse createUser(@Valid @RequestBody UserCreateRequest request, @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = TenantContext.getTenantId();
        Role requesterRole = Role.valueOf(jwt.getClaimAsString("role"));
        return userService.createUser(request, tenantId, requesterRole);
    }

    @Operation(summary = "Patch a user's data. Requires ADMIN or TENANT_ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Entrada inválida")
    })
    @RequirePinAuthorization
    @PatchMapping("/{userId}")
    UserResponse updateUser(@PathVariable UUID userId, @Valid @RequestBody UserUpdateRequest request) {
        UUID contextTenantId = TenantContext.getTenantId();
        return userService.updateUser(userId, contextTenantId, request);
    }

    @Operation(summary = "Update the current user's password")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Contraseña actualizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Contraseña actual inválida o formato de la nueva contraseña inválido")
    })
    @RequirePinAuthorization
    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void updateMyPassword(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UserUpdatePasswordRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        rateLimiterService.checkLimit("password-update:" + userId, 5, 1, Duration.ofMinutes(15));
        userService.updateMyPassword(userId, request);
    }

    @Operation(summary = "Update a user's role. Requires ADMIN or TENANT_ADMIN.")
    @ApiResponse(responseCode = "200", description = "Rol del usuario actualizado correctamente")
    @RequireAdmin
    @PatchMapping("/{userId}/role")
    UserResponse updateUserRole(@PathVariable UUID userId, @Valid @RequestBody UserChangeRoleRequest request, @AuthenticationPrincipal Jwt jwt) {
        UUID contextTenantId = TenantContext.getTenantId();
        Role requesterRole = Role.valueOf(jwt.getClaimAsString("role"));
        return userService.changeUserRole(userId, contextTenantId, request, requesterRole);
    }

    @Operation(summary = "Update the current user's pin")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Pin actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Entrada inválida")
    })
    @PreAuthorize("@pinAuthorizationSecurity.isElevatedOrInitialSetup(#jwt.subject)")
    @PutMapping("/me/admin-pin")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void updateMyAdminPin(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UserUpdatePinRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        rateLimiterService.checkLimit("pin-update:" + userId, 5, 1, Duration.ofMinutes(15));
        userService.updateAdminPin(userId, request.adminPin());
    }

    @Operation(summary = "Disable a user (Soft Delete). Requires ADMIN or TENANT_ADMIN.")
    @ApiResponse(responseCode = "200", description = "Estado del usuario actualizado correctamente")
    @RequireAdmin
    @PatchMapping("/{userId}/status")
    UserResponse toggleUserStatus(@PathVariable UUID userId) {
        UUID contextTenantId = TenantContext.getTenantId();
        return userService.toggleUserStatus(userId, contextTenantId);
    }
}

