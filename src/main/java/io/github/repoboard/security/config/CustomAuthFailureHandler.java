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

@Component
public class CustomAuthFailureHandler implements AuthenticationFailureHandler {

    private static final Map<String, String> ERROR_MESSAGES = Map.of(
        "SUSPENDED", "정지된 계정은 로그인할 수 없습니다.",
        "DELETED", "최근 탈퇴한 계정은 7일 이후에 재가입할 수 있습니다."
    );

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