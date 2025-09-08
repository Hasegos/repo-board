package io.github.repoboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootApplication
public class RepoBoardApplication {

	public static void main(String[] args) {
		SpringApplication.run(RepoBoardApplication.class, args);
	}
}