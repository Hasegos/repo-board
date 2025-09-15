package io.github.repoboard.service;

import io.github.repoboard.dto.github.GithubRepoDTO;
import io.github.repoboard.dto.request.SavedRepoDTO;
import io.github.repoboard.model.RepoOwner;
import io.github.repoboard.model.SavedRepo;
import io.github.repoboard.model.User;
import io.github.repoboard.repository.SavedRepoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SavedRepoService {

    private final SavedRepoRepository savedRepoRepository;
    private final GitHubApiService gitHubApiService;

    @Transactional(readOnly = true)
    public List<SavedRepo> getSavedReposByUserId(Long userId){
        return savedRepoRepository.findAllByUserId(userId);
    }

    public SavedRepo savedRepoById(Long repoGithubId, User user){

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
    public SavedRepo updateSavedRepoDB(Integer savedGithubId, Long userId, String note, boolean isPinned){

        SavedRepo savedRepo = savedRepoRepository.findByRepoGithubIdAndUserId(savedGithubId, userId)
                .orElseThrow(() -> new IllegalArgumentException("저장한 레포지토리가 존재하지 않습니다."));

        savedRepo.setNote(note);
        savedRepo.setPinned(isPinned);

        return savedRepoRepository.save(savedRepo);
    }

    @Transactional
    public void deleteSavedRepoDB(Integer savedGithubId, Long userId){
        savedRepoRepository.findByRepoGithubIdAndUserId(savedGithubId, userId)
                .ifPresent(savedRepoRepository::delete);
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