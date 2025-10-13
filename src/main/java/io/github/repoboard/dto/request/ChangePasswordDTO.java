package io.github.repoboard.dto.request;

import io.github.repoboard.common.validation.annotation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 사용자 비밀번호 변경 요청 DTO.
 */
@Getter
@Setter
public class ChangePasswordDTO {

    /** 현재 비밀번호 */
    @NotBlank
    private String currentPassword;

    /** 새 비밀번호 (형식 검증 포함) */
    @ValidPassword
    @NotBlank
    private String newPassword;

    /** 새 비밀번호 확인 */
    @NotBlank
    private String confirmPassword;
}