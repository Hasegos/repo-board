package io.github.repoboard.service;

import io.github.repoboard.dto.request.ChangePasswordDTO;
import io.github.repoboard.dto.auth.UserDTO;
import io.github.repoboard.model.Profile;
import io.github.repoboard.model.User;
import io.github.repoboard.model.enums.UserProvider;
import io.github.repoboard.model.enums.UserRoleType;
import io.github.repoboard.repository.UserRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.framework.AopContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * 사용자 계정 생성/조회/수정/삭제를 담당하는 도메인 애플리케이션 서비스.
 *
 * <p>주요 기능</p>
 * <ul>
 *   <li>{@link #register(UserDTO)}: 회원가입(로컬/소셜)</li>
 *   <li>{@link #changeUserPassword(Long, ChangePasswordDTO)} (Long, ChangePasswordDTO)}: 비밀번호 변경(로컬 계정)</li>
 *   <li>{@link #deleteUserAndProfile(Long)}: 회원 및 프로필 삭제(연계된 S3 이미지 삭제 포함)</li>
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
    private final S3Service s3Service;

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
     * 회원 가입을 수행한다.
     *
     * <p>기본값</p>
     * <ul>
     *   <li>provider: {@link UserProvider#LOCAL}</li>
     *   <li>role: {@link UserRoleType#USER}</li>
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

        if(userRepository.existsByUsername(userDTO.getUsername())){
            throw new EntityExistsException("이미 사용중인 아이디입니다.");
        }

        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setProvider(UserProvider.LOCAL);
        user.setRole(UserRoleType.USER);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

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

        user.setUpdatedAt(Instant.now());
        user.setPassword(passwordEncoder.encode(change.getNewPassword()));
    }

    /**
     * 회원 탈퇴(트랜잭션 외부) 메서드.
     *
     * <p>동작 순서</p>
     * <ol>
     *   <li>{@link #deleteUserInTx(Long)} 를 <b>프록시</b>를 통해 호출하여
     *       DB에서 사용자/프로필을 삭제(트랜잭션 커밋)</li>
     *   <li>반환된 S3 오브젝트 키가 있으면 <b>커밋 이후</b> S3에서 파일 삭제</li>
     * </ol>
     *
     * <p>트랜잭션 전제</p>
     * <ul>
     *   <li>본 메서드는 {@code Propagation.NEVER}로 선언되어 있어,
     *       항상 트랜잭션 <b>밖</b>에서 실행되어 DB 커밋 → S3 삭제 순서를 보장한다.</li>
     *   <li>{@code AopContext.currentProxy()} 사용을 위해
     *       {@code expose-proxy=true} 설정이 필요하다.
     * </ul>
     *
     * @param userId 탈퇴할 사용자 식별자
     */
    @Transactional(propagation = Propagation.NEVER)
    public void deleteUserAndProfile(Long userId){

        UserService self = (UserService) AopContext.currentProxy();
        String s3Key = self.deleteUserInTx(userId);

        if(s3Key != null && !s3Key.isEmpty()){
            s3Service.deleteFile(s3Key);
        }
    }

    /**
     * (트랜잭션) 사용자 및 프로필을 삭제하고, 프로필에 연결된 S3 오브젝트 키를 반환한다.
     *
     * @param userId 삭제 대상 사용자 식별자
     * @return 프로필 이미지의 S3 오브젝트 키(없으면 null)
     */
    @Transactional
    public String deleteUserInTx(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 사용자는 존재하지 않습니다."));

        Profile profile = user.getProfile();
        String s3Key = profile != null ? profile.getS3Key() : null;

        userRepository.delete(user);

        return s3Key;
    }
}