package com.faust0z.B2BSystem;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import org.hibernate.annotations.TenantId;
import org.springframework.data.jpa.repository.Query;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

@AnalyzeClasses(packages = "com.faust0z.B2BSystem")
class TenancyEnforcementTest {

    @ArchTest
    static final ArchRule every_domain_entity_must_have_tenant_id_field = classes()
            .that().areAnnotatedWith(Entity.class)
            .and().haveNameNotMatching(".*User")
            .and().haveNameNotMatching(".*SubscriptionPlanEntity")
            .and().haveNameNotMatching(".*Tenant")
            .and().haveNameNotMatching(".*EmailVerificationTokenEntity")
            .should(haveFieldAnnotatedWithTenantId())
            .because("Hibernate requires @TenantId on a field for row-level filtering.");

    @ArchTest
    static final ArchRule native_queries_must_contain_tenant_id_filter = methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Repository")
            .and().areAnnotatedWith(Query.class)
            .should(containTenantIdFilterIfNative())
            .because("Native queries bypass Hibernate multi-tenancy filters and must enforce manual isolation limits.");

    private static ArchCondition<JavaClass> haveFieldAnnotatedWithTenantId() {
        return new ArchCondition<>("have a field annotated with @TenantId (including inherited)") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                boolean hasTenantId = item.getFields().stream()
                        .anyMatch(field -> field.isAnnotatedWith(TenantId.class));

                // Also check parent classes
                if (!hasTenantId) {
                    var parent = item.getSuperclass();
                    while (parent.isPresent()) {
                        JavaClass parentClass = parent.get().toErasure();
                        if (parentClass.getFields().stream().anyMatch(field -> field.isAnnotatedWith(TenantId.class))) {
                            hasTenantId = true;
                            break;
                        }
                        parent = parentClass.getSuperclass();
                    }
                }

                if (!hasTenantId) {
                    events.add(SimpleConditionEvent.violated(item,
                            item.getName() + " is missing @TenantId field (including inherited)"));
                }
            }
        };
    }

    private static ArchCondition<JavaMethod> containTenantIdFilterIfNative() {
        return new ArchCondition<>("contain 'tenant_id = :tenantId' parameter constraint inside native query strings") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                Query query = method.getAnnotationOfType(Query.class);

                if (query.nativeQuery()) {
                    String rawSql = query.value().toLowerCase().replaceAll("\\s+", "");
                    boolean hasTenantIdParam = rawSql.contains("tenant_id=:tenantid");

                    if (method.getName().equals("closeUnfinishedShifts") && method.getOwner().getSimpleName().equals("ShiftRepository")) {
                        return;
                    }
if (method.getOwner().getSimpleName().equals("BillingCycleRepository") &&
                            (method.getName().equals("findActivesNative") ||
                             method.getName().equals("findTenantIdsWithExpiredActive") ||
                             method.getName().equals("findActiveIds"))) {
                        return;
                    }

                    if (!hasTenantIdParam) {
                        String failureMessage = String.format("Repository method %s.%s contains native query missing 'tenant_id = :tenantId' validation filter",
                                method.getOwner().getSimpleName(), method.getName());
                        events.add(SimpleConditionEvent.violated(method, failureMessage));
                    }
                }
            }
        };
    }
}