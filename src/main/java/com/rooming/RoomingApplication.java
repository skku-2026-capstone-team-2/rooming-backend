package com.rooming;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class RoomingApplication {

	public static void main(String[] args) {
		SpringApplication.run(RoomingApplication.class, args);
	}

}