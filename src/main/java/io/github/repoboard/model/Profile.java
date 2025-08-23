package io.github.repoboard.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "profiles")
public class Profile {

    @Id
    @Column(name = "user_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "repository_url", columnDefinition = "TEXT")
    private String repositoryUrl;

    @Column(name = "stacks", nullable = false)
    private String stacks;

    @Column(name = "experience", nullable = false)
    private String experience;

    @Column(name = "self_info",nullable = false, columnDefinition = "TEXT")
    private String selfInfo;

    @Column(name = "nickname", nullable = false)
    private String nickname;

    @Column(name = "follower_count")
    private Integer followerCount;

    @Column(name = "repo_count")
    private Integer repoCount;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "s3Key")
    private String s3Key;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    private Instant updatedAt;
}