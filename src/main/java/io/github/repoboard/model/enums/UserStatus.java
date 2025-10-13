package io.github.repoboard.model.enums;

/**
 * 사용자 계정 상태를 나타내는 열거형(enum).
 *
 * <p>회원의 사용 가능 여부 및 시스템 동작 결정에 사용된다.</p>
 */
public enum UserStatus {

    /** 활성 상태 (정상적으로 서비스 이용 가능) */
    ACTIVE,

    /** 정지 상태 (관리자에 의해 차단된 계정) */
    SUSPENDED,

    /** 삭제 상태 (사용자 탈퇴 또는 삭제 처리됨) */
    DELETED
}