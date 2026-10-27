package com.faust0z.B2BSystem;

import org.junit.jupiter.api.AfterEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.resend.api-key=dummy-test-key",
                "app.resend.from-email=test@test.com",
                "app.frontend.url=http://localhost:3000",
                "app.admin.email=admin@test.com",
                "app.admin.password=12345678"
        }
)
@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @AfterEach
    void clearTenantContext() {
        com.faust0z.B2BSystem.core.TenantContext.clear();
    }
}