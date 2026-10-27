package com.faust0z.B2BSystem.core;

import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class HibernateTenancyConfig {

    @Bean
    HibernatePropertiesCustomizer tenantIdentifierCustomizer(TenantIdentifierResolver resolver) {
        return hibernateProperties ->
                hibernateProperties.put("hibernate.tenant_identifier_resolver", resolver);
    }
}