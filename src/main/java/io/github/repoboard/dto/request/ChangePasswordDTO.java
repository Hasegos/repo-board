package io.github.repoboard.dto.request;

import io.github.repoboard.common.validation.annotation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordDTO {

    @NotBlank
    private String currentPassword;

    @ValidPassword
    @NotBlank
    private String newPassword;

    @NotBlank
    private String confirmPassword;
}