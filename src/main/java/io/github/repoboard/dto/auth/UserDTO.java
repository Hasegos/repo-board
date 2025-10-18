package io.github.repoboard.dto.auth;

import io.github.repoboard.common.validation.annotation.ValidPassword;
import io.github.repoboard.common.validation.annotation.ValidUsername;
import io.github.repoboard.model.enums.UserProvider;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 사용자 등록(회원가입) 요청 DTO.
 */
@Getter
@Setter
public class UserDTO {

    @NotBlank(message = "아이디는 필수 입력입니다.")
    @ValidUsername
    private String username;

    @NotBlank(message = "비밀번호는 필수 입력입니다.")
    @ValidPassword
    private String password;

    @ValidPassword
    private String passwordConfirm;

    private String role;
    private UserProvider provider;
    private String providerId;
    private String status;
}