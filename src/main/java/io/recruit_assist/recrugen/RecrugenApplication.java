package io.recruit_assist.recrugen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class RecrugenApplication {

	public static void main(String[] args) {
		SpringApplication.run(RecrugenApplication.class, args);
	}

}
