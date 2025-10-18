package io.github.repoboard.security.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * 로그인 실패 시 사용자에게 적절한 에러 메시지를 제공하는 커스텀 핸들러.
 *
 * <p>Spring Security의 {@link AuthenticationFailureHandler}를 구현하여,
 * 폼 로그인 및 소셜 로그인 실패 원인에 따라 사용자 친화적인 오류 메시지를 세션에 저장한다.</p>
 */
@Component
public class CustomAuthFailureHandler implements AuthenticationFailureHandler {

    private static final Map<String, String> ERROR_MESSAGES = Map.of(
        "SUSPENDED", "정지된 계정은 로그인할 수 없습니다.",
        "DELETED", "최근 탈퇴한 계정은 7일 이후에 재가입할 수 있습니다."
    );

    /**
     * 인증 실패 시 호출되는 메서드.
     *
     * <p>예외 유형에 따라 오류 메시지를 분기 처리하고, 해당 메시지를 세션에 저장한 뒤
     * 로그인 페이지로 리다이렉트한다.</p>
     *
     * @param request 클라이언트 요청
     * @param response 서버 응답
     * @param exception 발생한 인증 예외
     * @throws IOException 리다이렉트 처리 중 I/O 예외 발생 가능
     * @throws ServletException 서블릿 예외 발생 가능
     */
    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        String msg = "이메일 또는 비밀번호가 올바르지 않습니다.";
        if(exception instanceof LockedException){
            msg = ERROR_MESSAGES.get("SUSPENDED");
        }
        else if(exception instanceof DisabledException){
            msg = ERROR_MESSAGES.get("DELETED");
        }
        else if(exception instanceof BadCredentialsException){
            msg = "존재하지 않은 계정이거나 비밀번호가 틀렸습니다.";
        }
        else if(exception instanceof OAuth2AuthenticationException oae){
            var err = oae.getError();
            if(err != null){
                String code = err.getErrorCode();
                String desc = err.getDescription();

                if(ERROR_MESSAGES.containsKey(code)){
                    msg = ERROR_MESSAGES.get(code);
                } else if(desc != null && !desc.isBlank()){
                    msg = desc;
                }
            }
            if((msg == null || msg.isBlank()) && oae.getMessage() != null){
                msg = oae.getMessage();
            }
        }

        else if (exception.getMessage() != null && !exception.getMessage().isBlank()) {
            msg = exception.getMessage();
        }

        request.getSession().setAttribute("loginError",msg);
        response.sendRedirect("/users/login");
    }
}