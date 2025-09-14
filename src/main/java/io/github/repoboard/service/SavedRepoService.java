package io.github.repoboard.service;

import io.github.repoboard.dto.SavedRepoDTO;
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

    @Transactional(readOnly = true)
    public List<SavedRepo> getSavedReposByUserId(Long userId){
        return savedRepoRepository.findAllByUserId(userId);
    }

    @Transactional
    public SavedRepo savedReposDB(SavedRepoDTO dto, User user){

        SavedRepo savedRepo = new SavedRepo();
        savedRepo.setUser(user);
        savedRepo.setRepoGithubId(dto.getRepoGithubId());
        savedRepo.setName(dto.getName());
        savedRepo.setHtmlUrl(dto.getHtmlUrl());
        savedRepo.setDescription(dto.getDescription());
        savedRepo.setLanguageMain(dto.getLanguageMain());
        savedRepo.setStars(dto.getStars());
        savedRepo.setNote(dto.getNote());
        savedRepo.setForks(dto.getForks());
        savedRepo.setReadmeExcerpt(dto.getReadmeExcerpt());
        savedRepo.setUpdatedAt(dto.getUpdatedAt());

        savedRepo.setOwner(new RepoOwner());
        savedRepo.getOwner().setOwnerLogin(dto.getOwnerLogin());
        savedRepo.getOwner().setOwnerAvatarUrl(dto.getOwnerAvatarUrl());
        savedRepo.getOwner().setOwnerHtmlUrl(dto.getOwnerHtmlUrl());

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
}