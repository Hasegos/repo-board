package io.github.repoboard.service;

import io.github.repoboard.dto.ProfileDTO;
import io.github.repoboard.model.Profile;
import io.github.repoboard.model.User;
import io.github.repoboard.repository.ProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ProfileDBService {

    private final ProfileRepository profileRepository;

    @Transactional
    public Profile createProfileDB(User user,
                                   ProfileDTO profileDTO,
                                   String imageUrl,
                                   String s3Key){

        validateNickname(profileDTO.getNickname(),user.getId());

        Profile profile = new Profile();
        profile.setUser(user);
        profile.setNickname(profileDTO.getNickname());
        profile.setSelfInfo(profileDTO.getSelfInfo());
        profile.setExperience(profileDTO.getExperience());
        profile.setRepositoryUrl(profileDTO.getRepositoryUrl());
        profile.setStacks(profileDTO.getStacks());
        profile.setCreatedAt(Instant.now());
        profile.setImageUrl(imageUrl);
        profile.setS3Key(s3Key);

        return profileRepository.save(profile);
    }

    @Transactional
    public Profile updateProfileDB(Long userId, ProfileDTO profileDTO){

        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("프로필이 존재하지 않습니다."));

        if(!profile.getNickname().equals(profileDTO.getNickname())){
            validateNickname(profileDTO.getNickname(), userId);
        }

        profile.setNickname(profileDTO.getNickname());
        profile.setSelfInfo(profileDTO.getSelfInfo());
        profile.setRepositoryUrl(profileDTO.getRepositoryUrl());
        profile.setExperience(profileDTO.getExperience());
        profile.setStacks(profileDTO.getStacks());
        profile.setUpdatedAt(Instant.now());

        return profileRepository.save(profile);
    }

    @Transactional
    public void updateProfileImageDB(Profile profile, String imageUrl, String s3Key){

        profile.setImageUrl(imageUrl);
        profile.setS3Key(s3Key);
        profile.setUpdatedAt(Instant.now());

        profileRepository.save(profile);
    }

    @Transactional
    public void deleteProfileDB(Long profileId){
        profileRepository.deleteById(profileId);
    }

    private void validateNickname(String nickname, Long userId){

        if(nickname == null || nickname.trim().length() < 2){
            throw new IllegalArgumentException("닉네임은 두 글자 이상입니다.");
        }
        boolean exists;
        if(userId == null){
            exists = profileRepository.existsByNickname(nickname);
        }else{
            exists = profileRepository.existsByNicknameAndUserIdNot(nickname,userId);
        }
        if(exists){
            throw new IllegalArgumentException("이미 사용중인 닉네임입니다.");
        }
    }
}