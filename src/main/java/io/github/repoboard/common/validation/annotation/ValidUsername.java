package io.github.repoboard.common.validation.annotation;

import io.github.repoboard.common.validation.validator.UsernameValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 이메일 형식의 사용자명 유효성 검증용 어노테이션.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Constraint(validatedBy = UsernameValidator.class)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ValidUsername {
    String message() default "유효한 이메일 형식을 입력해주세요.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}