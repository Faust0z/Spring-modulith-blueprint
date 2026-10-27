package com.faust0z.B2BSystem.user;

import com.faust0z.B2BSystem.core.TenantContext;
import com.faust0z.B2BSystem.user.Role;
import com.faust0z.B2BSystem.user.UserResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.faust0z.B2BSystem.user.UserMapper;
import com.faust0z.B2BSystem.user.UserService;
import com.faust0z.B2BSystem.user.User;
import com.faust0z.B2BSystem.user.UserRepository;
import com.faust0z.B2BSystem.user.InvalidAdminPinException;
import com.faust0z.B2BSystem.user.RoleEscalationException;
import com.faust0z.B2BSystem.user.UserChangeRoleRequest;
import com.faust0z.B2BSystem.user.UserCreateRequest;
import com.faust0z.B2BSystem.user.UserNotFoundException;
import com.faust0z.B2BSystem.user.UserUpdatePasswordRequest;
import com.faust0z.B2BSystem.user.UserUpdateRequest;
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private UserService userService;

    @Captor
    private ArgumentCaptor<User> userEntityCaptor;

    @Nested
    class GetAllUsers {
        @Test
        void getAllUsers_ValidTenantId_ReturnsUserList() {
            UUID tenantId = UUID.randomUUID();
            UserResponse response = createDefaultResponse(UUID.randomUUID(), tenantId);
            when(userRepository.findAllByTenantId(tenantId, UserResponse.class)).thenReturn(List.of(response));

            List<UserResponse> result = userService.getAllUsers(tenantId);

            assertThat(result).hasSize(1).extracting(UserResponse::email).containsExactly(response.email());
        }
    }

    @Nested
    class GetUserById {
        @Test
        void getUserById_UserExists_ReturnsResponse() {
            UUID userId = UUID.randomUUID();
            UUID tenantId = UUID.randomUUID();
            UserResponse response = createDefaultResponse(userId, tenantId);
            when(userRepository.findByIdAndTenantId(userId, tenantId, UserResponse.class)).thenReturn(Optional.of(response));

            UserResponse result = userService.getUserById(userId, tenantId);

            assertThat(result.id()).isEqualTo(userId);
        }

        @Test
        void getUserById_UserDoesNotExist_ThrowsException() {
            UUID userId = UUID.randomUUID();
            UUID tenantId = UUID.randomUUID();
            when(userRepository.findByIdAndTenantId(userId, tenantId, UserResponse.class)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(userId, tenantId))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    class CreateUser {
        @Test
        void createUser_ValidRequest_SavesAndReturnsResponse() {
            UUID tenantId = UUID.randomUUID();
            UserCreateRequest request = new UserCreateRequest("Facundo Perez", "facundo@gym.com", "user123",
                    "pass", Role.TENANT_USER, null);
            User entity = createDefaultEntity(tenantId);
            UserResponse expectedResponse = createDefaultResponse(entity.getId(), tenantId);

            when(userRepository.existsByEmail(request.email())).thenReturn(false);
            when(userRepository.existsByUsername(request.username())).thenReturn(false);
            when(userMapper.toEntity(request)).thenReturn(entity);
            when(passwordEncoder.encode(request.password())).thenReturn("encodedPass");
            when(userRepository.save(entity)).thenReturn(entity);
            when(userMapper.toResponse(entity)).thenReturn(expectedResponse);

            UserResponse result = userService.createUser(request, tenantId, Role.TENANT_ADMIN);

            assertThat(result).isEqualTo(expectedResponse);
            verify(userRepository).save(userEntityCaptor.capture());
            User savedEntity = userEntityCaptor.getValue();
            assertThat(savedEntity.getTenantId()).isEqualTo(tenantId);
            assertThat(savedEntity.getEmail()).isEqualTo("facundo@gym.com");
            assertThat(savedEntity.getPassword()).isEqualTo("encodedPass");
        }

        @Test
        void createUser_RoleEscalationAttempted_ThrowsException() {
            UserCreateRequest request = new UserCreateRequest("Facundo Perez", null, null, null,
                    Role.ADMIN, null);

            assertThatThrownBy(() -> userService.createUser(request, UUID.randomUUID(), Role.TENANT_ADMIN))
                    .isInstanceOf(RoleEscalationException.class);
        }

        @Test
        void createUser_AdminCreatesTenantAdmin_SetsTermsAcceptedAt() {
            UUID tenantId = UUID.randomUUID();
            UserCreateRequest request = new UserCreateRequest("Juan Perez", "juan@gimnasio.com.ar", "user123",
                    "pass", Role.TENANT_ADMIN, null);
            User entity = createDefaultEntity(tenantId);

            when(userMapper.toEntity(request)).thenReturn(entity);
            when(userRepository.save(any())).thenReturn(entity);
            when(userMapper.toResponse(entity)).thenReturn(createDefaultResponse(entity.getId(), tenantId));

            userService.createUser(request, tenantId, Role.ADMIN);

            assertThat(entity.getTermsAcceptedAt()).isNotNull();
        }
    }

    @Nested
    class UpdateUser {
        @Test
        void updateUser_ValidRequest_UpdatesAndReturnsResponse() {
            UUID userId = UUID.randomUUID();
            UUID tenantId = UUID.randomUUID();
            UserUpdateRequest request = new UserUpdateRequest("Facu Perez", "new@gym.com", "newuser");
            User existingEntity = createDefaultEntity(tenantId);
            UserResponse expectedResponse = createDefaultResponse(userId, tenantId);

            when(userRepository.findByIdAndTenantId(userId, tenantId, User.class)).thenReturn(Optional.of(existingEntity));
            when(userRepository.existsByEmail(request.email())).thenReturn(false);
            when(userRepository.existsByUsername(request.username())).thenReturn(false);
            when(userMapper.toResponse(existingEntity)).thenReturn(expectedResponse);

            UserResponse result = userService.updateUser(userId, tenantId, request);

            assertThat(result).isEqualTo(expectedResponse);
            verify(userMapper).updateUserFromRequest(request, existingEntity);
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    class UpdateMyPassword {
        @Test
        void updateMyPassword_ValidPasswords_UpdatesPasswordAndEvictsCache() {
            UUID userId = UUID.randomUUID();
            UserUpdatePasswordRequest request = new UserUpdatePasswordRequest("oldPass", "newPass");
            User entity = createDefaultEntity(UUID.randomUUID());
            entity.setPassword("encoded_oldPass");
            Cache cacheMock = mock(Cache.class);

            when(userRepository.findById(userId)).thenReturn(Optional.of(entity));
            when(passwordEncoder.matches(request.currentPassword(), entity.getPassword())).thenReturn(true);
            when(passwordEncoder.matches(request.newPassword(), entity.getPassword())).thenReturn(false);
            when(passwordEncoder.encode(request.newPassword())).thenReturn("encoded_newPass");
            when(cacheManager.getCache("revocations")).thenReturn(cacheMock);

            userService.updateMyPassword(userId, request);

            assertThat(entity.getPassword()).isEqualTo("encoded_newPass");
            verify(userRepository, never()).save(any());
            verify(cacheMock).put(eq("user:" + userId), anyLong());
        }
    }

    @Nested
    class ChangeUserRole {
        @Test
        void changeUserRole_StandardRoleChange_UpdatesRoleAndEvictsCache() {
            UUID userId = UUID.randomUUID();
            UUID tenantId = UUID.randomUUID();
            UserChangeRoleRequest request = new UserChangeRoleRequest(Role.TENANT_ADMIN);
            User entity = createDefaultEntity(tenantId);
            Cache cacheMock = mock(Cache.class);

            when(userRepository.findByIdAndTenantId(userId, tenantId, User.class)).thenReturn(Optional.of(entity));
            when(userMapper.toResponse(entity)).thenReturn(createDefaultResponse(userId, tenantId));
            when(cacheManager.getCache("revocations")).thenReturn(cacheMock);

            userService.changeUserRole(userId, tenantId, request, Role.ADMIN);

            assertThat(entity.getRole()).isEqualTo(Role.TENANT_ADMIN);
            verify(userRepository, never()).save(any());
            verify(cacheMock).put(eq("user:" + userId), anyLong());
        }
    }

    @Nested
    class ToggleUserStatus {
        @Test
        void toggleUserStatus_UserExists_InvertsIsActive() {
            UUID userId = UUID.randomUUID();
            UUID tenantId = UUID.randomUUID();
            User entity = createDefaultEntity(tenantId);
            entity.setIsActive(true);
            Cache cacheMock = mock(Cache.class);

            when(userRepository.findByIdAndTenantId(userId, tenantId, User.class)).thenReturn(Optional.of(entity));
            when(userMapper.toResponse(entity)).thenReturn(createDefaultResponse(userId, tenantId));
            when(cacheManager.getCache("revocations")).thenReturn(cacheMock);

            userService.toggleUserStatus(userId, tenantId);

            assertThat(entity.getIsActive()).isFalse();
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    class ValidateAdminPin {
        private final UUID tenantId = UUID.randomUUID();

        @AfterEach
        void tearDown() {
            TenantContext.clear();
        }

        @Test
        void validateAdminPin_CorrectPin_ReturnsUserId() {
            UUID userId = UUID.randomUUID();
            User entity = createDefaultEntity(tenantId);
            entity.setAdminPin("encodedPin");
            TenantContext.setTenantId(tenantId);

            when(userRepository.findByIdAndTenantId(userId, tenantId, User.class)).thenReturn(Optional.of(entity));
            when(passwordEncoder.matches("123456", "encodedPin")).thenReturn(true);

            UUID result = userService.validateAdminPin(userId, "123456");

            assertThat(result).isEqualTo(userId);
        }

        @Test
        void validateAdminPin_WrongPin_ThrowsInvalidAdminPin() {
            UUID userId = UUID.randomUUID();
            User entity = createDefaultEntity(tenantId);
            entity.setAdminPin("encodedPin");
            TenantContext.setTenantId(tenantId);

            when(userRepository.findByIdAndTenantId(userId, tenantId, User.class)).thenReturn(Optional.of(entity));
            when(passwordEncoder.matches("000000", "encodedPin")).thenReturn(false);

            assertThatThrownBy(() -> userService.validateAdminPin(userId, "000000"))
                    .isInstanceOf(InvalidAdminPinException.class);
        }

        @Test
        void validateAdminPin_NoPinSet_ThrowsInvalidAdminPin() {
            UUID userId = UUID.randomUUID();
            User entity = createDefaultEntity(tenantId);
            entity.setAdminPin(null);
            TenantContext.setTenantId(tenantId);

            when(userRepository.findByIdAndTenantId(userId, tenantId, User.class)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> userService.validateAdminPin(userId, "123456"))
                    .isInstanceOf(InvalidAdminPinException.class);
        }

        @Test
        void validateAdminPin_UserNotFound_ThrowsUserNotFound() {
            UUID userId = UUID.randomUUID();
            TenantContext.setTenantId(tenantId);

            when(userRepository.findByIdAndTenantId(userId, tenantId, User.class)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.validateAdminPin(userId, "123456"))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    private User createDefaultEntity(UUID tenantId) {
        User entity = new User();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(tenantId);
        entity.setEmail("juan@gimnasio.com.ar");
        entity.setFullName("Juan Perez");
        entity.setUsername("user");
        entity.setIsActive(true);
        entity.setRole(Role.TENANT_USER);
        return entity;
    }

    private UserResponse createDefaultResponse(UUID userId, UUID tenantId) {
        return new UserResponse(userId, tenantId, "Juan Perez", "juan@gimnasio.com.ar", "user123",
                Role.TENANT_USER, true);
    }
}
