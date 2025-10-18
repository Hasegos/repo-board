package io.github.repoboard.common.validation.annotation;

import io.github.repoboard.common.validation.validator.PasswordValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 비밀번호 유효성 검사용 커스텀 검증 어노테이션.
 *
 * <p>영문, 숫자, 특수문자를 포함한 8자 이상이어야 한다.</p>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Constraint(validatedBy = PasswordValidator.class)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ValidPassword {
    String message() default "비밀번호 최소 8자 이상, 영문, 숫자, 특수문자를 포함해야 합니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}