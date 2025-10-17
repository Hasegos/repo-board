package io.github.repoboard.service;

import io.github.repoboard.dto.request.SavedRepoDTO;
import io.github.repoboard.model.RepoOwner;
import io.github.repoboard.model.SavedRepo;
import io.github.repoboard.model.User;
import io.github.repoboard.repository.SavedRepoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SavedRepo DB 쓰기 전용 서비스.
 * <p>엔티티 생성/수정/삭제를 트랜잭션 경계 안에서 처리한다.</p>
 */
@Service
@RequiredArgsConstructor
public class SavedRepoDBService {

    private final SavedRepoRepository savedRepoRepository;

    /**
     * SavedRepo 저장.
     *
     * @param dto  저장할 데이터 DTO
     * @param user 소유 사용자
     */
    @Transactional
    public void save(SavedRepoDTO dto, User user){
        SavedRepo savedRepo = new SavedRepo();
        savedRepo.setUser(user);
        savedRepo.setRepoGithubId(dto.getRepoGithubId());
        savedRepo.setName(dto.getName());
        savedRepo.setHtmlUrl(dto.getHtmlUrl());
        savedRepo.setDescription(dto.getDescription());
        savedRepo.setLanguageMain(dto.getLanguage());
        savedRepo.setStars(dto.getStars());
        savedRepo.setForks(dto.getForks());

        RepoOwner owner = new RepoOwner();
        owner.setOwnerLogin(dto.getOwnerLogin());
        owner.setOwnerAvatarUrl(dto.getOwnerAvatarUrl());
        owner.setOwnerHtmlUrl(dto.getOwnerHtmlUrl());
        savedRepo.setOwner(owner);

        savedRepoRepository.save(savedRepo);
    }

    /**
     * 노트 업데이트.
     *
     * @param savedGithubId 저장 레포의 GitHub ID
     * @param userId        사용자 ID
     * @param note          메모
     */
    @Transactional
    public void updateNote(Long savedGithubId, Long userId, String note){
        SavedRepo savedRepo = savedRepoRepository.findByRepoGithubIdAndUserId(savedGithubId, userId)
                .orElseThrow(() -> new EntityNotFoundException("저장된 레포를 찾을 수 없습니다."));
        savedRepo.setNote(note);

        savedRepoRepository.save(savedRepo);
    }

    /**
     * 핀 여부 업데이트.
     *
     * @param savedGithubId 저장 레포의 GitHub ID
     * @param userId        사용자 ID
     * @param isPinned      핀 여부
     */
    @Transactional
    public void updatePin(Long savedGithubId, Long userId, boolean isPinned){
        SavedRepo savedRepo = savedRepoRepository.findByRepoGithubIdAndUserId(savedGithubId, userId)
                .orElseThrow(() -> new EntityNotFoundException("저장된 레포를 찾을 수 없습니다."));
        savedRepo.setPinned(isPinned);

        savedRepoRepository.save(savedRepo);
    }

    /**
     * 저장 레포 삭제.
     *
     * @param savedGithubId 저장 레포의 GitHub ID
     * @param userId        사용자 ID
     */
    @Transactional
    public void delete(Long savedGithubId, Long userId){
        SavedRepo savedRepo = savedRepoRepository.findByRepoGithubIdAndUserId(savedGithubId, userId)
                .orElseThrow(() -> new EntityNotFoundException("저장된 레포를 찾을 수 없습니다."));

       savedRepoRepository.delete(savedRepo);
    }

    /**
     * 특정 사용자에 대해 동일 GitHub ID가 이미 저장되었는지 검사.
     */
    public boolean existsByGithubIdAndUserId(Long repoGithubId, Long userId){
        return savedRepoRepository.existsByRepoGithubIdAndUserId(repoGithubId, userId);
    }
}