package io.github.repoboard.service;

import io.github.repoboard.dto.request.ChangePasswordDTO;
import io.github.repoboard.dto.auth.UserDTO;
import io.github.repoboard.model.DeleteUser;
import io.github.repoboard.model.User;
import io.github.repoboard.model.enums.UserProvider;
import io.github.repoboard.model.enums.UserRoleType;
import io.github.repoboard.model.enums.UserStatus;
import io.github.repoboard.repository.DeleteUserRepository;
import io.github.repoboard.repository.UserRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 계정 생성/조회/수정/삭제를 담당하는 도메인 애플리케이션 서비스.
 *
 * <p>주요 기능</p>
 * <ul>
 *   <li>{@link #register(UserDTO)}: 회원가입(로컬/소셜)</li>
 *   <li>{@link #changeUserPassword(Long, ChangePasswordDTO)} (Long, ChangePasswordDTO)}: 비밀번호 변경(로컬 계정)</li>
 * </ul>
 *
 * <p>트랜잭션</p>
 * <ul>
 *   <li>읽기 전용 조회 메서드는 {@code readOnly=true}를 사용</li>
 *   <li>쓰기 메서드는 기본 {@code @Transactional}로 묶음</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DeleteUserRepository deleteUserRepository;

    /**
     * 사용자명으로 사용자 엔티티를 조회한다.
     *
     * @param username 조회할 사용자명(고유)
     * @return 사용자가 존재하면 {@link Optional}에 래핑된 {@link User}, 없으면 빈 Optional
     */
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username){
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public User findByUserId(Long userId){
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 유저는 존재하지 않습니다."));
    }

    /**
     * 모든 사용자 목록을 가입일 내림차순으로 조회한다.
     *
     * @return 전체 사용자 리스트
     */
    @Transactional(readOnly = true)
    public List<User> findAllUsersOrderByCreatedAtDesc(){
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    /**
     * 회원 가입을 수행한다.
     *
     * <p>기본값</p>
     * <ul>
     *   <li>provider: {@link UserProvider#LOCAL}</li>
     *   <li>role: {@link UserRoleType#ROLE_USER}</li>
     * </ul>
     *
     * <p>소셜 로그인</p>
     * <ul>
     *   <li>{@code providerId}가 존재할 경우 DTO의 provider/providerId를 반영한다.</li>
     * </ul>
     *
     * @param userDTO 가입 정보(아이디/비밀번호/소셜 식별자 등)
     * @return 생성된 사용자 엔티티(영속화됨)
     */
    @Transactional
    public User register(UserDTO userDTO){

        validateReRegistration(userDTO.getUsername());

        if(userRepository.existsByUsername(userDTO.getUsername())){
            throw new EntityExistsException("이미 사용중인 아이디입니다.");
        }

        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setProvider(UserProvider.LOCAL);
        user.setRole(UserRoleType.ROLE_USER);

        /* 소셜 로그인 */
        if(userDTO.getProviderId() != null && !userDTO.getProviderId().isEmpty()){
            user.setProvider(userDTO.getProvider());
            user.setProviderId(userDTO.getProviderId());
        }
        return userRepository.save(user);
    }

    /**
     * 회원(LOCAL 계정)의 비밀번호를 변경한다.
     *
     * <p>검증 규칙</p>
     * <ul>
     *   <li>새 비밀번호와 확인 비밀번호가 일치해야 함</li>
     *   <li>LOCAL 계정인 경우 현재 비밀번호가 일치해야 함</li>
     *   <li>새 비밀번호는 기존 비밀번호와 달라야 함</li>
     * </ul>
     *
     * @param userId 대상 사용자 식별자
     * @param change 비밀번호 변경 정보(현재/새/확인)
     */
    @Transactional
    public void changeUserPassword(Long userId, ChangePasswordDTO change){

        if(!change.getNewPassword().equals(change.getConfirmPassword())){
            throw new IllegalArgumentException("새 비밀번호 확인이 일치하지않습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 사용자는 존재하지 않습니다."));

        if(user.getProvider() == UserProvider.LOCAL){

            if(!passwordEncoder.matches(change.getCurrentPassword(), user.getPassword())){
                throw new BadCredentialsException("현재 비밀번호가 일치하지않습니다.");
            }

            if(passwordEncoder.matches(change.getNewPassword(), user.getPassword())){
                throw new IllegalArgumentException("이전 비밀번호와 동일합니다.");
            }
        } else {
            throw new AccessDeniedException("소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다.");
        }
        user.setPassword(passwordEncoder.encode(change.getNewPassword()));
    }

    /**
     * 삭제된 사용자 백업 엔티티로부터 새로운 사용자 엔티티를 복원한다.
     *
     * @param d {@link DeleteUser} 백업 엔티티
     * @return 복구된 사용자 엔티티
     */
    public User createdUserFromBackup(DeleteUser d){
        User user = new User();
        user.setUsername(d.getUsername());
        user.setPassword(d.getPassword());
        user.setRole(d.getRole());
        user.setProvider(d.getProvider());
        user.setProviderId(d.getProviderId());
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    /**
     * 최근 7일 이내 삭제된 사용자일 경우 재가입을 차단한다.
     *
     * @param username 중복 확인할 사용자명
     * @throws IllegalStateException 삭제 후 7일이 지나지 않은 경우
     */
    private void validateReRegistration(String username){
        deleteUserRepository.findByUsername(username)
                .ifPresent(backup -> {
                    Instant sevenDayAgo = Instant.now().minus(Duration.ofDays(7));
                    if (backup.getDeleteAt().isAfter(sevenDayAgo)) {
                        throw new IllegalStateException("최근 탈퇴한 계정으로 7일 내 재가입이 불가합니다.");
                    }
                });
    }
}