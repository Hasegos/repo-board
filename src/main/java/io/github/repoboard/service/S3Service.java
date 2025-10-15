package io.github.repoboard.service;

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
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Set;
import java.util.UUID;

/**
 * <p>이미지 파일을 AWS S3에 업로드/조회/삭제하는 애플리케이션 서비스.</p>
 */
@Service
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

        String fileName = generateUniqueKeyWithExtension(file.getContentType(), "trade-images");
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
     * 이미지 URL로부터 데이터를 스트리밍하여 S3에 직접 업로드합니다.
     *
     * @param imageUrl 업로드할 이미지의 전체 URL
     * @return 업로드된 오브젝트의 퍼블릭 URL
     * @throws IOException 네트워크 오류 또는 파일 처리 오류 발생 시
     */
    public String uploadFromUrl(String imageUrl) throws IOException {
        if(imageUrl == null || imageUrl.trim().isEmpty()){
            throw new IllegalArgumentException("이미지 URL이 존재하지 않습니다.");
        }

        validateAllowedHost(imageUrl);

        URL url = new URL(imageUrl);
        URLConnection connection = url.openConnection();

        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        String contentType = connection.getContentType();

        if(!isValidImageType(contentType)){
            throw new IllegalArgumentException("허용되지 않은 이미지 타입입니다. : " + contentType);
        }

        String fileName = generateUniqueKeyWithExtension(contentType, "trade-images");
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(contentType)
                .build();

        try (InputStream is = connection.getInputStream()){
            s3Client.putObject(request, RequestBody.fromInputStream(is, connection.getContentLengthLong()));
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
     * Content-Type이 허용된 이미지 타입인지 검사한다.
     *
     * @param contentType HTTP Content-Type 헤더값
     * @return 허용되는 이미지 타입이면 true
     */
    private boolean isValidImageType(String contentType) {
        if (contentType == null) return false;

        return contentType.equals(MediaType.IMAGE_JPEG_VALUE) ||
                contentType.equals(MediaType.IMAGE_PNG_VALUE) ||
                contentType.equals("image/jpg");
    }

    /**
     * 업로드 가능한 이미지 MIME 타입인지 검사하고 대표 확장자를 반환한다.
     *
     * @param contentType  MIME 타입
     * @return 허용되는 경우 ".jpeg" 또는 ".png", 허용되지 않으면 빈 문자열
     */
    private String getExtensionFromContentType(String contentType){
        if(contentType == null) return "";
        if(contentType.equalsIgnoreCase(MediaType.IMAGE_JPEG_VALUE) ||
            contentType.equalsIgnoreCase("image/jpg")){
            return ".jpg";
        }
        if(contentType.equalsIgnoreCase(MediaType.IMAGE_PNG_VALUE)){
            return ".png";
        }
        return "";
    }

    /**
     * Content-Type을 기반으로 고유한 S3 파일 키(경로 포함)를 생성합니다.
     * @param contentType MIME 타입
     * @param prefix S3 내에서 사용할 폴더명 (예: "trade-images")
     * @return 생성된 전체 S3 키 (예: "trade-images/uuid-string.jpg")
     */
    private String generateUniqueKeyWithExtension(String contentType, String prefix){
        String extension = getExtensionFromContentType(contentType);

        if(extension.isEmpty()){
            extension =".jpg";
        }
        return prefix + "/" + UUID.randomUUID() + extension;
    }

    /**
     * 이미지 URL이 안전한 외부 호스트(GitHub)에서 온 것인지 검증한다.
     *
     * <p>SSRF(Server Side Request Forgery) 공격을 방지하기 위해 다음을 검사한다:</p>
     * <ul>
     *   <li>URL 형식이 올바른지</li>
     *   <li>HTTPS 프로토콜만 허용</li>
     *   <li>허용된 호스트(avatars.githubusercontent.com, raw.githubusercontent.com)만 접근 가능</li>
     *   <li>내부망(IP 127.x.x.x, 169.254.x.x 등) 또는 로컬 네트워크로의 접근 차단</li>
     * </ul>
     *
     * @param imageUrl 검증할 이미지 URL
     * @throws IllegalArgumentException URL이 비정상이거나, 내부망 접근 또는 비허용 호스트일 경우
     */
    private void validateAllowedHost(String imageUrl){
        if(imageUrl == null || imageUrl.trim().isEmpty()){
            throw new IllegalArgumentException("URL이 비어있습니다.");
        }

        URI uri;
        try{
            uri = URI.create(imageUrl);
        } catch (Exception e){
            throw new IllegalArgumentException("잘못된 이미지 URL 형식입니다.");
        }

        String scheme = uri.getScheme();
        if(scheme == null || !scheme.equalsIgnoreCase("https")){
            throw new IllegalArgumentException("허용되지 않은 프로토콜입니다.");
        }

        String host =  uri.getHost();
        if (host == null){
            throw new IllegalArgumentException("host 정보가없습니다.");
        }

        Set<String> allowedHosts = Set.of(
                "avatars.githubusercontent.com",
                "raw.githubusercontent.com"
        );

        if(!allowedHosts.contains(host)){
            throw new IllegalArgumentException("허용되지 않은 허스트 접근입니다.");
        }

        try{
            InetAddress address = InetAddress.getByName(host);
            if(isPrivateOrLocalAddress(address)){
                throw new IllegalArgumentException("내부 네트워크 전역 차단됨");
            }
        }catch (Exception e){
            throw new IllegalArgumentException("호스트 해석 실패");
        }

    }

    /**
     * 사설망, 루프백, 링크로컬 등 내부 IP 주소 여부를 검사한다.
     *
     * @param address 검사할 IP 주소
     * @return 내부 또는 로컬 주소이면 true
     */
    private boolean isPrivateOrLocalAddress(InetAddress address){
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.getHostAddress().startsWith("169.254.")
                || address.getHostAddress().startsWith("127.");
    }
}