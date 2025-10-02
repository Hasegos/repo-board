package io.github.repoboard.service;

import io.github.repoboard.dto.github.GithubRepoDTO;
import io.github.repoboard.dto.request.SavedRepoDTO;
import io.github.repoboard.dto.view.SavedRepoView;import io.github.repoboard.model.RepoOwner;
import io.github.repoboard.model.SavedRepo;
import io.github.repoboard.model.User;
import io.github.repoboard.repository.SavedRepoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * SavedRepo 도메인 서비스.
 * <p>저장소 조회, 검증, 정렬, DTO 변환, View 조립을 담당한다.</p>
 */
@Service
@RequiredArgsConstructor
public class SavedRepoService {

    private final SavedRepoRepository savedRepoRepository;
    private final SavedRepoDBService savedRepoDBService;
    private final GitHubApiService gitHubApiService;
    private final UserService userService;

    /**
     * 정렬 옵션을 적용한 Pageable 생성.
     *
     * @param pageable 원본 페이지 정보
     * @param sort     정렬 기준 ("recent", "popular", 그 외 기본 id 내림차순)
     * @return 정렬이 적용된 Pageable
     */
    private Pageable applySort(Pageable pageable, String sort){
        Sort sortOption = switch (sort){
            case "recent" -> Sort.by(Sort.Direction.DESC, "updatedAt");
            case "popular" -> Sort.by(Sort.Direction.DESC, "stars");
            default -> Sort.by(Sort.Direction.DESC, "id");
        };

        if(pageable.isUnpaged()){
            return PageRequest.of(0,20,sortOption);
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sortOption);
    }

    /**
     * 핀된 레포지토리 조회.
     *
     * @param userId   사용자 ID
     * @param language 언어 필터 (null/blank 시 전체)
     * @param sort     정렬 기준
     * @param pageable 페이지 정보
     * @return 페이지 단위 결과
     */
    @Transactional(readOnly = true)
    public Page<SavedRepo> findPinnedRepos(Long userId, String language, String sort,Pageable pageable){
        Pageable finalPageable = applySort(pageable, sort);

        if(language == null || language.isBlank()){
            return savedRepoRepository.findAllByUserIdAndIsPinnedTrue(userId, finalPageable);
        }
        return savedRepoRepository.findAllByUserIdAndIsPinnedTrueAndLanguageMainIgnoreCase(userId, language, finalPageable);
    }

    /**
     * 핀되지 않은 레포지토리 조회.
     *
     * @param userId   사용자 ID
     * @param language 언어 필터
     * @param sort     정렬 기준
     * @param pageable 페이지 정보
     * @return 페이지 단위 결과
     */
    @Transactional(readOnly = true)
    public Page<SavedRepo> findUnpinnedRepos(Long userId, String language, String sort, Pageable pageable){
        Pageable finalPageable = applySort(pageable, sort);

       if(language == null || language.isBlank()){
           return savedRepoRepository.findAllByUserIdAndIsPinnedFalse(userId, finalPageable);
       }
       return savedRepoRepository.findAllByUserIdAndIsPinnedFalseAndLanguageMainIgnoreCase(userId, language, finalPageable);
    }

    /**
     * 전체 저장 레포지토리 조회(핀 여부 무관).
     *
     * @param userId   사용자 ID
     * @param language 언어 필터
     * @param sort     정렬 기준
     * @param pageable 페이지 정보
     * @return 페이지 단위 결과
     */
    @Transactional(readOnly = true)
    public Page<SavedRepo> findAllSavedRepos(Long userId, String language, String sort, Pageable pageable){
        Pageable finalPageable = applySort(pageable, sort);

        if(language == null || language.isBlank()){
            return savedRepoRepository.findAllByUserId(userId, finalPageable);
        }

        return savedRepoRepository.findAllByUserIdAndLanguageMainIgnoreCase(userId, language, finalPageable);
    }

    /**
     * 저장소 뷰 조립 (핀/비핀 목록 + 언어 옵션).
     *
     * @param userId       사용자 ID
     * @param language     언어 필터
     * @param sort         정렬 기준
     * @param pinnedPage   핀된 레포 페이지 번호
     * @param unpinnedPage 핀되지 않은 레포 페이지 번호
     * @return SavedRepoView DTO
     */
    public SavedRepoView loadSavedRepos(Long userId, String language, String sort, int pinnedPage, int unpinnedPage){
        User user = userService.findByUserId(userId);

        Page<SavedRepo> pinnedRepos =
                findPinnedRepos(user.getId(), language, sort, PageRequest.of(pinnedPage,4));

        Page<SavedRepo> unpinnedRepos =
                findUnpinnedRepos(user.getId(), language, sort, PageRequest.of(unpinnedPage,8));

        Set<String> languageOptions = user.getSavedRepos().stream()
                .map(SavedRepo::getLanguageMain)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));

        return new SavedRepoView(user, pinnedRepos, unpinnedRepos, languageOptions);
    }

    /**
     * GitHub repoId로 저장 처리.
     *
     * @param repoGithubId GitHub 레포지토리 ID
     * @param userId       사용자 ID
     * @throws IllegalArgumentException 중복 저장 또는 원격 조회 실패 시
     */
    public void savedRepoById(Long repoGithubId, Long userId){
        User user = userService.findByUserId(userId);

        boolean existing = savedRepoDBService.existsByGithubIdAndUserId(repoGithubId, userId);
        if(existing){
            throw new IllegalArgumentException("이미 저장한 레포지토리 입니다.");
        }

        GithubRepoDTO dto = gitHubApiService.getRepositoryId(repoGithubId);
        if(dto == null){
            throw new IllegalArgumentException("존재하지 않는 Github Repo 입니다.");
        }
        SavedRepoDTO savedRepoDTO = convertToSavedRepoDTO(dto);
        savedRepoDBService.save(savedRepoDTO, user);
    }

    /** GithubRepoDTO → SavedRepoDTO 변환 */
    private SavedRepoDTO convertToSavedRepoDTO(GithubRepoDTO dto){
        return SavedRepoDTO.builder()
                .repoGithubId(dto.getId())
                .name(dto.getName())
                .htmlUrl(dto.getHtmlUrl())
                .description(dto.getDescription())
                .language(dto.getLanguage())
                .stars(dto.getStargazersCount())
                .forks(dto.getForksCount())
                .updatedAt(dto.getUpdatedAt())
                .ownerLogin(dto.getOwner().getLogin())
                .ownerAvatarUrl(dto.getOwner().getAvatarUrl())
                .ownerHtmlUrl(dto.getOwner().getHtmlUrl())

                .build();
    }
}