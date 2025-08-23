package io.github.repoboard.service;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * <p>이미지 파일을 AWS S3에 업로드/조회/삭제하는 애플리케이션 서비스.</p>
 */
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final String bucketName;
    private final String region;

    /**
     * 애플리케이션 기동 시 S3 클라이언트를 초기화한다.
     *
     * @param accessKey AWS Access Key ID
     * @param secretKey AWS Secret Access Key
     * @param region    S3 리전(예: ap-northeast-2)
     * @param bucketName 업로드/삭제 대상 S3 버킷명
     */
    public S3Service(@Value("${aws.access-key-id}") String accessKey,
                     @Value("${aws.secret-access-key}") String secretKey,
                     @Value("${aws.region}") String region,
                     @Value("${aws.s3.bucket-name}") String bucketName){
        this.bucketName = bucketName;
        this.region = region;
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    /**
     * 멀티파트 이미지 파일을 AWS S3에 업로드한다.
     *
     * <p>허용된 MIME 타입(현재 JPEG, PNG)만 업로드할 수 있으며, <br>
     * 오브젝트 키는 trade-images/{UUID}-{원본파일명} 형태로 생성된다.</p>
     *
     * @param file 업로드할 이미지 파일
     * @return 업로드된 오브젝트의 퍼블릭 URL 포맷 문자열 <br>
     *         (주의: 버킷/오브젝트가 공개되어 있지 않으면 실제 접근 불가)
     */
    public String uploadFile(MultipartFile file) throws IOException{

        if(file == null || file.isEmpty()){
            throw new IllegalArgumentException("파일이 존재하지 않습니다.");
        }

        if(getFileType(file).contentEquals("")){
            throw new IllegalArgumentException("허용되지 않은 이미지 타입입니다.");
        }

        String fileName = generateFileName(file.getOriginalFilename());
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(file.getContentType())
                .build();

        /* 메모리 복사를 줄이기 위해 InputStream 기반 업로드 */
        try(InputStream is = file.getInputStream()){
            s3Client.putObject(request, RequestBody.fromInputStream(is, file.getSize()));
        }
        return getFileUrl(fileName);
    }

    /**
     * 주어진 오브젝트 키를 S3에서 삭제한다.
     * @param fileName S3 오브젝트 키
     */
    public void deleteFile(String fileName){
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        s3Client.deleteObject(request);
    }

    /**
     * 업로드된 오브젝트의 퍼블릭 접근 URL 문자열을 생성한다.
     *
     * <p>예: {@code https://{bucket}.s3.{region}.amazonaws.com/{key}}</p>
     *
     * @param fileName S3 오브젝트 키(예: {@code trade-images/uuid-filename.png})
     * @return 가상 호스트 스타일 퍼블릭 URL 포맷 문자열
     */
    public String getFileUrl(String fileName){
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                bucketName, region, fileName);
    }

    /**
     * 업로드 가능한 이미지 MIME 타입인지 검사하고 대표 확장자를 반환한다.
     *
     * @param file 검사 대상 파일
     * @return 허용되는 경우 ".jpeg" 또는 ".png", 허용되지 않으면 빈 문자열
     */
    private String getFileType(MultipartFile file){
        String contentType = file.getContentType();
        if(contentType != null){
            MediaType mediaType = MediaType.parseMediaType(contentType);
            switch (mediaType.toString()){
                case MediaType.IMAGE_JPEG_VALUE -> { return ".jpeg"; }
                case MediaType.IMAGE_PNG_VALUE -> { return ".png"; }
            }
        }
        return "";
    }

    /**
     * 충돌 방지를 위해 UUID를 포함한 오브젝트 키를 생성한다.
     *
     * <p>형태: {@code trade-images/{UUID}-{원본파일명}}</p>
     *
     * @param originalFileName 원본 파일명(null 가능)
     * @return 생성된 오브젝트 키
     */
    private String generateFileName(String originalFileName){
        String base = (originalFileName == null || originalFileName.isBlank())
                ? "unknown"
                : originalFileName.replaceAll("[^a-zA-Z0-9._-]", "-");
        return "trade-images/" + UUID.randomUUID() + "-" + originalFileName;
    }
}