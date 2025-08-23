package io.github.repoboard.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordDTO {

    private String currentPassword;
    private String newPassword;
    private String confirmPassword;
}