package io.github.repoboard.service;

import io.github.repoboard.dto.request.ChangePasswordDTO;
import io.github.repoboard.model.User;
import io.github.repoboard.security.core.CustomUserPrincipal;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

/**
 * 사용자 설정 관련 서비스.
 * <p>비밀번호 변경, 로그아웃, 회원 삭제, 사용자 조회 등을 담당한다.</p>
 */
@Service
@RequiredArgsConstructor
public class SettingService {

    private final UserService userService;
    private final OAuth2AuthorizedClientService clientService;

    /**
     * 비밀번호 변경 처리.
     *
     * @param userId 사용자 ID
     * @param change 비밀번호 변경 DTO
     * @param br     BindingResult (에러 발생 시 reject 처리)
     */
    public void changeUserPassword(Long userId,
                                   ChangePasswordDTO change,
                                   BindingResult br){
        try{
            userService.changeUserPassword(userId, change);
        }catch (BadCredentialsException e) {
            br.rejectValue("currentPassword", "bad", "현재 비밀번호가 올바르지 않습니다.");
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("동일")) {
                br.rejectValue("newPassword", "same", e.getMessage());
            }
            else if (e.getMessage().contains("확인")) {
                br.rejectValue("confirmPassword", "mismatch", e.getMessage());
            }
            else {
                br.reject("error", e.getMessage());
            }
        } catch (AccessDeniedException e) {
            br.reject("denied", "소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다.");
        }
    }

    /**
     * 비밀번호 변경 성공 후 세션 로그아웃 처리.
     *
     * @param request  HttpServletRequest
     * @param response HttpServletResponse
     */
    public void logoutAfterPasswordChange(HttpServletRequest request,
                                          HttpServletResponse response){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(request,response,auth);
    }

    /**
     * 회원 탈퇴 처리 및 로그아웃.
     *
     * @param userId   사용자 ID
     * @param request  HttpServletRequest
     * @param response HttpServletResponse
     */
    public void deleteUser(Long userId,
                           HttpServletRequest request,
                           HttpServletResponse response){
        userService.deleteUserAndProfile(userId);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth instanceof OAuth2AuthenticationToken oat && clientService != null) {
            clientService.removeAuthorizedClient(
                    oat.getAuthorizedClientRegistrationId(), auth.getName());
        }

        new SecurityContextLogoutHandler().logout(request, response, auth);
        SecurityContextHolder.clearContext();
    }

    /**
     * principal 기반으로 User 조회.
     *
     * @param principal CustomUserPrincipal
     * @return User 엔티티
     * @throws EntityNotFoundException 존재하지 않는 경우
     */
    public User getUserByPrincipal(CustomUserPrincipal principal){
        return userService.findByUsername(principal.getUser().getUsername())
                .orElseThrow(() -> new EntityNotFoundException("해당 유저는 존재하지 않습니다."));
    }
}