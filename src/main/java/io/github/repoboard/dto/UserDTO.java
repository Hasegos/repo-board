package io.github.repoboard.dto;

import io.github.repoboard.model.enums.UserProvider;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDTO {

    @NotBlank(message = "아이디는 필수 입력입니다.")
    private String username;

    @NotBlank(message = "비밀번호는 필수 입력입니다.")
    private String password;
    private String role;
    private UserProvider provider;
    private String providerId;
    private String status;
}