package io.github.repoboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class RepoBoardApplication {

	public static void main(String[] args) {
		SpringApplication.run(RepoBoardApplication.class, args);
	}
}