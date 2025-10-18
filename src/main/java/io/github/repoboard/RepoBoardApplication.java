package io.github.repoboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * RepoBoard 애플리케이션 엔트리포인트.
 * <p>스케줄링이 활성화되어 백업 정리 잡을 수행한다.</p>
 */
@EnableScheduling
@SpringBootApplication
public class RepoBoardApplication {

	public static void main(String[] args) {
		SpringApplication.run(RepoBoardApplication.class, args);
	}
}