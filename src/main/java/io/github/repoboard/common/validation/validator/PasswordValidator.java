package io.github.repoboard.common.validation.validator;

import io.github.repoboard.common.validation.annotation.ValidPassword;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * {@link ValidPassword} 어노테이션의 실제 검증 로직.
 *
 * <p>영문, 숫자, 특수문자 포함 8자 이상 여부를 검증한다.</p>
 */
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&]).{8,}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        return value != null && PASSWORD_PATTERN.matcher(value).matches();
    }
}