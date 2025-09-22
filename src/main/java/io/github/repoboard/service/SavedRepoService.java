package io.github.repoboard.service;

import io.github.repoboard.common.exception.SavedRepoNotFoundException;
import io.github.repoboard.dto.github.GithubRepoDTO;
import io.github.repoboard.dto.request.SavedRepoDTO;
import io.github.repoboard.model.RepoOwner;
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

@Service
@RequiredArgsConstructor
public class SavedRepoService {

    private final SavedRepoRepository savedRepoRepository;
    private final GitHubApiService gitHubApiService;

    private Pageable applySort(Pageable pageable, String sort){
        Sort sortOption = switch (sort){
            case "recent" -> Sort.by(Sort.Direction.DESC, "updatedAt");
            case "popular" -> Sort.by(Sort.Direction.DESC, "stars");
            default -> Sort.by(Sort.Direction.DESC, "id");
        };

        if(pageable.isUnpaged()){
            return Pageable.unpaged(sortOption);
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sortOption);
    }

    /**
     * 고정된 레포지토리 찾기
     * @param userId
     * @param language
     * @param sort
     * @return
     */
    public Page<SavedRepo> findPinnedRepos(Long userId, String language, String sort,Pageable pageable){
        Pageable finalPageable = applySort(pageable, sort);

        if(language == null || language.isBlank()){
            return savedRepoRepository.findAllByUserIdAndIsPinnedTrue(userId, finalPageable);
        }
        return savedRepoRepository.findAllByUserIdAndIsPinnedTrueAndLanguageMainIgnoreCase(userId, language, finalPageable);
    }

    /**
     * 고정되지 않은 레포지토리
     * @param userId
     * @param language
     * @param sort
     * @param pageable
     * @return
     */
    public Page<SavedRepo> findUnpinnedRepos(Long userId, String language, String sort, Pageable pageable){
        Pageable finalPageable = applySort(pageable, sort);

       if(language == null || language.isBlank()){
           return savedRepoRepository.findAllByUserIdAndIsPinnedFalse(userId, finalPageable);
       }
       return savedRepoRepository.findAllByUserIdAndIsPinnedFalseAndLanguageMainIgnoreCase(userId, language, finalPageable);
    }


    public SavedRepo savedRepoById(Long repoGithubId, User user){
        boolean existing = savedRepoRepository.existsByRepoGithubIdAndUserId(repoGithubId, user.getId());
        if(existing){
            throw new IllegalArgumentException("이미 저장한 레포지토리 입니다.");
        }

        GithubRepoDTO dto = gitHubApiService.getRepositoryId(repoGithubId);
        if(dto == null){
            throw new IllegalArgumentException("존재하지 않는 Github Repo 입니다.");
        }
        SavedRepoDTO savedRepoDTO = convertToSavedRepoDTO(dto);
        return savedReposDB(savedRepoDTO, user);
    }

    @Transactional
    public SavedRepo savedReposDB(SavedRepoDTO savedRepoDTO, User user){
        SavedRepo savedRepo = new SavedRepo();
        savedRepo.setUser(user);
        savedRepo.setRepoGithubId(savedRepoDTO.getRepoGithubId());
        savedRepo.setName(savedRepoDTO.getName());
        savedRepo.setHtmlUrl(savedRepoDTO.getHtmlUrl());
        savedRepo.setDescription(savedRepoDTO.getDescription());
        savedRepo.setLanguageMain(savedRepoDTO.getLanguage());
        savedRepo.setStars(savedRepoDTO.getStars());
        savedRepo.setForks(savedRepoDTO.getForks());
        savedRepo.setUpdatedAt(savedRepoDTO.getUpdatedAt());

        savedRepo.setOwner(new RepoOwner());
        savedRepo.getOwner().setOwnerLogin(savedRepoDTO.getOwnerLogin());
        savedRepo.getOwner().setOwnerAvatarUrl(savedRepoDTO.getOwnerAvatarUrl());
        savedRepo.getOwner().setOwnerHtmlUrl(savedRepoDTO.getOwnerHtmlUrl());

        return savedRepoRepository.save(savedRepo);
    }

    @Transactional
    public SavedRepo updateSavedRepoNote(Long savedGithubId, Long userId, String note){
        SavedRepo savedRepo = savedRepoRepository.findByRepoGithubIdAndUserId(savedGithubId, userId)
                .orElseThrow(SavedRepoNotFoundException :: new);
        savedRepo.setNote(note);
        return savedRepoRepository.save(savedRepo);
    }

    @Transactional
    public SavedRepo updatedSavedRepoPin(Long savedGithubId, Long userId, boolean isPinned){
        SavedRepo savedRepo = savedRepoRepository.findByRepoGithubIdAndUserId(savedGithubId, userId)
                .orElseThrow(SavedRepoNotFoundException :: new);
        savedRepo.setPinned(isPinned);
        return savedRepoRepository.save(savedRepo);
    }

    @Transactional
    public void deleteSavedRepoDB(Long savedGithubId, Long userId){
        SavedRepo savedRepo = savedRepoRepository.findByRepoGithubIdAndUserId(savedGithubId, userId)
                .orElseThrow(SavedRepoNotFoundException::new);

        savedRepoRepository.deleteByRepoGithubIdAndUserId(savedRepo.getRepoGithubId(), userId);
    }

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