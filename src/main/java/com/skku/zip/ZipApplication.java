package com.skku.zip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ZipApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZipApplication.class, args);
	}

}
