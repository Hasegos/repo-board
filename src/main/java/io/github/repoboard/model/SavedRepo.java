package io.github.repoboard.model;

import io.github.repoboard.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * 사용자가 저장한 GitHub 레포지토리 정보를 나타내는 엔티티.
 *
 * <p>사용자는 동일한 소유자(owner)와 이름(name)을 가진 레포지토리를 중복 저장할 수 없다.</p>
 *
 * <p>레포지토리 정보에는 기본 메타데이터(언어, 스타, 포크 수 등)와 개인 메모, 핀 여부가 포함된다.</p>
 *
 * <h3>제약 조건</h3>
 * <ul>
 *     <li>고유 제약: 사용자 + 소유자 + 이름 조합은 유일해야 함</li>
 *     <li>인덱스: 사용자 ID, (소유자 + 이름) 복합 인덱스</li>
 * </ul>
 */
@Entity
@Table(
    name = "saved_repo",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "ux_saved_repo_user_repo",
            columnNames = {"user_id", "repo_github_id"}
        )
    },
    indexes = {
        @Index(name = "idx_saved_repo_user", columnList = "user_id"),
        @Index(name = "idx_saved_repo_repo_id", columnList = "repo_github_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SavedRepo extends BaseTimeEntity {

    /** 고유 식별자 (PK) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 저장한 사용자 (FK) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** GitHub 레포지토리 고유 ID */
    @Column(name = "repo_github_id", nullable = false)
    private Long repoGithubId;

    /** 레포지토리 이름 */
    @Column(name = "name", nullable = false)
    private String name;

    /** GitHub HTML URL */
    @Column(name = "html_url", nullable = false, columnDefinition = "TEXT")
    private String htmlUrl;

    /** 레포지토리 설명 */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 주 언어 */
    @Column(name = "language_main", nullable = false)
    private String languageMain;

    /** 스타 개수 */
    @Column(name = "stars")
    private Integer stars;

    /** 사용자 메모 */
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    /** 포크 수 */
    @Column(name = "forks")
    private Integer forks;

    /** 레포지토리 소유자 정보 (임베디드) */
    @Embedded
    private RepoOwner owner;

    /** 고정 여부(핀 여부) */
    @Column(name = "is_pinned")
    private boolean isPinned = false;
}