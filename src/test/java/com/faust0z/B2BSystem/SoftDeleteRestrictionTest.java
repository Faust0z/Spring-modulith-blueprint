package com.faust0z.B2BSystem;

import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import org.hibernate.annotations.SQLRestriction;
import com.faust0z.B2BSystem.core.TenantSoftDeletableEntity;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "com.faust0z.B2BSystem")
class SoftDeleteRestrictionTest {

    @ArchTest
    static final ArchRule soft_deletable_entities_have_sql_restriction = classes()
        .that().areAnnotatedWith(Entity.class)
        .and().areAssignableTo(TenantSoftDeletableEntity.class)
        .should(haveSqlRestrictionOnDeletedAt())
        .because("Every @Entity subclass of TenantSoftDeletableEntity must carry @SQLRestriction(\"deleted_at IS NULL\")");

    private static ArchCondition<JavaClass> haveSqlRestrictionOnDeletedAt() {
        return new ArchCondition<>("have @SQLRestriction(\"deleted_at IS NULL\")") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                boolean hasRestriction = item.getAnnotations().stream()
                        .filter(a -> "org.hibernate.annotations.SQLRestriction".equals(a.getType().getName()))
                        .anyMatch(a -> {
                            Object value = a.get("value").orElse(null);
                            return value != null && "deleted_at IS NULL".equals(value.toString());
                        });

                if (!hasRestriction) {
                    events.add(SimpleConditionEvent.violated(item,
                        item.getName() + " must carry @SQLRestriction(\"deleted_at IS NULL\")"));
                }
            }
        };
    }
}