package io.github.repoboard.model.enums;

/**
 * 사용자 역할(Role)을 나타내는 열거형(enum).
 *
 * <p>Spring Security 권한 시스템에서 사용된다.</p>
 */
public enum UserRoleType {

    /** 관리자 권한 */
    ROLE_ADMIN,

    /** 일반 사용자 권한 */
    ROLE_USER
}