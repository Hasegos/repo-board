package io.github.repoboard.model;

import io.github.repoboard.model.enums.RepoVisibleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(
    name = "saved_repo",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "ux_saved_repo_user_owner_name",
            columnNames = {"user_id", "owner", "name"}
        )
    },
    indexes = {
        @Index(name = "idx_saved_repo_user", columnList = "user_id"),
        @Index(name = "idx_saved_repo_owner_name", columnList = "owner,name"),
        @Index(name = "idx_saved_repo_view", columnList = "view_count")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SavedRepo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "html_url", nullable = false, columnDefinition = "TEXT")
    private String htmlUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "language_main", nullable = false)
    private String languageMain;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_visibility", nullable = false)
    private RepoVisibleType itemVisibility = RepoVisibleType.PUBLIC;

    @Column(name = "stars")
    private Integer stars;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "forks")
    private Integer forks;

    @Column(name = "readme_excerpt",columnDefinition = "TEXT")
    private String readmeExcerpt;

    @Column(name = "owner", nullable = false)
    private String owner;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}