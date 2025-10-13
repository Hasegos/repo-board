package io.github.repoboard.service;

import io.github.repoboard.model.DeleteUser;
import io.github.repoboard.model.User;
import io.github.repoboard.model.enums.UserStatus;
import io.github.repoboard.repository.DeleteUserRepository;
import io.github.repoboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 관리자 전용 사용자 관리 서비스.
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>전체 사용자 조회 및 상세 조회</li>
 *   <li>계정 등록, 정지/활성화 토글</li>
 *   <li>사용자 삭제(백업 포함) 및 복구</li>
 *   <li>관리자 로그 파일 읽기</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final DeleteUserService deleteUserService;
    private final DeleteUserRepository deleteUserRepository;

    /**
     * 전체 사용자 목록을 조회한다.
     *
     * @return 가입일 기준 내림차순 정렬된 사용자 목록
     */
    public List<User> getAllUsers(){
        return userService.findAllUsersOrderByCreatedAtDesc();
    }

    /**
     * 사용자 ID로 상세 정보를 조회한다.
     *
     * @param userId 조회할 사용자 ID
     * @return 해당 사용자 엔티티
     */
    public User getUserById(Long userId){
        return userService.findByUserId(userId);
    }

    /**
     * 사용자의 상태를 토글한다.
     * <p>ACTIVE → SUSPENDED, SUSPENDED → ACTIVE</p>
     *
     * @param userId 상태를 변경할 사용자 ID
     */
    public void toggleUserStatus(Long userId){
        User user = userService.findByUserId(userId);
        UserStatus before = user.getStatus();
        UserStatus after = (before == UserStatus.ACTIVE) ? UserStatus.SUSPENDED : UserStatus.ACTIVE;

        user.setStatus(after);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        String admin = SecurityContextHolder.getContext().getAuthentication().getName();
        log.warn("[ADMIN ACTION] {} 가 사용자 상태 토글 - id: {}, username: {}, {} -> {}, statusAt: {}",
                admin, user.getId(), user.getUsername(), before, after, Instant.now());
    }

    /**
     * 사용자를 백업 후 삭제한다.
     *
     * @param userId 삭제할 사용자 ID
     */
    public void deleteUser(Long userId){
        deleteUserService.backupAndDelete(userId);
    }

    /**
     * 삭제된 사용자(백업 사용자) 목록을 조회한다.
     *
     * @return {@link DeleteUser} 리스트
     */
    public List<DeleteUser> getDeletedUsers(){
        return deleteUserRepository.findAll();
    }

    /**
     * 삭제된 사용자를 복구한다.
     *
     * @param username 복구할 사용자명
     * @throws IllegalArgumentException 동일한 username이 이미 존재하거나, 백업 정보가 없는 경우
     */
    public void restoreDeletedUser(String username){
        DeleteUser deleted = deleteUserRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("삭제된 사용자 정보가 없습니다."));

        if(userRepository.existsByUsername(username)){
            throw new IllegalArgumentException("이미 동일한 username을 가진 사용자가 존재합니다.");
        }

        User restoredUser = deleteUserService.restoreUser(deleted);

        String admin = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("[ADMIN] {} → 사용자 복구: id={}, username={}, 복구시각={}",
                admin, restoredUser.getId(), restoredUser.getUsername(), Instant.now());
    }

    /**
     * 관리자 로그 파일을 읽어 페이징된 형태로 반환한다.
     *
     * @param page         페이지 번호 (0부터 시작)
     * @param linesPerPage 한 페이지당 읽을 로그 라인 수
     * @return 최신 순으로 정렬된 로그 라인 목록
     */
    public List<String> readAdminLogs(int page, int linesPerPage){
        Path path = Paths.get("logs/admin.log");
        if(!Files.exists(path)) return List.of();

        try(Stream<String> lines = Files.lines(path)){
           List<String> allLines = lines.collect(Collectors.toList());
           int fromIndex = Math.max(allLines.size() - (page + 1) * linesPerPage, 0);
           int toIndex = Math.min(allLines.size() - page * linesPerPage, allLines.size());
           List<String> result = allLines.subList(fromIndex,toIndex);
           Collections.reverse(result);
            return result;
        }catch (IOException e){
            return List.of("⚠ 로그 파일을 읽을 수 없습니다." + e.getMessage());
        }
    }
}