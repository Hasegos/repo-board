package io.github.repoboard.service;

import io.github.repoboard.dto.ProfileDTO;
import io.github.repoboard.model.Profile;
import io.github.repoboard.model.User;
import io.github.repoboard.repository.ProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final UserService userService;
    private final ProfileRepository profileRepository;
    private final S3Service s3Service;
    private final ProfileDBService profileDBService;

    @Transactional(readOnly = true)
    public void ensureProfileNotExists(Long userId){
        if(profileRepository.existsByUserId(userId)){
            throw new IllegalArgumentException("이미 프로필이 존재합니다.");
        }
    }

    @Transactional(readOnly = true)
    public Profile findProfileByUserId(Long userId){
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("프로필이 존재하지 않습니다."));
    }

    @Transactional(propagation = Propagation.NEVER)
    public Profile registerProfile(Long userId, ProfileDTO dto, MultipartFile file) throws IOException{

        User user = userService.findByUserId(userId);
        ensureProfileNotExists(user.getId());

        String imageUrl = null;
        String s3Key = null;
        if(file != null && !file.isEmpty()){
            try{
                imageUrl = s3Service.uploadFile(file);
                s3Key = extractS3KeyFromUrl(imageUrl);
            } catch (IOException e){
                throw new IOException("프로필 이미지 업로드 실패 : " + e);
            }
        }

        try{
            return profileDBService.createProfileDB(user, dto, imageUrl, s3Key);
        }catch (Exception e){
            if(s3Key != null && !s3Key.isEmpty()) {
                try{
                    s3Service.deleteFile(s3Key);
                }catch (Exception ex){
                    log.error("롤백 중 S3 파일 삭제 실패 : key = " + s3Key, ex);
                }
            }
            throw e;
        }
    }

    @Transactional(propagation =  Propagation.NEVER)
    public void updateProfileImage(Long userId, MultipartFile file) throws IOException {

        if(file == null || file.isEmpty()) { return; }

        Profile profile = findProfileByUserId(userId);


        String oldKey = profile.getS3Key();
        String newImageUrl = null;
        String newS3Key = null;

        try{
            newImageUrl = s3Service.uploadFile(file);
            newS3Key = extractS3KeyFromUrl(newImageUrl);
        } catch (Exception e){
            throw new IOException("프로필 이미지 업로드 실패 : " + e);
        }

        try{
            profileDBService.updateProfileImageDB(profile, newImageUrl, newS3Key);
        }catch (Exception e){
            try{
                s3Service.deleteFile(newS3Key);
            } catch (Exception ex){
                log.error("롤백 중 S3 파일 삭제 실패 : key = " + newS3Key, ex);
            }
            throw e;
        }

        if(oldKey != null && !oldKey.isEmpty()) {
            try{
                s3Service.deleteFile(oldKey);
            } catch (Exception e){
                log.error("롤백 중 S3 파일 삭제 실패 : key = " + oldKey, e);
            }
        }
    }

    @Transactional(propagation = Propagation.NEVER)
    public void deleteProfileByUserId(Long userId){

        Profile profile = findProfileByUserId(userId);

        String s3Key = profile.getS3Key();
        profileDBService.deleteProfileDB(profile.getId());

        if(s3Key != null && !s3Key.isEmpty()){
            try {
                s3Service.deleteFile(s3Key);
            }catch (Exception ex){
                log.error("롤백 중 S3 파일 삭제 실패 : key = " + s3Key, ex);
            }
        }
    }

    private String extractS3KeyFromUrl(String imageUrl){

        if(imageUrl == null || !imageUrl.contains("/")){
            throw new IllegalArgumentException("올바른 이미지 URL이 아닙니다.");
        }
        try{
            var uri = java.net.URI.create(imageUrl);
            String path = uri.getPath();
            return path.startsWith("/") ? path.substring(1) : path;
        } catch (Exception e){
            throw new IllegalArgumentException("올바른 이미지 URL이 아닙니다.");
        }
    }
}