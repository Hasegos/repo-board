package io.github.repoboard.model.enums;

/**
 * 사용자 인증 수단(OAuth2 또는 로컬 로그인) 제공자 enum.
 *
 * <p>회원 가입 또는 로그인 시 어떤 인증 방식으로 가입했는지를 식별하는 데 사용된다.</p>
 */
public enum UserProvider {

    /** Google OAuth2 로그인 */
    GOOGLE,

    /** GitHub OAuth2 로그인 */
    GITHUB,

    /** 일반 Form 로그인 (자체 회원가입) */
    LOCAL
}