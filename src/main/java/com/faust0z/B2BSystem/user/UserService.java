package com.faust0z.B2BSystem.user;

import com.faust0z.B2BSystem.core.TenantContext;
import com.faust0z.B2BSystem.user.Role;
import com.faust0z.B2BSystem.user.UserAuthInternalResponse;
import com.faust0z.B2BSystem.user.UserInternalApi;
import com.faust0z.B2BSystem.user.UserResponse;
import com.faust0z.B2BSystem.tenant.domain.TenantNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.faust0z.B2BSystem.user.Role;
import com.faust0z.B2BSystem.user.UserAuthInternalResponse;
import com.faust0z.B2BSystem.user.UserInternalApi;
import com.faust0z.B2BSystem.user.UserResponse;
import com.faust0z.B2BSystem.user.UserMapper;
import com.faust0z.B2BSystem.user.UserService;
import com.faust0z.B2BSystem.user.User;
import com.faust0z.B2BSystem.user.UserRepository;
import com.faust0z.B2BSystem.user.EmailAlreadyInUseException;
import com.faust0z.B2BSystem.user.IncorrectPasswordException;
import com.faust0z.B2BSystem.user.InvalidAdminPinException;
import com.faust0z.B2BSystem.user.RoleEscalationException;
import com.faust0z.B2BSystem.user.SamePasswordException;
import com.faust0z.B2BSystem.user.UserChangeRoleRequest;
import com.faust0z.B2BSystem.user.UserCreateRequest;
import com.faust0z.B2BSystem.user.UserDisabledException;
import com.faust0z.B2BSystem.user.UserNotFoundException;
import com.faust0z.B2BSystem.user.UserUpdatePasswordRequest;
import com.faust0z.B2BSystem.user.UserUpdateRequest;
import com.faust0z.B2BSystem.user.UsernameAlreadyInUseException;
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserInternalApi {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final CacheManager cacheManager;

    @Cacheable(value = "tenant_users", key = "#contextTenantId")
    public List<UserResponse> getAllUsers(UUID contextTenantId) {
        log.debug("Fetching users");
        return userRepository.findAllByTenantId(contextTenantId, UserResponse.class);
    }

    @Cacheable(value = "users", key = "#userId")
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId, UUID contextTenantId) {
        log.debug("Fetching user by id | userId={}", userId);
        return userRepository.findByIdAndTenantId(userId, contextTenantId, UserResponse.class).orElseThrow(UserNotFoundException::new);
    }

    @CacheEvict(value = "tenant_users", key = "#contextTenantId")
    @Transactional
    @Override
    public UserResponse createUser(UserCreateRequest request, UUID contextTenantId, Role requesterRole) {
        log.debug("Processing user registration");

        if (userRepository.existsByEmail(request.email())) throw new EmailAlreadyInUseException();
        if (request.username() != null && userRepository.existsByUsername(request.username())) throw new UsernameAlreadyInUseException();
        if (requesterRole != Role.ADMIN && request.role() == Role.ADMIN) throw new RoleEscalationException();

        User userEntity = userMapper.toEntity(request);
        userEntity.setTenantId(contextTenantId);
        userEntity.setEmail(request.email().toLowerCase());
        userEntity.setPassword(passwordEncoder.encode(request.password()));
        if (request.adminPin() != null) userEntity.setAdminPin(passwordEncoder.encode(request.adminPin()));
        userEntity.setTermsAcceptedAt(Instant.now());

        userEntity = userRepository.save(userEntity);
        log.info("User created | userId={} | fullName={}", userEntity.getId(), userEntity.getFullName());
        return userMapper.toResponse(userEntity);
    }

    @Caching(
            put = @CachePut(value = "users", key = "#userId"),
            evict = {@CacheEvict(value = "tenant_users", key = "#contextTenantId"), @CacheEvict(value = "user_dnis", allEntries = true)}
    )
    @Transactional
    public UserResponse updateUser(UUID userId, UUID contextTenantId, UserUpdateRequest request) {
        log.debug("Updating user data | userId={}", userId);

        User existingUser = userRepository.findByIdAndTenantId(userId, contextTenantId, User.class).orElseThrow(UserNotFoundException::new);

        if (request.email() != null && (existingUser.getEmail() == null || !existingUser.getEmail().equalsIgnoreCase(request.email()))) {
            if (userRepository.existsByEmail(request.email())) throw new EmailAlreadyInUseException();
        }

        if (request.username() != null && (existingUser.getUsername() == null || !existingUser.getUsername().equalsIgnoreCase(request.username()))) {
            if (userRepository.existsByUsername(request.username())) throw new UsernameAlreadyInUseException();
        }

        userMapper.updateUserFromRequest(request, existingUser);
        if (existingUser.getEmail() != null) existingUser.setEmail(existingUser.getEmail().toLowerCase());

        log.debug("User data updated | userId: {}", userId);
        return userMapper.toResponse(existingUser);
    }

    @CacheEvict(value = "users", key = "#userId")
    @Transactional
    public void updateMyPassword(UUID userId, UserUpdatePasswordRequest request) {
        log.debug("Updating user password | userId={}", userId);

        User existingUser = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        if (!passwordEncoder.matches(request.currentPassword(), existingUser.getPassword())) throw new IncorrectPasswordException();
        if (passwordEncoder.matches(request.newPassword(), existingUser.getPassword())) throw new SamePasswordException();

        existingUser.setPassword(passwordEncoder.encode(request.newPassword()));

        Cache cache = cacheManager.getCache("revocations");
        if (cache != null) cache.put("user:" + userId, Instant.now().getEpochSecond());

        log.debug("Password updated | userId: {}", userId);
    }

    @Caching(
            put = @CachePut(value = "users", key = "#userId"),
            evict = {@CacheEvict(value = "tenant_users", key = "#contextTenantId"), @CacheEvict(value = "user_dnis", allEntries = true)}
    )
    @Transactional
    public UserResponse changeUserRole(UUID userId, UUID contextTenantId, UserChangeRoleRequest request, Role requesterRole) {
        log.debug("Changing user role | userId={} | newRole={}", userId, request.role());
        User userEntity = userRepository.findByIdAndTenantId(userId, contextTenantId, User.class).orElseThrow(UserNotFoundException::new);

        if (requesterRole == Role.TENANT_ADMIN && request.role() == Role.ADMIN) throw new RoleEscalationException();

        userEntity.setRole(request.role());
        Cache cache = cacheManager.getCache("revocations");
        if (cache != null) cache.put("user:" + userId, Instant.now().getEpochSecond());

        log.info("User role changed | userId={} | newRole={}", userId, userEntity.getRole());
        return userMapper.toResponse(userEntity);
    }

    @Transactional
    public void updateAdminPin(UUID userId, String pin) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        user.setAdminPin(passwordEncoder.encode(pin));
        log.info("Admin PIN updated | userId={}", userId);
    }

    @Caching(
            put = @CachePut(value = "users", key = "#userId"),
            evict = {@CacheEvict(value = "tenant_users", key = "#contextTenantId"), @CacheEvict(value = "user_dnis", allEntries = true)}
    )
    @Transactional
    public UserResponse toggleUserStatus(UUID userId, UUID contextTenantId) {
        log.debug("Toggling user status | userId={}", userId);

        User userEntity = userRepository.findByIdAndTenantId(userId, contextTenantId, User.class).orElseThrow(UserNotFoundException::new);

        userEntity.setIsActive(!userEntity.getIsActive());

        Cache cache = cacheManager.getCache("revocations");
        if (cache != null) cache.put("user:" + userId, Instant.now().getEpochSecond());

        log.info("User status toggled | userId={} | newStatus={}", userId, userEntity.getIsActive());
        return userMapper.toResponse(userEntity);
    }

    @Override
    public UserResponse getUserResponseByIdentifier(String identifier) {
        return userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(identifier, identifier, UserResponse.class)
                .orElseThrow(UserNotFoundException::new);
    }

    @Override
    public UserAuthInternalResponse getUserAuthInternalByIdentifier(String identifier) {
        UserAuthInternalResponse response = userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(identifier, identifier, UserAuthInternalResponse.class)
                .orElseThrow(UserNotFoundException::new);
        if (!response.isActive()) throw new UserDisabledException();
        return response;
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserResponseById(UUID userId) {
        return userRepository.findById(userId, UserResponse.class).orElseThrow(UserNotFoundException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public UUID getTenantIdByUserId(UUID userId) {
        UserResponse user = getUserResponseById(userId);
        if (user.tenantId() == null) throw new TenantNotFoundException();
        return user.tenantId();
    }

    @Override
    @Transactional(readOnly = true)
    public UUID validateAdminPin(UUID userId, String adminPin) {
        User user = userRepository.findByIdAndTenantId(userId, TenantContext.getTenantId(), User.class).orElseThrow(UserNotFoundException::new);
        if (user.getAdminPin() == null || !passwordEncoder.matches(adminPin, user.getAdminPin())) {
            throw new InvalidAdminPinException();
        }
        return userId;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByIdAndAdminPinNotNull(UUID userId) {
        return userRepository.existsByIdAndTenantIdAndAdminPinNotNull(userId, TenantContext.getTenantId());
    }
}
