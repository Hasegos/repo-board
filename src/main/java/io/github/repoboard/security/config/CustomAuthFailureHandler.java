package io.github.repoboard.security.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {

        String msg = "이메일 또는 비밀번호가 올바르지 않습니다.";
        if(exception instanceof LockedException){
            msg = "정지된 계정입니다.";
        }else if(exception instanceof DisabledException){
            msg = "탈퇴(비활성화된) 계정입니다.";
        } else if(exception instanceof OAuth2AuthenticationException oae){
            msg = "소셜 로그인에 실패했습니다. 잠시 후 다시 시도해주세요.";
            var err = oae.getError();
            if(err != null){
                String code = err.getErrorCode();
                String desc = err.getDescription();
                if ("ACCOUNT_SUSPENDED".equals(code))      msg = (desc != null) ? desc : "정지된 계정입니다.";
                else if ("ACCOUNT_DELETED".equals(code))   msg = (desc != null) ? desc : "탈퇴(비활성)된 계정입니다.";
            }
        }
        request.getSession().setAttribute("loginError",msg);
        response.sendRedirect("/users/login");
    }
}